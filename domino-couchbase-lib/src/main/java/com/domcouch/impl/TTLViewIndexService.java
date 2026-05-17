package com.domcouch.impl;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ViewIndexService with TTL-based lifecycle and formula-aware index sharing.
 * <p>
 * Index names are derived from a hash of (selectionFormula + keyColumns),
 * so views with identical formulas share the same index. Metadata is stored
 * in a Couchbase collection ({@code _view_index_meta}) so it survives restarts.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #ensureIndex} — creates index if missing, refreshes lastAccessAt</li>
 *   <li>Access extends TTL (renewed on each ensureIndex call)</li>
 *   <li>{@link #cleanupStale} — drops indexes whose TTL has expired</li>
 * </ol>
 * Cleanup can be called manually or scheduled periodically.
 */
public class TTLViewIndexService implements ViewIndexService {

    private static final String META_COLLECTION = "view_index_meta";
    private static final int DEFAULT_TTL_SECONDS = 3600; // 1 hour

    private final Scope scope;
    private final Collection metaCollection;
    private final int ttlSeconds;
    private final Set<String> createdIndexes = ConcurrentHashMap.newKeySet();

    public TTLViewIndexService(Scope scope) {
        this(scope, DEFAULT_TTL_SECONDS);
    }

    public TTLViewIndexService(Scope scope, int ttlSeconds) {
        this.scope = scope;
        this.ttlSeconds = ttlSeconds;
        this.metaCollection = ensureMetaCollection(scope);
    }

    private static Collection ensureMetaCollection(Scope scope) {
        try {
            // Check if collection exists
            scope.query("SELECT 1 FROM " + META_COLLECTION + " LIMIT 1");
        } catch (Exception e) {
            // Collection doesn't exist — create it via DDL
            try {
                scope.query("CREATE COLLECTION `" + scope.bucketName()
                        + "`.`" + scope.name() + "`.`" + META_COLLECTION + "` IF NOT EXISTS");
                // Wait briefly for DDL to propagate
                Thread.sleep(500);
            } catch (Exception ex) {
                System.err.println("[domcouch] Failed to create metadata collection "
                        + META_COLLECTION + ": " + ex.getMessage());
            }
        }
        return scope.collection(META_COLLECTION);
    }

    // ---- ViewIndexService ----

    @Override
    public String ensureIndex(CouchbaseView view) {
        var keyColumns = view.getCategoryColumns();
        if (keyColumns.isEmpty()) return "";

        String hash = computeHash(view.getSelectionFormula(), keyColumns);
        String idxName = "idx_nav_" + hash;

        // Check in-memory cache first
        if (createdIndexes.contains(idxName)) {
            refreshMetadata(idxName, view);
            return idxName;
        }

        // Check metadata document in Couchbase
        JsonObject meta = loadMetadata(idxName);
        if (meta != null) {
            createdIndexes.add(idxName);
            refreshMetadata(idxName, view);
            return idxName;
        }

        // Create index if not exists
        if (!indexExists(idxName)) {
            createIndex(idxName, view);
        }

        // Store metadata
        storeMetadata(idxName, view);
        createdIndexes.add(idxName);
        return idxName;
    }

    @Override
    public void dropIndex(CouchbaseView view) {
        var keyColumns = view.getCategoryColumns();
        if (keyColumns.isEmpty()) return;

        String hash = computeHash(view.getSelectionFormula(), keyColumns);
        String idxName = "idx_nav_" + hash;

        createdIndexes.remove(idxName);
        try {
            if (indexExists(idxName)) {
                scope.query("DROP INDEX `" + view.getDatabase().getCollectionPath()
                        + "`.`" + idxName + "`");
            }
            deleteMetadata(idxName);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean hasIndex(CouchbaseView view) {
        var keyColumns = view.getCategoryColumns();
        if (keyColumns.isEmpty()) return false;
        String hash = computeHash(view.getSelectionFormula(), keyColumns);
        return createdIndexes.contains("idx_nav_" + hash);
    }

    @Override
    public String getIndexName(CouchbaseView view) {
        var keyColumns = view.getCategoryColumns();
        if (keyColumns.isEmpty()) return "";
        return "idx_nav_" + computeHash(view.getSelectionFormula(), keyColumns);
    }

    // ---- TTL management ----

    /**
     * Drop all indexes whose TTL has expired.
     * Call periodically or on application startup.
     * @return number of indexes cleaned up
     */
    public int cleanupStale() {
        int cleaned = 0;
        Instant cutoff = Instant.now().minusSeconds(ttlSeconds);
        try {
            String query = "SELECT meta().id, indexName FROM " + META_COLLECTION
                    + " WHERE _type = 'domcouch.index_meta' AND lastAccessAt < $cutoff";
            QueryResult result = scope.query(query,
                    QueryOptions.queryOptions().parameters(
                            JsonObject.create().put("cutoff", cutoff.toString())));
            for (JsonObject row : result.rowsAsObject()) {
                String metaId = row.getString("id");
                String idxName = row.getString("indexName");
                try {
                    dropIndexByName(idxName);
                    deleteMetadata(metaId);
                    createdIndexes.remove(idxName);
                    cleaned++;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Metadata collection might not exist yet — ignore
        }
        return cleaned;
    }

    // ---- internal ----

    private void createIndex(String idxName, CouchbaseView view) {
        var keyColumns = view.getCategoryColumns();
        StringBuilder sb = new StringBuilder("CREATE INDEX `").append(idxName).append("`")
                .append(" ON ").append(view.getDatabase().getCollectionPath())
                .append("(");
        for (int i = 0; i < keyColumns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("items.").append(keyColumns.get(i).toUpperCase()).append("[0].`values`[0]");
        }
        sb.append(") WHERE _type = 'domcouch.document'");
        try {
            scope.query(sb.toString());
            Thread.sleep(200); // let index build asynchronously
        } catch (Exception e) {
            System.err.println("[domcouch] TTL index creation failed: " + e.getMessage());
        }
    }

    private void dropIndexByName(String idxName) {
        try {
            // Try with the default collection path — exact path depends on the database
            // Simple: drop from the default namespace
            scope.query("DROP INDEX `" + idxName + "` ON "
                    + scope.bucketName() + ".`" + scope.name() + "`.documents");
        } catch (Exception e) {
            // Index may already be gone
        }
    }

    private boolean indexExists(String idxName) {
        try {
            QueryResult r = scope.query(
                    "SELECT COUNT(*) AS cnt FROM system:indexes WHERE name = '" + idxName + "'");
            var rows = r.rowsAsObject();
            return !rows.isEmpty() && rows.get(0).getInt("cnt") > 0;
        } catch (Exception e) { return false; }
    }

    // ---- metadata CRUD ----

    private void storeMetadata(String idxName, CouchbaseView view) {
        String metaId = "meta_" + idxName;
        JsonObject meta = JsonObject.create()
                .put("_type", "domcouch.index_meta")
                .put("indexName", idxName)
                .put("selectionFormula", view.getSelectionFormula() != null ? view.getSelectionFormula() : "")
                .put("keyColumns", view.getCategoryColumns())
                .put("createdAt", Instant.now().toString())
                .put("lastAccessAt", Instant.now().toString())
                .put("ttlSeconds", ttlSeconds);
        try {
            metaCollection.upsert(metaId, meta);
        } catch (Exception e) {
            // KV upsert may fail if collection doesn't exist — metadata is optional
        }
    }

    private JsonObject loadMetadata(String idxName) {
        String metaId = "meta_" + idxName;
        try {
            var result = metaCollection.get(metaId);
            return result.contentAsObject();
        } catch (Exception e) { return null; }
    }

    private void refreshMetadata(String idxName, CouchbaseView view) {
        // Update lastAccessAt
        String metaId = "meta_" + idxName;
        try {
            String query = "UPDATE " + META_COLLECTION
                    + " SET lastAccessAt = $now WHERE meta().id = $id";
            scope.query(query, QueryOptions.queryOptions().parameters(
                    JsonObject.create()
                            .put("now", Instant.now().toString())
                            .put("id", metaId)));
        } catch (Exception ignored) {}
    }

    private void deleteMetadata(String metaId) {
        try {
            metaCollection.remove(metaId);
        } catch (Exception ignored) {}
    }

    // ---- hashing ----

    static String computeHash(String selectionFormula, List<String> keyColumns) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = (selectionFormula != null ? selectionFormula : "")
                    + "||" + String.join(",", keyColumns.stream().sorted().toList());
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            // Fallback: simple hash for environments without SHA-256
            return Integer.toHexString(Objects.hash(selectionFormula, keyColumns));
        }
    }
}
