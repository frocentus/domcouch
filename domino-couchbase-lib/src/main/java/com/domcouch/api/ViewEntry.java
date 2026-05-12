package com.domcouch.api;

import java.util.Vector;

/**
 * A single document entry within a View.
 * Mirrors lotus.domino.ViewEntry.
 */
public interface ViewEntry {

    /**
     * @return the column values for this entry, or an empty Vector
     */
    Vector<Object> getColumnValues();

    /**
     * @param index 0-based column index
     * @return the column value at the given index
     */
    Object getColumnValue(int index);

    /**
     * Open the backing Document for this entry.
     *
     * @return the Document, or null if no longer valid
     */
    Document getDocument();

    /**
     * @return the universal ID of the backing document
     */
    String getUniversalID();

    /**
     * @return true if this entry references a valid document
     */
    boolean isValid();

    /**
     * @return the position of this entry within the collection (1-based)
     */
    int getPosition();
}
