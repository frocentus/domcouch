package com.domcouch.impl;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.codec.RawJsonTranscoder;
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
            var result = collection.get(unid,
                    GetOptions.getOptions().transcoder(RawJsonTranscoder.INSTANCE));
            if (result == null) return null;
            String rawJson = result.contentAs(String.class);
            if (rawJson == null) return null;
            JsonObject json = JsonObject.fromJson(rawJson);
            if (canRead(json, getCurrentUserName())) {
                return new CouchbaseDocument(this, json);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public View getView(String name) {
        return views.computeIfAbsent(name, k -> {
            // Try to reconstruct from persisted definition
            CouchbaseView loaded = loadViewDefinition(name);
            if (loaded != null) {
                loaded.setIndexService(viewIndexService);
                return loaded;
            }
            // No persisted definition — return shell view
            return new CouchbaseView(this, scope, name);
        });
    }

    /** Persist view definition as a Couchbase document. */
    private void persistViewDefinition(String name, String formula, List<String> keyColumns, List<ViewColumn> columns) {
        try {
            JsonObject def = JsonObject.create()
                    .put("_type", "domcouch.view_def")
                    .put("name", name)
                    .put("formula", formula != null ? formula : "");
            if (keyColumns != null && !keyColumns.isEmpty()) {
                var arr = com.couchbase.client.java.json.JsonArray.create();
                keyColumns.forEach(arr::add);
                def.put("keyColumns", arr);
            }
            if (columns != null && !columns.isEmpty()) {
                var arr = com.couchbase.client.java.json.JsonArray.create();
                for (ViewColumn col : columns) {
                    JsonObject c = JsonObject.create()
                            .put("name", col.getName())
                            .put("expression", col.getExpression());
                    if (col.isFormula()) c.put("formula", true);
                    arr.add(c);
                }
                def.put("columns", arr);
            }
            collection.upsert("view_def_" + name, def);
        } catch (Exception e) {
            log.warn("Failed to persist view definition {}: {}", name, e.getMessage());
        }
    }

    /** Load a persisted view definition. Returns null if not found. */
    private CouchbaseView loadViewDefinition(String name) {
        try {
            var result = collection.get("view_def_" + name);
            if (result == null) return null;
            JsonObject def = result.contentAs(JsonObject.class);
            if (def == null) {
                // Fallback: try Object.class
                Object content = result.contentAs(Object.class);
                if (content instanceof java.util.Map map) {
                    def = JsonObject.from(map);
                } else {
                    return null;
                }
            }
            String formula = def.getString("formula");
            // Reconstruct key columns
            List<String> keyCols = null;
            var keyArr = def.getArray("keyColumns");
            if (keyArr != null) {
                keyCols = new ArrayList<>();
                for (Object k : keyArr.toList()) keyCols.add(k.toString());
            }
            // Reconstruct display columns
            List<ViewColumn> cols = null;
            var colArr = def.getArray("columns");
            if (colArr != null) {
                cols = new ArrayList<>();
                for (Object o : colArr.toList()) {
                    if (o instanceof JsonObject c) {
                        String cn = c.getString("name");
                        String ce = c.getString("expression");
                        if (cn != null && ce != null) {
                            if (c.getBoolean("formula")) {
                                cols.add(ViewColumn.formula(cn, ce));
                            } else {
                                cols.add(ViewColumn.field(cn, ce));
                            }
                        }
                    }
                }
            }
            return new CouchbaseView(this, scope, name, formula, keyCols, cols);
        } catch (Exception e) {
            log.debug("No persisted view definition for {}: {}", name, e.getMessage());
        }
        return null;
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
        persistViewDefinition(name, selectionFormula, keyColumns, columns);
        createViewIndex(name, keyColumns != null && !keyColumns.isEmpty() ? keyColumns.get(0) : null);
        return view;
    }

    private void createViewIndex(String name, String keyItemName) {
        // Validate keyItemName: only alphanumeric, underscore
        if (keyItemName != null && !keyItemName.matches("[a-zA-Z0-9_]+")) {
            log.warn("Skipping index for unsafe key column: {}", keyItemName);
            return;
        }
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
            String n1qlFormula = formulaTranslator.toN1ql(formula);
            String stmt = "SELECT meta().id AS _id FROM " + getCollectionPath()
                    + " AS doc WHERE doc._type = 'domcouch.document' AND (" + n1qlFormula + ")";
            return fetchDocumentsByN1qlIds(stmt, getCurrentUserName());
        } catch (Exception e) {
            throw new NotesException(4003, "Search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public DocumentCollection getAllDocuments() {
        String stmt = "SELECT meta().id AS _id FROM " + getCollectionPath()
                + " AS doc WHERE doc._type = 'domcouch.document'";
        return fetchDocumentsByN1qlIds(stmt, getCurrentUserName());
    }

    DocumentCollection findByParentUNID(String parentUnid) {
        String stmt = "SELECT meta().id AS _id FROM " + getCollectionPath()
                + " AS doc WHERE doc._type = 'domcouch.document'"
                + " AND parentUNID = $parentUnid";
        List<Document> docs = new ArrayList<>();
        try {
            List<String> allIds = new ArrayList<>();
            for (JsonObject row : scope.query(stmt,
                    QueryOptions.queryOptions()
                            .parameters(JsonObject.create().put("parentUnid", parentUnid)))
                    .rowsAsObject()) {
                String unid = row.getString("_id");
                if (unid != null) allIds.add(unid);
            }
            if (allIds.isEmpty()) return new CouchbaseDocumentCollection(docs);
            return fetchDocumentsByN1qlIds(allIds, getCurrentUserName());
        } catch (Exception e) {
            log.warn("findByParentUNID failed: {}", e.getMessage());
        }
        return new CouchbaseDocumentCollection(docs);
    }

    /**
     * Fetch documents in batches of 100 via N1QL USE KEYS to avoid N+1 KV reads.
     */
    private DocumentCollection fetchDocumentsByN1qlIds(String idStmt, String userName) {
        try {
            List<String> allIds = new ArrayList<>();
            for (JsonObject row : scope.query(idStmt,
                    QueryOptions.queryOptions()
                            .scanConsistency(QueryScanConsistency.REQUEST_PLUS))
                    .rowsAsObject()) {
                String unid = row.getString("_id");
                if (unid != null) allIds.add(unid);
            }
            return fetchDocumentsByN1qlIds(allIds, userName);
        } catch (Exception e) {
            log.warn("fetchDocumentsByN1qlIds failed: {}", e.getMessage());
        }
        return new CouchbaseDocumentCollection(new ArrayList<>());
    }

    /** Batch-fetch documents from a pre-collected list of IDs. */
    private DocumentCollection fetchDocumentsByN1qlIds(List<String> allIds, String userName) {
        List<Document> docs = new ArrayList<>();
        try {
            if (allIds.isEmpty()) return new CouchbaseDocumentCollection(docs);
            String cp = getCollectionPath();
            for (int i = 0; i < allIds.size(); i += 100) {
                int end = Math.min(i + 100, allIds.size());
                String keysList = "'" + String.join("','", allIds.subList(i, end)) + "'";
                String batchStmt = "SELECT meta().id AS _id FROM " + cp
                        + " USE KEYS [" + keysList + "]";
                for (JsonObject row : scope.query(batchStmt).rowsAsObject()) {
                    String unid = row.getString("_id");
                    if (unid != null) {
                        Document doc = getDocumentByUNID(unid);
                        if (doc != null) docs.add(doc);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("fetchDocumentsByN1qlIds failed: {}", e.getMessage());
        }
        return new CouchbaseDocumentCollection(docs);
    }
    public int getDocumentCount() {
        try {
            String stmt = "SELECT COUNT(*) AS cnt FROM " + getCollectionPath()
                    + " AS doc WHERE doc._type = 'domcouch.document'";
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
        if (viewIndexService instanceof TTLViewIndexService ttl) {
            ttl.cleanupStale();
        }
        open = false;
        views.clear();
        formulaCache.clear();
    }

    // ---- internal (package-private) ----

    void upsertDocument(String unid, JsonObject json) {
        collection.upsert(unid, json);
    }

    /** N1QL query with request_plus scan consistency for read-your-writes. */
    private QueryResult queryWithConsistency(String stmt) {
        return scope.query(stmt, QueryOptions.queryOptions()
                .scanConsistency(QueryScanConsistency.REQUEST_PLUS));
    }

    void removeDocument(String unid) {
        collection.remove(unid);
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
            // Handle both object and array formats (multi-instance schema)
            Object val = items.get(name);
            if (val instanceof JsonArray arr) {
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject item = arr.getObject(i);
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
            } else if (val instanceof JsonObject item) {
                if (item.getInt("type") == Item.READERS) {
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
        // Validate folder name: only alphanumeric, dash, underscore, space
        if (!name.matches("[a-zA-Z0-9 _-]+")) {
            throw new NotesException(4000, "Invalid folder name: " + name);
        }
        // Folders are views whose selection formula is auto-generated
        String n1ql = "'" + name + "' IN doc.folders";
        CouchbaseView folder = new CouchbaseView(this, scope, name, n1ql, (List<String>) null, (List<ViewColumn>) null);
        folder.setIndexService(viewIndexService);
        views.put(name, folder);
        return folder;
    }

    @Override
    public View getFolder(String name) throws NotesException {
        if (!folderNames.contains(name)) return null;
        return views.computeIfAbsent(name, n -> {
            String n1ql = "'" + n + "' IN doc.folders";
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

    // ---- forms ----

    @Override
    public Form createForm(String formName, java.util.List<Form.FieldDefinition> fields) throws NotesException {
        try {
            // Store form definition as a Couchbase document
            var formDef = JsonObject.create()
                    .put("_type", "domcouch.form")
                    .put("name", formName);
            var fieldsArr = com.couchbase.client.java.json.JsonArray.create();
            for (var fd : fields) {
                fieldsArr.add(JsonObject.create()
                        .put("name", fd.getName())
                        .put("type", fd.getType())
                        .put("computed", fd.isComputed())
                        .put("computedWhenComposed", fd.isComputedWhenComposed())
                        .put("computedForDisplay", fd.isComputedForDisplay())
                        .put("formula", fd.getFormula() != null ? fd.getFormula() : "")
                        .put("defaultFormula", fd.getDefaultFormula() != null ? fd.getDefaultFormula() : "")
                        .put("validationFormula", fd.getValidationFormula() != null ? fd.getValidationFormula() : "")
                        .put("validationMessage", fd.getValidationMessage() != null ? fd.getValidationMessage() : "")
                        .put("multiValue", fd.isMultiValue())
                        .put("richText", fd.isRichText())
                        .put("numberFormat", fd.getNumberFormat() != null ? fd.getNumberFormat() : "")
                        .put("dateFormat", fd.getDateFormat() != null ? fd.getDateFormat() : ""));
            }
            formDef.put("fields", fieldsArr);
            collection.upsert("form_" + formName, formDef);
            return new StoredForm(formName, fields);
        } catch (Exception e) {
            throw new NotesException(4000, "Failed to create form: " + formName, e);
        }
    }

    @Override
    public Form getForm(String formName) throws NotesException {
        try {
            var result = collection.get("form_" + formName);
            if (result == null) return null;
            JsonObject def = result.contentAs(JsonObject.class);
            if (def == null) {
                Object content = result.contentAs(Object.class);
                if (content instanceof java.util.Map map) def = JsonObject.from(map);
                else return null;
            }
            var fieldsArr = def.getArray("fields");
            if (fieldsArr == null) return null;
            var fields = new java.util.ArrayList<Form.FieldDefinition>();
            for (int i = 0; i < fieldsArr.size(); i++) {
                JsonObject fd = fieldsArr.getObject(i);
                fields.add(new StoredFieldDef(fd));
            }
            return new StoredForm(formName, fields);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public java.util.List<String> getFormNames() throws NotesException {
        var names = new java.util.ArrayList<String>();
        try {
            String stmt = "SELECT meta().id FROM " + getCollectionPath()
                    + " WHERE _type = 'domcouch.form'";
            for (JsonObject row : scope.query(stmt).rowsAsObject()) {
                String id = row.getString("id");
                if (id != null && id.startsWith("form_")) names.add(id.substring(5));
            }
        } catch (Exception ignored) {}
        return names;
    }

    private record StoredForm(String name, java.util.List<Form.FieldDefinition> fields) implements Form {
        @Override public String getName() { return name; }
        @Override public java.util.List<FieldDefinition> getFields() { return fields; }
        @Override public FieldDefinition getField(String name) {
            return fields.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        }
    }

    private record StoredFieldDef(String name, int type, boolean computed,
            boolean computedWhenComposed, boolean computedForDisplay,
            String formula, String defaultFormula, String validationFormula,
            String validationMessage, boolean multiValue, boolean richText,
            String numberFormat, String dateFormat) implements Form.FieldDefinition {
        StoredFieldDef(JsonObject fd) {
            this(fd.getString("name"), fd.getInt("type"),
                    fd.getBoolean("computed"), fd.getBoolean("computedWhenComposed"),
                    fd.getBoolean("computedForDisplay"),
                    str(fd, "formula"), str(fd, "defaultFormula"),
                    str(fd, "validationFormula"), str(fd, "validationMessage"),
                    fd.getBoolean("multiValue"), fd.getBoolean("richText"),
                    str(fd, "numberFormat"), str(fd, "dateFormat"));
        }
        // Explicitly implement interface methods since records generate accessor() not getXxx()
        @Override public String getName() { return name(); }
        @Override public int getType() { return type(); }
        @Override public boolean isComputed() { return computed(); }
        @Override public boolean isComputedWhenComposed() { return computedWhenComposed(); }
        @Override public boolean isComputedForDisplay() { return computedForDisplay(); }
        @Override public String getFormula() { return formula(); }
        @Override public String getDefaultFormula() { return defaultFormula(); }
        @Override public String getValidationFormula() { return validationFormula(); }
        @Override public String getValidationMessage() { return validationMessage(); }
        @Override public boolean isMultiValue() { return multiValue(); }
        @Override public boolean isRichText() { return richText(); }
        @Override public String getNumberFormat() { return numberFormat(); }
        @Override public String getDateFormat() { return dateFormat(); }
        private static String str(JsonObject o, String key) {
            String v = o.getString(key); return v != null && !v.isEmpty() ? v : null;
        }
    }
}
