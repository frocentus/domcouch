package com.domcouch.api;

import com.couchbase.client.java.Cluster;

/**
 * Entry-point into the Domino-style API.
 * Establish a session against a Couchbase cluster, then open Databases.
 * Mirrors lotus.domino.Session.
 */
public interface Session {

    /**
     * Factory: create a Session connected to a Couchbase cluster.
     *
     * @param connectionString e.g. "couchbase://localhost"
     * @param username         cluster username
     * @param password         cluster password
     * @return a connected Session
     */
    static Session createSession(String connectionString, String username, String password) {
        return com.domcouch.impl.CouchbaseSession.connect(connectionString, username, password);
    }

    /**
     * Open a Database by name. In Couchbase terms, the databaseName maps to a scope
     * within the configured bucket.
     *
     * @param databaseName the Domino-style database name (maps to Couchbase scope)
     * @return an open Database handle
     * @throws NotesException if the scope / bucket cannot be accessed
     */
    Database getDatabase(String databaseName) throws NotesException;

    /**
     * Open a Database within a specific bucket.
     *
     * @param bucketName   the Couchbase bucket name
     * @param databaseName the scope / database name
     * @return an open Database handle
     * @throws NotesException on connection error
     */
    Database getDatabase(String bucketName, String databaseName) throws NotesException;

    /**
     * @return the username used to create this session
     */
    String getUserName();

    /**
     * @return true if the session is still connected to the cluster
     */
    boolean isValid();

    /**
     * Release all Couchbase resources and disconnect.
     */
    void recycle();

    /**
     * Internal: expose the underlying Couchbase Cluster for advanced usage.
     * Library-internal use only.
     */
    Cluster getNativeCluster();

    // ---- Not applicable in Couchbase ----

    /**
     * Create a session with the server's identity (trusted session).
     * <p>
     * <b>Not applicable in Couchbase.</b> Couchbase authenticates per-cluster
     * via username/password, certificates, or client certificates. There is
     * no "trusted server" identity concept.
     *
     * @throws NotesException always — error code 4500
     */
    static Session createTrustedSession() throws NotesException {
        throw new NotesException(4500,
                "createTrustedSession() is not applicable in Couchbase. " +
                "Use Session.createSession(connectionString, username, password) instead.");
    }
}
