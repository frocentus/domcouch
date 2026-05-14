package com.domcouch.formula;

/**
 * Resolution context for formula evaluation.
 * <p>
 * Resolves variable names to values and supports writing/deleting document fields.
 * Implementations can back this with a Document, a Map, a Session, or a stack of scopes.
 * <p>
 * Default implementations of the optional methods throw
 * {@link ContextNotSupportedException}. The {@link Evaluator} catches this and
 * returns sensible defaults ("", 0.0, or empty list) — so a read-only context
 * that only implements {@code resolve()} still works safely with any formula.
 */
public interface FormulaContext {

    /** Resolve a variable name. Returns null if the field does not exist; returns "" for empty fields. */
    Object resolve(String name);

    /** Write a value to a document field. Default: throws {@link ContextNotSupportedException}. */
    default void setField(String name, Object value) {
        throw new ContextNotSupportedException("setField");
    }

    /** Delete a document field. Default: throws {@link ContextNotSupportedException}. */
    default void deleteField(String name) {
        throw new ContextNotSupportedException("deleteField");
    }

    /** Return all field names on the document. Default: throws {@link ContextNotSupportedException}. */
    default java.util.List<String> getFieldNames() {
        throw new ContextNotSupportedException("getFieldNames");
    }

    /** Return the document's universal ID. Default: throws {@link ContextNotSupportedException}. */
    default String getDocumentUNID() {
        throw new ContextNotSupportedException("getDocumentUNID");
    }

    /**
     * Return the current database file path (e.g. {@code "mail\harald.nsf"}).
     * Used by {@code @DbName[1]}. Default: throws {@link ContextNotSupportedException}.
     */
    default String getDatabaseName() {
        throw new ContextNotSupportedException("getDatabaseName");
    }

    /**
     * Return the server name (e.g. {@code "CN=Server/O=Org"}).
     * Used by {@code @DbName[0]} and {@code @ServerName}.
     * Default: throws {@link ContextNotSupportedException}.
     */
    default String getServerName() {
        throw new ContextNotSupportedException("getServerName");
    }

    /**
     * Return the database title (e.g. {@code "Personnel Records"}).
     * Used by {@code @DbTitle}. Default: throws {@link ContextNotSupportedException}.
     */
    default String getDatabaseTitle() {
        throw new ContextNotSupportedException("getDatabaseTitle");
    }

    /**
     * Return the 16-character hex replica ID (e.g. {@code "85255B6E004A6D12"}).
     * Used by {@code @ReplicaID}. Default: throws {@link ContextNotSupportedException}.
     */
    default String getReplicaID() {
        throw new ContextNotSupportedException("getReplicaID");
    }

    // ---- Document metadata ----

    /**
     * Return the document size in bytes. Used by {@code @DocLength}.
     * Default: throws {@link ContextNotSupportedException}.
     */
    default long getDocumentSize() {
        throw new ContextNotSupportedException("getDocumentSize");
    }

    /**
     * Return the number of file attachments. Used by {@code @Attachments}.
     * Default: throws {@link ContextNotSupportedException}.
     */
    default int getAttachmentCount() {
        throw new ContextNotSupportedException("getAttachmentCount");
    }

    /**
     * Return folder names containing this document. Used by {@code @WhichFolders}.
     * Default: throws {@link ContextNotSupportedException}.
     */
    default java.util.List<String> getFolderNames() {
        throw new ContextNotSupportedException("getFolderNames");
    }

    /**
     * Return whether the document is valid (not deleted). Used by {@code @IsValid}.
     * Default: throws {@link ContextNotSupportedException}.
     */
    default boolean isDocumentValid() {
        throw new ContextNotSupportedException("isDocumentValid");
    }

    // ---- Document locking ----

    /** Lock the document. Used by {@code @DocLock("LOCK")}. Default: throws. */
    default boolean lockDocument() {
        throw new ContextNotSupportedException("lockDocument");
    }

    /** Unlock the document. Used by {@code @DocLock("UNLOCK")}. Default: throws. */
    default boolean unlockDocument() {
        throw new ContextNotSupportedException("unlockDocument");
    }

    /** Return the lock holder name or "". Used by {@code @DocLock("STATUS")}. Default: throws. */
    default String getDocumentLockStatus() {
        throw new ContextNotSupportedException("getDocumentLockStatus");
    }

    /** Return whether document locking is enabled. Used by {@code @DocLock("LOCKINGENABLED")}. Default: throws. */
    default boolean isDocumentLockingEnabled() {
        throw new ContextNotSupportedException("isDocumentLockingEnabled");
    }

    // ---- Session / environment ----

    /**
     * Return the Domino domain (e.g. {@code "MyOrg"}). Used by {@code @Domain}.
     * Default: throws {@link ContextNotSupportedException}.
     */
    default String getDomain() {
        throw new ContextNotSupportedException("getDomain");
    }

    /**
     * Return the value of a notes.ini / environment variable.
     * Used by {@code @Environment(name)}. Default: throws {@link ContextNotSupportedException}.
     */
    default String getEnvironmentValue(String name) {
        throw new ContextNotSupportedException("getEnvironmentValue");
    }

    // ---- Document lifecycle ----

    /** Mark the document for deletion. Used by {@code @DeleteDocument}. Default: throws. */
    default void markForDeletion() {
        throw new ContextNotSupportedException("markForDeletion");
    }

    /** Unmark the document for deletion. Used by {@code @UndeleteDocument}. Default: throws. */
    default void unmarkForDeletion() {
        throw new ContextNotSupportedException("unmarkForDeletion");
    }

    /** Permanently delete the document. Used by {@code @HardDeleteDocument}. Default: throws. */
    default void hardDelete() {
        throw new ContextNotSupportedException("hardDelete");
    }

    /** Add this document to a folder. Used by {@code @AddToFolder}. Default: throws. */
    default void addToFolder(String folderName) {
        throw new ContextNotSupportedException("addToFolder");
    }
}
