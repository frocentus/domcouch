package com.domcouch.impl;

import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.domcouch.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Couchbase-backed View implementation using N1QL queries.
 */
public class CouchbaseView implements View {

    private final CouchbaseDatabase database;
    private final Scope scope;
    private final String name;
    private final String selectionFormula;
    private final String keyColumnN1ql;
    private final List<String> keyColumns; // for multi-level categorization
    private final List<ViewColumn> columns; // null = legacy extract-all-items mode
    private final com.domcouch.formula.translate.FormulaTranslator formulaTranslator;

    /** Legacy 3-arg constructor for views without explicit columns. */
    public CouchbaseView(CouchbaseDatabase database, Scope scope, String name) {
        this(database, scope, name, null,
                (java.util.List<String>) null, (List<ViewColumn>) null);
    }

    public CouchbaseView(CouchbaseDatabase database, Scope scope,
                         String name, String selectionFormula, String keyItemName) {
        this(database, scope, name, selectionFormula,
                keyItemName != null ? List.of(keyItemName) : (java.util.List<String>) null,
                (List<ViewColumn>) null);
    }

    public CouchbaseView(CouchbaseDatabase database, Scope scope,
                         String name, String selectionFormula, String keyItemName,
                         List<ViewColumn> columns) {
        this(database, scope, name, selectionFormula,
                keyItemName != null ? List.of(keyItemName) : (java.util.List<String>) null, columns);
    }

    /** Full constructor with multi-key column support for categorization. */
    public CouchbaseView(CouchbaseDatabase database, Scope scope,
                         String name, String selectionFormula, List<String> keyColumns,
                         List<ViewColumn> columns) {
        this.database = database;
        this.scope = scope;
        this.name = name;
        this.selectionFormula = selectionFormula;
        this.keyColumns = keyColumns != null ? List.copyOf(keyColumns) : List.of();
        this.keyColumnN1ql = buildKeyColumnN1ql();
        this.columns = columns;
        this.formulaTranslator = hasFormulaColumns()
                ? new com.domcouch.formula.translate.FormulaTranslator()
                : null;
    }

    private String buildKeyColumnN1ql() {
        if (keyColumns == null || keyColumns.isEmpty()) return null;
        return buildKeyColumnRef(keyColumns.get(0));
    }

    private boolean hasFormulaColumns() {
        return columns != null && columns.stream().anyMatch(ViewColumn::isFormula);
    }

