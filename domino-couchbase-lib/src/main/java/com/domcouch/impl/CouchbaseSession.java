package com.domcouch.impl;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.bucket.BucketType;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.domcouch.api.Database;
import com.domcouch.api.NotesException;
import com.domcouch.api.Session;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Couchbase-backed Session — the entry point for the Domino-style API.
 * <p>
 * Database mapping:
 * <ul>
 *   <li>{@code getDatabase("contacts")} → bucket <b>contacts</b>, scope {@code data}</li>
 *   <li>{@code getDatabase("mybucket", "myscope")} → bucket <b>mybucket</b>, scope <b>myscope</b></li>
 * </ul>
 * Each database is an isolated bucket by default, matching Domino's per-.nsf isolation.
 * The two-arg overload supports the legacy scope-per-database pattern.
 */
public class CouchbaseSession implements Session {

    private static final int DEFAULT_BUCKET_RAM_MB = 100;

    private final Cluster cluster;
    private final String username;
    private final int bucketRamQuotaMB;
    private final Map<String, CouchbaseDatabase> databases;

    private CouchbaseSession(Cluster cluster, String username, int bucketRamQuotaMB) {
        this.cluster = cluster;
        this.username = username;
        this.bucketRamQuotaMB = bucketRamQuotaMB;
        this.databases = new ConcurrentHashMap<>();
    }

    /**
     * Connect to a Couchbase cluster with default bucket RAM quota (100 MB).
     */
    public static CouchbaseSession connect(String connectionString,
                                           String username, String password) {
        return connect(connectionString, username, password, DEFAULT_BUCKET_RAM_MB);
    }

    /**
     * Connect to a Couchbase cluster with a custom bucket RAM quota.
     * <p>
     * Each call to {@link #getDatabase(String)} creates a new bucket with this
     * RAM quota (if the bucket doesn't already exist). For production, pre-create
     * buckets via the Couchbase admin UI or REST API and use the two-arg
     * {@link #getDatabase(String, String)} overload instead.
     *
     * @param connectionString e.g. "couchbase://localhost"
     * @param username         cluster username
     * @param password         cluster password
     * @param bucketRamQuotaMB RAM quota per auto-created bucket (MB)
     * @return a connected Session
     */
    public static CouchbaseSession connect(String connectionString,
                                           String username, String password,
                                           int bucketRamQuotaMB) {
        Cluster cluster = Cluster.connect(connectionString,
                ClusterOptions.clusterOptions(username, password)
                        .environment(env -> env.timeoutConfig(tc ->
                                tc.connectTimeout(Duration.ofSeconds(30))
                                        .queryTimeout(Duration.ofSeconds(30))
                        )));
        return new CouchbaseSession(cluster, username, bucketRamQuotaMB);
    }

    /**
     * Open a Database by name.
     * <p>
     * Creates (or reuses) a dedicated Couchbase <b>bucket</b> named after the
     * database, with a fixed scope {@value com.domcouch.impl.CouchbaseDatabase#DEFAULT_SCOPE}.
     * This provides true isolation — each database is its own bucket, matching
     * Domino's per-.nsf model.
     */
    @Override
    public Database getDatabase(String databaseName) throws NotesException {
        CouchbaseDatabase db = databases.get(databaseName);
        if (db != null) return db;
        try {
            ensureBucketExists(databaseName);
            ensureScopeExists(databaseName, CouchbaseDatabase.DEFAULT_SCOPE);
            db = new CouchbaseDatabase(cluster, databaseName, CouchbaseDatabase.DEFAULT_SCOPE);
            db.setCurrentUserName(username);
            databases.put(databaseName, db);
            return db;
        } catch (Exception e) {
            throw new NotesException(4000, "Cannot open database: " + databaseName, e);
        }
    }

    /**
     * Open a Database within a specific pre-existing bucket.
     * Uses scope {@code databaseName} within bucket {@code bucketName}.
     * Does NOT auto-create buckets — the bucket must already exist.
     */
    @Override
    public Database getDatabase(String bucketName, String databaseName) throws NotesException {
        String key = bucketName + "/" + databaseName;
        CouchbaseDatabase db = databases.get(key);
        if (db != null) return db;
        try {
            ensureScopeExists(bucketName, databaseName);
            db = new CouchbaseDatabase(cluster, bucketName, databaseName);
            db.setCurrentUserName(username);
            databases.put(key, db);
            return db;
        } catch (Exception e) {
            throw new NotesException(4000, "Cannot open database: " + key, e);
        }
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public boolean isValid() {
        try {
            cluster.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void recycle() {
        databases.values().forEach(CouchbaseDatabase::recycle);
        databases.clear();
        try {
            cluster.disconnect();
        } catch (Exception ignored) {}
    }

    @Override
    public Cluster getNativeCluster() {
        return cluster;
    }

    // ---- internal ----

    /**
     * Create a Couchbase bucket if it doesn't already exist.
     * Bucket creation is idempotent (skips if the bucket already exists).
     * <p>
     * Creates a Couchbase bucket with flush enabled, no replicas, and
     * the configured RAM quota. Waits up to 30 seconds for the bucket
     * to become available.
     */
    void ensureBucketExists(String bucketName) {
        BucketManager bucketManager = cluster.buckets();

        // Check if bucket already exists
        try {
            var allBuckets = bucketManager.getAllBuckets();
            if (allBuckets.containsKey(bucketName)) {
                return;
            }
        } catch (Exception e) {
            // Proceed to create — may fail if it already exists
        }

        BucketSettings settings = BucketSettings.create(bucketName)
                .ramQuotaMB(bucketRamQuotaMB)
                .bucketType(BucketType.COUCHBASE)
                .flushEnabled(true)
                .numReplicas(0);
        bucketManager.createBucket(settings);

        // Poll until the bucket is available (max 30 seconds)
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                cluster.bucket(bucketName);
                return; // bucket is ready
            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
            }
        }
        throw new RuntimeException("Bucket '" + bucketName + "' did not become ready within 30 seconds");
    }

    private void ensureScopeExists(String bucketName, String scopeName) {
        Bucket bucket = cluster.bucket(bucketName);
        CollectionManager colMgr = bucket.collections();

        boolean scopeExists = false;
        try {
            for (ScopeSpec spec : colMgr.getAllScopes()) {
                if (spec.name().equals(scopeName)) {
                    scopeExists = true;
                    break;
                }
            }
        } catch (Exception e) {
            scopeExists = false;
        }

        if (!scopeExists) {
            colMgr.createScope(scopeName);
        }

        // Ensure the "documents" collection exists in the scope
        boolean collExists = false;
        try {
            for (ScopeSpec spec : colMgr.getAllScopes()) {
                if (spec.name().equals(scopeName)) {
                    for (CollectionSpec cs : spec.collections()) {
                        if (cs.name().equals(CouchbaseDatabase.COLLECTION_NAME)) {
                            collExists = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            collExists = false;
        }

        if (!collExists) {
            colMgr.createCollection(scopeName, CouchbaseDatabase.COLLECTION_NAME);
            // Brief pause to let the collection become available
            try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }
}
