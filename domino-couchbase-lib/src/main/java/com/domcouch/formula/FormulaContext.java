package com.domcouch.formula;

/**
 * Resolution context for formula evaluation.
 * <p>
 * Resolves variable names to values and supports writing/deleting document fields.
 * Implementations can back this with a Document, a Map, a Session, or a stack of scopes.
 */
public interface FormulaContext {

    /** Resolve a variable name (case-insensitive — names are upper-cased by the Lexer). */
    Object resolve(String name);

    /** Write a value to a document field. Default: throws. */
    default void setField(String name, Object value) {
        throw new UnsupportedOperationException("setField not supported");
    }

    /** Delete a document field. Default: throws. */
    default void deleteField(String name) {
        throw new UnsupportedOperationException("deleteField not supported");
    }
}
