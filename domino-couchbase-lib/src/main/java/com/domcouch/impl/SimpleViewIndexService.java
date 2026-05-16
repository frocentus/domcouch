package com.domcouch.impl;

import com.couchbase.client.java.Scope;
import com.couchbase.client.java.query.QueryResult;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple ViewIndexService that creates indexes on first access and
 * drops them on explicit recycle. Thread-safe (ConcurrentHashMap).
 */
public class SimpleViewIndexService implements ViewIndexService {

    private final Scope scope;
    private final Set<String> createdIndexes = ConcurrentHashMap.newKeySet();

    public SimpleViewIndexService(Scope scope) {
        this.scope = scope;
    }

    @Override
    public String ensureIndex(CouchbaseView view) {
        String idxName = getIndexName(view);
        if (createdIndexes.contains(idxName)) return idxName;

        var keyColumns = view.getCategoryColumns();
        if (keyColumns.isEmpty()) return idxName;

        // Build the CREATE INDEX statement
        StringBuilder sb = new StringBuilder("CREATE INDEX `").append(idxName).append("`")
                .append(" ON ").append(view.getDatabase().getCollectionPath())
                .append("(");
        for (int i = 0; i < keyColumns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("items.").append(keyColumns.get(i)).append("[0].`values`[0]");
        }
        sb.append(") WHERE _type = 'domcouch.document'");

        try {
            // Check if index already exists (idempotent)
            if (indexExists(idxName)) {
                createdIndexes.add(idxName);
                return idxName;
            }
            scope.query(sb.toString());
            createdIndexes.add(idxName);
            // Wait briefly for index to be ready (Couchbase builds async)
            Thread.sleep(200);
        } catch (Exception e) {
            // Index creation failed — queries will still work (full scan)
            System.err.println("[domcouch] Index creation failed for " + idxName + ": " + e.getMessage());
        }
        return idxName;
    }

    @Override
    public void dropIndex(CouchbaseView view) {
        String idxName = getIndexName(view);
        if (!createdIndexes.remove(idxName)) return;
        try {
            if (indexExists(idxName)) {
                scope.query("DROP INDEX `" + view.getDatabase().getCollectionPath()
                        + "`.`" + idxName + "`");
            }
        } catch (Exception e) {
            // Swallow — index may already be gone
        }
    }

    @Override
    public boolean hasIndex(CouchbaseView view) {
        return createdIndexes.contains(getIndexName(view));
    }

    private boolean indexExists(String idxName) {
        try {
            QueryResult r = scope.query(
                    "SELECT COUNT(*) AS cnt FROM system:indexes WHERE name = '" + idxName + "'");
            var rows = r.rowsAsObject();
            return !rows.isEmpty() && rows.get(0).getInt("cnt") > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
