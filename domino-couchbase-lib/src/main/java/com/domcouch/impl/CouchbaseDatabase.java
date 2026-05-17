package com.domcouch.impl;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryScanConsistency;
import com.domcouch.api.*;
import com.domcouch.formula.CompiledFormula;
import com.domcouch.formula.FormulaContext;
import com.domcouch.formula.translate.FormulaTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Couchbase-backed Database implementation.
 *
 * Maps a Domino "database" to a Couchbase scope containing a collection
 * named {@value #COLLECTION_NAME}.
 */
public class CouchbaseDatabase implements Database {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseDatabase.class);

    public static final String COLLECTION_NAME = "documents";

    /** Default scope name when a database maps to a full bucket (single-arg getDatabase). */
    public static final String DEFAULT_SCOPE = "data";

    private static final int DEFAULT_FTS_MAX_DOCS = 500;

    private final Cluster cluster;
    private final Bucket bucket;
    private final Scope scope;
    private final Collection collection;
    private final String bucketName;
    private final String scopeName;
    private String title;
    private boolean open;
    private final Map<String, CouchbaseView> views;
    private final Set<String> folderNames;
    private final ViewIndexService viewIndexService;

    public CouchbaseDatabase(Cluster cluster, String bucketName, String scopeName) {
        this.cluster = cluster;
        this.bucketName = bucketName;
        this.scopeName = scopeName;
        this.bucket = cluster.bucket(bucketName);
        this.scope = bucket.scope(scopeName);
        this.collection = scope.collection(COLLECTION_NAME);
        this.open = true;
        this.title = scopeName;
        this.views = new ConcurrentHashMap<>();
        this.folderNames = ConcurrentHashMap.newKeySet();
        this.viewIndexService = new TTLViewIndexService(scope);

        // Ensure the collection exists by performing a lightweight query that creates the primary index
        ensurePrimaryIndex();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getScopeName() {
        return scopeName;
    }

    // ---- Database API ----

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getFileName() {
        return scopeName;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public Document createDocument() {
        return new CouchbaseDocument(this);
    }

    @Override
    public Document getDocumentByUNID(String unid) {
        try {
            var result = collection.get(unid);
            if (result == null) return null;
            JsonObject json = result.contentAsObject();
            // Reader-field enforcement BEFORE full deserialization
            if (!canRead(json, getCurrentUserName())) return null;
            return new CouchbaseDocument(this, json);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public View getView(String name) {
        return views.computeIfAbsent(name,
                k -> new CouchbaseView(this, scope, name));
    }

    @Override
    public View createView(String name, String selectionFormula) {
        return createView(name, selectionFormula, (List<String>) null, (List<ViewColumn>) null);
    }

    public View createView(String name, String selectionFormula, String keyItemName) {
        return createView(name, selectionFormula,
                keyItemName != null ? List.of(keyItemName) : null, (List<ViewColumn>) null);
    }

    @Override
    public View createView(String name, String selectionFormula, List<ViewColumn> columns) {
        return createView(name, selectionFormula, (String) null, columns);
    }

    @Override
    public View createView(String name, String selectionFormula,
                           String keyItemName, List<ViewColumn> columns) {
        return createView(name, selectionFormula,
                keyItemName != null ? List.of(keyItemName) : (List<String>) null, columns);
    }

    @Override
    public View createView(String name, String selectionFormula, List<String> keyColumns, List<ViewColumn> columns) {
        String n1qlFormula = formulaTranslator.toN1ql(selectionFormula);
        CouchbaseView view = new CouchbaseView(this, scope, name, n1qlFormula, keyColumns, columns);
        view.setIndexService(viewIndexService);
        views.put(name, view);
        createViewIndex(name, keyColumns != null && !keyColumns.isEmpty() ? keyColumns.get(0) : null);
        return view;
    }

    private void createViewIndex(String name, String keyItemName) {
        try {
            String collectionPath = getCollectionPath();
            String indexName = "idx_view_" + name.replaceAll("[^a-zA-Z0-9]", "_");
            // Check if index already exists to avoid redundant DDL on every start
            boolean exists = false;
            try {
                var result = scope.query("SELECT COUNT(*) AS cnt FROM system:indexes WHERE name = '" + indexName + "'");
                var rows = result.rowsAsObject();
                exists = !rows.isEmpty() && rows.get(0).getInt("cnt") > 0;
            } catch (Exception ignored) { /* query failed, assume doesn't exist */ }
            if (!exists) {
                String createIndex;
                if (keyItemName != null) {
                    createIndex = "CREATE INDEX `" + indexName + "` ON " + collectionPath
                            + "((items.Form[0].`values`[0]), (items." + keyItemName.toUpperCase() + "[0].`values`[0]))"
                            + " WHERE _type = 'domcouch.document'";
                } else {
                    createIndex = "CREATE INDEX `" + indexName + "` ON " + collectionPath
                            + "((items.Form[0].`values`[0])) WHERE _type = 'domcouch.document'";
                }
                scope.query(createIndex);
            }
        } catch (Exception e) {
            log.warn("Could not create index for view '{}': {}", name, e.getMessage());
        }
    }

    @Override
    public DocumentCollection FTSearch(String query) throws NotesException {
        return FTSearch(query, DEFAULT_FTS_MAX_DOCS);
    }

    @Override
    public DocumentCollection FTSearch(String query, int maxDocs) throws NotesException {
        List<Document> docs = new ArrayList<>();
        try {
            String stmt = buildSearchStatement();
            String userName = getCurrentUserName();
            QueryResult result = scope.query(stmt,
                    QueryOptions.queryOptions().parameters(JsonObject.create()
                            .put("q", "%" + query.toLowerCase() + "%")
                            .put("limit", maxDocs)));
            for (JsonObject row : result.rowsAsObject()) {
                if (canRead(row, userName)) {
                    docs.add(new CouchbaseDocument(this, row));
                }
            }
        } catch (Exception e) {
            throw new NotesException(4002, "FTSearch failed: " + e.getMessage(), e);
        }
        return new CouchbaseDocumentCollection(docs);
    }

    @Override
    public DocumentCollection search(String formula) throws NotesException {
        if (formula == null || formula.isBlank()) {
            return getAllDocuments();
        }
        try {
            List<Document> docs = new ArrayList<>();
            String n1qlFormula = formulaTranslator.toN1ql(formula);
            // IDs via N1QL, bodies via KV — same approach as getAllDocuments()
            String stmt = "SELECT meta().id AS _id FROM " + getCollectionPath()
                    + " WHERE _type = 'domcouch.document' AND (" + n1qlFormula + ")";
            String userName = getCurrentUserName();
            for (JsonObject row : scope.query(stmt, QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS)).rowsAsObject()) {
                String unid = row.getString("_id");
                if (unid == null) continue;
                try {
                    var result = collection.get(unid);
                    if (result != null) {
                        JsonObject docJson = result.contentAsObject();
                        if (canRead(docJson, userName)) {
                            docs.add(new CouchbaseDocument(this, docJson));
                        }
                    }
                } catch (Exception kvEx) { /* skip */ }
            }
            return new CouchbaseDocumentCollection(docs);
        } catch (Exception e) {
            throw new NotesException(4003, "Search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public DocumentCollection getAllDocuments() {
        List<Document> docs = new ArrayList<>();
        try {
            // Workaround for Couchbase SDK ClassCastException:
            // rowsAsObject() fails with nested JSON arrays (our items schema).
            // Solution: SELECT only meta().id via N1QL, then fetch each
            // document via KV (collection.get) which handles arrays correctly.
            String stmt = "SELECT meta().id AS _id FROM " + getCollectionPath()
                    + " WHERE _type = 'domcouch.document'";
            String userName = getCurrentUserName();
            for (JsonObject row : scope.query(stmt).rowsAsObject()) {
                String unid = row.getString("_id");
                if (unid == null) continue;
                try {
                    var result = collection.get(unid);
                    if (result != null) {
                        JsonObject docJson = result.contentAsObject();
                        if (canRead(docJson, userName)) {
                            docs.add(new CouchbaseDocument(this, docJson));
                        }
                    }
                } catch (Exception kvEx) {
                    // Skip individual KV fetch failures
                }
            }
        } catch (Exception e) {
            log.warn("getAllDocuments failed: {}", e.getMessage());
        }
        return new CouchbaseDocumentCollection(docs);
    }

    @Override
    public int getDocumentCount() {
        try {
            String stmt = "SELECT COUNT(*) AS cnt FROM " + getCollectionPath()
                    + " WHERE _type = 'domcouch.document'";
            QueryResult result = scope.query(stmt);
            var rows = result.rowsAsObject();
            if (!rows.isEmpty()) {
                return rows.get(0).getInt("cnt");
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public void recycle() {
        // Clean up stale TTL indexes
        if (viewIndexService instanceof TTLViewIndexService ttl) {
            ttl.cleanupStale();
        }
        open = false;
        views.clear();
    }

    // ---- internal (package-private) ----

    void upsertDocument(String unid, JsonObject json) {
        collection.upsert(unid, json);
    }

    void removeDocument(String unid) {
        try {
            collection.remove(unid);
        } catch (Exception ignored) {}
    }

    // ---- private ----

    final FormulaTranslator formulaTranslator = new FormulaTranslator();
    private final Map<String, CompiledFormula> formulaCache = new ConcurrentHashMap<>();

    /** Set the current username for @UserName resolution in formulas. */
    public void setCurrentUserName(String name) {
        formulaTranslator.setCurrentUserName(name);
    }

    /** @return the current username used for Reader/Author checks and @UserName. */
    public String getCurrentUserName() {
        return formulaTranslator.getCurrentUserName();
    }

    /**
     * Pre-compile a named formula for repeated evaluation.
     * Call this at form-load time for each computed field.
     *
     * @param name    a unique name for this formula (e.g., the field name)
     * @param formula the Domino formula string
     * @return the compiled formula
     */
    public CompiledFormula compileFormula(String name, String formula) {
        CompiledFormula cf = formulaTranslator.compile(formula);
        formulaCache.put(formula, cf); // key by formula string, not name
        return cf;
    }

    /**
     * Evaluate a pre-compiled formula against a document context.
     *
     * @param compiled the compiled formula (from {@link #compileFormula})
     * @param ctx      the document context
     * @return the computed value
     */
    public Object evaluateCached(CompiledFormula compiled, FormulaContext ctx) {
        return formulaTranslator.evaluate(compiled, ctx);
    }

    /**
     * Evaluate a named formula against a document context.
     * Compiles on first use, caches for subsequent calls.
     *
     * @param formula the Domino formula string
     * @param ctx     the document context
     * @return the computed value
     */
    public Object evaluateFormula(String formula, FormulaContext ctx) {
        CompiledFormula cf = formulaCache.computeIfAbsent(formula,
                k -> formulaTranslator.compile(k));
        return formulaTranslator.evaluate(cf, ctx);
    }

    /**
     * Check if a user can read a document based on its raw JSON.
     * Used by both {@link CouchbaseDocument#isReadableBy} and {@link CouchbaseView#buildCollection}
     * to ensure consistent reader-field enforcement.
     * <p>
     * Domino semantics: if NO reader items (type=4) exist → public.
     * If ANY reader items exist → user must appear in at least one.
     *
     * @param docJson  the document JSON (must contain an "items" object)
     * @param userName the user to check
     * @return true if the user is allowed to read this document
     */
    public static boolean canRead(JsonObject docJson, String userName) {
        JsonObject items = docJson.getObject("items");
        if (items == null) return true;

        boolean hasReaderField = false;
        for (String name : items.getNames()) {
            JsonObject item = items.getObject(name);
            if (item != null && item.getInt("type") == Item.READERS) {
                hasReaderField = true;
                var values = item.getArray("values");
                if (values != null) {
                    for (Object v : values.toList()) {
                        if (v != null && v.toString().equals(userName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return !hasReaderField;
    }

    public String getCollectionPath() {
        return "`" + bucketName + "`.`" + scopeName + "`.`" + COLLECTION_NAME + "`";
    }


    private void ensurePrimaryIndex() {
        try {
            String stmt = "CREATE PRIMARY INDEX IF NOT EXISTS ON " + getCollectionPath();
            scope.query(stmt);
        } catch (Exception e) {
            // Index may already exist
        }
    }

    private String buildSearchStatement() {
        String cp = getCollectionPath();
        // Uses $q parameter placeholder — caller MUST pass parameters via QueryOptions
        return "SELECT d.* FROM " + cp + " AS d"
                + " WHERE d._type = 'domcouch.document'"
                + " AND d.unid IN ("
                + "   SELECT DISTINCT RAW doc.unid FROM " + cp + " AS doc"
                + "   UNNEST OBJECT_PAIRS(doc.items) AS item"
                + "   UNNEST item.val.`values` AS val"
                + "   WHERE doc._type = 'domcouch.document'"
                + "   AND LOWER(TO_STRING(val)) LIKE $q"
                + " )"
                + " LIMIT $limit";
    }

    // ---- folders ----

    @Override
    public View createFolder(String name) throws NotesException {
        if (name == null || name.isEmpty()) {
            throw new NotesException(4000, "Folder name cannot be null or empty");
        }
        folderNames.add(name);
        // Folders are views whose selection formula is auto-generated
        String n1ql = "'" + name.replace("'", "\\'") + "' IN doc.folders";
        CouchbaseView folder = new CouchbaseView(this, scope, name, n1ql, (List<String>) null, (List<ViewColumn>) null);
        folder.setIndexService(viewIndexService);
        views.put(name, folder);
        return folder;
    }

    @Override
    public View getFolder(String name) throws NotesException {
        if (!folderNames.contains(name)) return null;
        return views.computeIfAbsent(name, n -> {
            String n1ql = "'" + n.replace("'", "\\'") + "' IN doc.folders";
            var folder = new CouchbaseView(this, scope, n, n1ql, (List<String>) null, (List<ViewColumn>) null);
            folder.setIndexService(viewIndexService);
            return folder;
        });
    }

    @Override
    public List<String> getFolderNames() throws NotesException {
        return List.copyOf(folderNames);
    }

    @Override
    public void removeFolder(String name) throws NotesException {
        folderNames.remove(name);
        views.remove(name);
    }

    @Override
    public boolean isFolder(String name) throws NotesException {
        if (name == null) return false;
        return folderNames.contains(name);
    }
}
