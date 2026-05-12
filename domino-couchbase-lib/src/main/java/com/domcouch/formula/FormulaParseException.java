package com.domcouch.formula;

/**
 * Thrown when a formula cannot be lexed or parsed.
 * Unchecked — parse errors are programming errors, not recoverable business errors.
 */
public class FormulaParseException extends RuntimeException {

    /** Error code (see NotesException ranges: 4501–4504). */
    public final int id;

    /** Character offset where the error occurred, or -1 if unknown. */
    public final int position;

    public FormulaParseException(int id, String message, int position) {
        super(message);
        this.id = id;
        this.position = position;
    }

    public FormulaParseException(int id, String message, int position, Throwable cause) {
        super(message, cause);
        this.id = id;
        this.position = position;
    }

    @Override
    public String toString() {
        return "FormulaParseException[id=" + id + ", pos=" + position + ", msg=" + getMessage() + "]";
    }
}