    public CouchbaseDatabase getDatabase() {
        return database;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ViewEntryCollection getAllEntries() {
        String stmt = buildSelectStatement(false);
        return executeQuery(stmt);
    }

    @Override
    public ViewEntryCollection getAllEntriesByKey(Object key) {
        String keyCol = getFirstColumnName();
        if (keyCol == null) {
            // No key column defined — fall back to full-text-style search
            return textSearchFallback(key.toString(), 50);
        }
        String stmt = buildSelectStatement(false) + " AND " + keyCol + " = $key";
        try {
            QueryResult result = scope.query(stmt,
                    QueryOptions.queryOptions().parameters(JsonObject.create().put("key", key)));
            return buildCollection(result);
        } catch (Exception e) {
            return CouchbaseViewEntryCollection.empty();
        }
    }

    @Override
    public ViewEntry getEntryByKey(Object key) {
        ViewEntryCollection col = getAllEntriesByKey(key);
        return col.getFirstEntry();
    }

    @Override
    public ViewEntryCollection FTSearch(String query) {
        return FTSearch(query, 50);
    }

    @Override
    public ViewEntryCollection FTSearch(String query, int maxDocs) {
        String stmt = buildSelectStatement(false)
                + " AND SEARCH(doc, { \"query\": $q }) LIMIT $max";
        try {
            QueryResult result = scope.query(stmt,
                    QueryOptions.queryOptions()
                            .parameters(JsonObject.create()
                                    .put("q", query)
                                    .put("max", maxDocs)));
            return buildCollection(result);
        } catch (Exception e) {
            // Fallback to basic text search on items
            return textSearchFallback(query, maxDocs);
        }
    }

    @Override
    public int getEntryCount() {
        String stmt = "SELECT COUNT(*) AS cnt FROM " + database.getCollectionPath()
                + " WHERE _type = 'domcouch.document'";
        if (selectionFormula != null && !selectionFormula.isEmpty()) {
            stmt += " AND (" + selectionFormula + ")";
        }
        try {
            QueryResult result = scope.query(stmt);
            var rows = result.rowsAsObject();
            if (!rows.isEmpty()) {
                return rows.get(0).getInt("cnt");
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public void refresh() {
        // N1QL queries always return fresh data — no index refresh needed
    }

    @Override
    public void recycle() {
        // nothing to release for a view
    }

    // ---- ViewNavigator factory methods ----

    @Override
    public ViewNavigator createViewNav() {
        return new CouchbaseViewNavigator(this, 64, 0);
    }

    @Override
    public ViewNavigator createViewNav(int cacheSize) {
        return new CouchbaseViewNavigator(this, cacheSize, 0);
    }

    @Override
    public ViewNavigator createViewNavFrom(ViewEntry entry) {
        var nav = createViewNav();
        try { nav.gotoEntry(entry); } catch (NotesException ignored) {}
        return nav;
    }

    @Override
    public ViewNavigator createViewNavFromCategory(String category) {
        if (!isCategorized()) return createViewNav();
        // Create navigator and seek to category
        var nav = new CouchbaseViewNavigator(this, 64, 1);
        // Find the category entry by value
        try {
            for (int i = 0; i < nav.getCount(); i++) {
                ViewEntry e = nav.getNth(i + 1);
                if (e.isCategory() && !e.getColumnValues().isEmpty()
                        && category.equals(e.getColumnValues().get(0))) {
                    nav.gotoEntry(e);
                    return nav;
                }
            }
        } catch (NotesException ignored) {}
        return nav;
    }

    @Override
    public ViewNavigator createViewNavFromChildren(ViewEntry entry) {
        var nav = createViewNav();
        try {
            nav.gotoEntry(entry);
            ViewEntry child = nav.getChild();
            if (child != null) {
                int startInclusive = child.getPosition() - 1;
                int endExclusive = startInclusive + entry.getChildCount();
                return ((CouchbaseViewNavigator) nav).createSubset(startInclusive, endExclusive);
            }
        } catch (NotesException ignored) {}
        return nav;
    }

    @Override
    public ViewNavigator createViewNavFromDescendants(ViewEntry entry) {
        var nav = createViewNav();
        try {
            nav.gotoEntry(entry);
            int startPos = entry.getPosition() - 1;
            int endPos = startPos + 1 + entry.getDescendantCount(); // +1 for entry itself
            return ((CouchbaseViewNavigator) nav).createSubset(startPos, endPos);
        } catch (Exception ignored) {}
        return nav;
    }

    @Override
    public ViewNavigator createViewNavMaxLevel(int maxLevel) {
        return new CouchbaseViewNavigator(this, 64, maxLevel);
    }

    @Override
    public boolean isCategorized() {
        return keyColumns != null && !keyColumns.isEmpty();
    }

    @Override
    public List<String> getCategoryColumns() {
        return keyColumns != null ? keyColumns : List.of();
    }

    // ---- package-private helpers for CouchbaseViewNavigator ----

    String getSelectionFormula() {
        return selectionFormula;
    }

    Scope getScope() {
        return scope;
    }

    String buildSelectStatement(boolean countOnly) {
        String cp = database.getCollectionPath();
        String selectClause;
        if (countOnly) {
            selectClause = "COUNT(*) AS cnt";
        } else if (columns != null && !columns.isEmpty()) {
            // Build explicit column SELECT — push translatable formulas to N1QL
            StringBuilder sb = new StringBuilder("unid");
            boolean needsDocStar = false;
            for (ViewColumn col : columns) {
                if (col.isFormula()) {
                    // Try N1QL value translation via the database's translator
                    try {
                        String n1ql = database.formulaTranslator.toN1qlValue(col.getExpression());
                        if (n1ql != null) {
                            sb.append(", (").append(n1ql).append(") AS `").append(col.getName()).append("`");
                            continue;
                        }
                    } catch (Exception ignored) { /* fall through to Java eval */ }
                    needsDocStar = true;
                } else {
                    sb.append(", doc.items.").append(escapeBacktick(col.getExpression().toUpperCase()))
                            .append("[0].`values`[0] AS `").append(col.getName()).append("`");
                }
            }
            if (needsDocStar) sb.append(", doc.*");
            selectClause = sb.toString();
        } else {
            selectClause = "unid, doc.*";
        }
        String stmt = "SELECT " + selectClause + " FROM " + cp + " AS doc"
                + " WHERE doc._type = 'domcouch.document'";
        if (selectionFormula != null && !selectionFormula.isEmpty()) {
            stmt += " AND (" + selectionFormula + ")";
        }
        return stmt;
    }

    private static String escapeBacktick(String s) {
        return s.replace("`", "\\`");
    }

    private String getFirstColumnName() {
        // Use explicitly-set key column if available
        if (keyColumnN1ql != null) return keyColumnN1ql;

        // Try to extract from formula: doc.items.FieldName ...
        if (selectionFormula != null) {
            int idx = selectionFormula.indexOf("doc.items.");
            if (idx >= 0) {
                int start = idx + "doc.items.".length();
                int end = start;
                while (end < selectionFormula.length()
                        && (Character.isLetterOrDigit(selectionFormula.charAt(end))
                            || selectionFormula.charAt(end) == '_')) {
                    end++;
                }
                if (end > start) {
                    String fieldName = selectionFormula.substring(start, end);
                    return "doc.items." + fieldName + "[0].`values`[0]";
                }
            }
        }
        return null;
    }

    private ViewEntryCollection executeQuery(String stmt) {
        try {
            QueryResult result = scope.query(stmt);
            return buildCollection(result);
        } catch (Exception e) {
            return CouchbaseViewEntryCollection.empty();
        }
    }

    private ViewEntryCollection buildCollection(QueryResult result) {
        List<ViewEntry> entries = new ArrayList<>();
        int pos = 0;
        for (JsonObject row : result.rowsAsObject()) {
            // Reader-field enforcement: skip entries user cannot read
            if (!isReadableRow(row)) continue;
            pos++;
            String unid = row.getString("unid");
            List<Object> cols = extractColumnValues(row);
            entries.add(new CouchbaseViewEntry(this, unid, cols, pos, row));
        }
        return new CouchbaseViewEntryCollection(entries);
    }

    /** Build N1QL field reference for a key column name. */
    String buildKeyColumnRef(String colName) {
        return "doc.items." + colName + "[0].`values`[0]";
    }

    /** Extract column values from a query result row (package-private for navigator). */
    List<Object> extractColumnValues(JsonObject row) {
        if (columns == null || columns.isEmpty()) {
            // Legacy mode: extract all items
            List<Object> cols = new ArrayList<>();
            cols.add(row.getString("form"));
            cols.add(row.getString("unid"));
            JsonObject items = row.getObject("items");
            if (items != null) {
                for (String key : items.getNames()) {
                    JsonObject item = items.getObject(key);
                    if (item != null) {
                        var values = item.getArray("values");
                        if (values != null && !values.isEmpty()) cols.add(values.get(0));
                    }
                }
            }
            return cols;
        }

        // Column-aware mode: extract defined columns
        List<Object> cols = new ArrayList<>();
        for (ViewColumn col : columns) {
            if (col.isFormula()) {
                // Evaluate formula against the document
                cols.add(evaluateFormulaColumn(row, col));
            } else {
                // Direct field: value was already fetched by N1QL SELECT AS
                String colName = col.getName();
                Object val = row.get(colName);
                cols.add(val != null ? val : "");
            }
        }
        return cols;
    }

    private Object evaluateFormulaColumn(JsonObject row, ViewColumn col) {
        // Check if value was already fetched via N1QL translation in SELECT
        Object n1qlValue = row.get(col.getName());
        if (n1qlValue != null) return n1qlValue;

        // Fall back to Java formula evaluation
        try {
            CouchbaseDocument doc = new CouchbaseDocument(database, row);
            DocumentFormulaContext ctx = new DocumentFormulaContext(doc);
            return formulaTranslator.evaluate(col.getExpression(), ctx);
        } catch (Exception e) {
            return "";
        }
    }

    private ViewEntryCollection textSearchFallback(String query, int maxDocs) {
        String lowerQuery = query.toLowerCase();
        String cp = database.getCollectionPath();
        String stmt = "SELECT doc.unid, doc.* FROM " + cp + " AS doc"
                + " WHERE doc._type = 'domcouch.document'"
                + " AND LOWER(doc.items.FirstName[0].`values`[0]) LIKE $q"
                + " LIMIT $max";
        try {
            QueryResult result = scope.query(stmt,
                    QueryOptions.queryOptions()
                            .parameters(JsonObject.create()
                                    .put("q", "%" + lowerQuery + "%")
                                    .put("max", maxDocs)));
            return buildCollection(result);
        } catch (Exception e) {
            return CouchbaseViewEntryCollection.empty();
        }
    }

    /**
     * Check if the current user can read the document represented by this query row.
     * Delegates to {@link CouchbaseDatabase#canRead} for centralized enforcement.
     */
    boolean isReadableRow(JsonObject row) {
        return CouchbaseDatabase.canRead(row, database.getCurrentUserName());
    }
}
