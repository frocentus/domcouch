package com.domcouch.api;

/**
 * Base exception for Domino API errors.
 * Mirrors lotus.domino.NotesException.
 *
 * <h3>Error code ranges</h3>
 * <table>
 *   <tr><td>4000–4099</td><td>Document operation failures</td></tr>
 *   <tr><td>4000</td><td>save failed</td></tr>
 *   <tr><td>4001</td><td>remove failed</td></tr>
 *   <tr><td>4002</td><td>FTSearch failed</td></tr>
 *   <tr><td>4003</td><td>search failed</td></tr>
 *   <tr><td>4010</td><td>No author access (security)</td></tr>
 *   <tr><td>4500</td><td>Feature not applicable in Couchbase</td></tr>
 * </table>
 */
public class NotesException extends Exception {

    /** Domino-style error code — 0 means success */
    public final int id;

    public NotesException(int id, String message) {
        super(message);
        this.id = id;
    }

    public NotesException(int id, String message, Throwable cause) {
        super(message, cause);
        this.id = id;
    }

    @Override
    public String toString() {
        return "NotesException[id=" + id + ", msg=" + getMessage() + "]";
    }
}
