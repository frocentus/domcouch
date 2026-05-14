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
}
