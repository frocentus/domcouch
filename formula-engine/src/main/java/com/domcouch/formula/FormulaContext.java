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

    /** Return the current database name. Default: throws {@link ContextNotSupportedException}. */
    default String getDatabaseName() {
        throw new ContextNotSupportedException("getDatabaseName");
    }
}
