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
    private final String keyColumnN1ql; // e.g. "doc.items.LastName.`values`[0]"

    public CouchbaseView(CouchbaseDatabase database, Scope scope,
                         String name, String selectionFormula, String keyItemName) {
        this.database = database;
        this.scope = scope;
        this.name = name;
        this.selectionFormula = selectionFormula;
        this.keyColumnN1ql = keyItemName != null
                ? "doc.items." + keyItemName + ".`values`[0]"
                : null;
    }

    public CouchbaseView(CouchbaseDatabase database, Scope scope, String name) {
        this(database, scope, name, null, null);
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

    // ---- internal ----

    private String buildSelectStatement(boolean countOnly) {
        String cp = database.getCollectionPath();
        String selectClause = countOnly ? "COUNT(*) AS cnt" : "unid, doc.*";
        String stmt = "SELECT " + selectClause + " FROM " + cp + " AS doc"
                + " WHERE doc._type = 'domcouch.document'";
        if (selectionFormula != null && !selectionFormula.isEmpty()) {
            stmt += " AND (" + selectionFormula + ")";
        }
        return stmt;
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
                    return "doc.items." + fieldName + ".`values`[0]";
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

    private List<Object> extractColumnValues(JsonObject row) {
        List<Object> cols = new ArrayList<>();
        // Extract form and unid as default columns
        cols.add(row.getString("form"));
        cols.add(row.getString("unid"));
        // Extract additional item values
        JsonObject items = row.getObject("items");
        if (items != null) {
            for (String key : items.getNames()) {
                JsonObject item = items.getObject(key);
                if (item != null) {
                    var values = item.getArray("values");
                    if (values != null && !values.isEmpty()) {
                        cols.add(values.get(0));
                    }
                }
            }
        }
        return cols;
    }

    private ViewEntryCollection textSearchFallback(String query, int maxDocs) {
        String lowerQuery = query.toLowerCase();
        String cp = database.getCollectionPath();
        String stmt = "SELECT doc.unid, doc.* FROM " + cp + " AS doc"
                + " WHERE doc._type = 'domcouch.document'"
                + " AND LOWER(doc.items.FirstName.`values`[0]) LIKE $q"
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
    private boolean isReadableRow(JsonObject row) {
        return CouchbaseDatabase.canRead(row, database.getCurrentUserName());
    }
}
