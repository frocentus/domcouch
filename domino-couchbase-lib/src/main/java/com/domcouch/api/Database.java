package com.domcouch.api;

import java.util.Vector;

/**
 * A Domino-style Database backed by a Couchbase scope.
 * Mirrors lotus.domino.Database.
 */
public interface Database {

    /**
     * @return the display title of this database
     */
    String getTitle();

    /**
     * Set a display title for this database.
     */
    void setTitle(String title);

    /**
     * @return the file path / name of this database (maps to Couchbase scope name)
     */
    String getFileName();

    /**
     * @return true if the database is open and connected
     */
    boolean isOpen();

    /**
     * Create a new, unsaved Document.
     *
     * @return a fresh Document with a generated UNID
     */
    Document createDocument();

    /**
     * Retrieve a document by its universal ID.
     *
     * @param unid the 32-character universal ID
     * @return the Document, or null if not found
     */
    Document getDocumentByUNID(String unid);

    /**
     * Retrieve or lazily create a View by name.
     *
     * @param name the view name
     * @return the View object
     */
    View getView(String name);

    /**
     * Create a new View backed by a N1QL index.
     *
     * @param name            the view name
     * @param selectionFormula SQL-like WHERE clause that selects documents
     * @return the newly created View
     */
    View createView(String name, String selectionFormula);

    /**
     * Create a new View with an explicit key column for lookups.
     *
     * @param name            the view name
     * @param selectionFormula SQL-like WHERE clause that selects documents
     * @param keyItemName     the Domino item name used as the lookup key (e.g., "LastName")
     * @return the newly created View
     */
    View createView(String name, String selectionFormula, String keyItemName);

    /**
     * Full-text search across all documents in the database.
     * Delegates to Couchbase FTS.
     *
     * @param query the full-text query
     * @return matching documents as a DocumentCollection
     */
    DocumentCollection FTSearch(String query) throws NotesException;

    /**
     * Full-text search with a document limit.
     *
     * @param query   the full-text query
     * @param maxDocs maximum documents to return
     * @return matching documents
     */
    DocumentCollection FTSearch(String query, int maxDocs) throws NotesException;

    /**
     * Run a selection-formula query and return matching documents.
     *
     * @param formula the selection formula (WHERE clause style)
     * @return matching documents as a DocumentCollection
     */
    DocumentCollection search(String formula) throws NotesException;

    /**
     * @return a collection of all documents in this database
     */
    DocumentCollection getAllDocuments();

    /**
     * @return the number of documents in this database
     */
    int getDocumentCount();

    /**
     * Release Couchbase resources held by this database.
     */
    void recycle();

    // ---- Not applicable in Couchbase ----

    /**
     * Static factory: open a database on a specific Domino server by file path.
     * <p>
     * <b>Not applicable in Couchbase.</b> Couchbase databases (buckets) have
     * unique names within a cluster — there is no server+path addressing.
     * Open databases via {@link Session#getDatabase(String)} instead.
     *
     * @throws NotesException always — error code 4500
     */
    static Database open(String server, String dbFile) throws NotesException {
        throw new NotesException(4500,
                "Database.open(server, dbFile) is not applicable in Couchbase. " +
                "Use session.getDatabase(\"" + dbFile + "\") instead. " +
                "Couchbase buckets have unique names per cluster — no server/path addressing exists.");
    }

    /**
     * Static factory: open a database by its replica ID on a specific server.
     * <p>
     * <b>Not applicable in Couchbase.</b> Domino replica IDs identify
     * databases across servers for replication. Couchbase has no replica ID
     * concept — XDCR replicates entire buckets by name. To access a replicated
     * bucket on another cluster, create a new {@code Session} pointing to that
     * cluster and open the bucket by name.
     *
     * @throws NotesException always — error code 4500
     */
    static Database openByReplicaID(String server, String replicaId) throws NotesException {
        throw new NotesException(4500,
                "Database.openByReplicaID(server, replicaId) is not applicable in Couchbase. " +
                "Couchbase has no replica ID concept — XDCR replicates buckets by name. " +
                "Open a Session against the target cluster and call getDatabase(name) instead.");
    }
}
