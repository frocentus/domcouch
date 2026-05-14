package com.domcouch.formula;

/**
 * Thrown by {@link FormulaContext} methods when the operation is not
 * implemented by this context.
 * <p>
 * The {@link Evaluator} catches this exception in every @Function handler
 * that depends on context methods and returns a sensible default (empty
 * string, 0.0, or empty list). This allows contexts to implement only the
 * operations they support — the formula engine degrades gracefully.
 * <p>
 * Example: a read-only context that only implements {@code resolve()} will
 * cause {@code @SetField} to return its argument unchanged and
 * {@code @DocumentUniqueID} to return "" — no crash.
 */
public class ContextNotSupportedException extends RuntimeException {

    private final String operation;

    /**
     * @param operation the name of the context method that is not supported
     *                  (e.g. "setField", "getDocumentUNID")
     */
    public ContextNotSupportedException(String operation) {
        super("FormulaContext does not support: " + operation);
        this.operation = operation;
    }

    /** @return the name of the unsupported operation */
    public String getOperation() {
        return operation;
    }
}
