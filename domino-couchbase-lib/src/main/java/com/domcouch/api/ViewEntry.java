package com.domcouch.api;

import java.util.Vector;

/**
 * A single entry within a View — either a document row or a category row.
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
     * @return the Document, or null if no longer valid or if this is a category entry
     */
    Document getDocument();

    /**
     * @return the universal ID of the backing document, or null for category entries
     */
    String getUniversalID();

    /**
     * @return true if this entry references a valid document or category
     */
    boolean isValid();

    /**
     * @return the position of this entry within the flat view index (1-based)
     */
    int getPosition();

    // ---- category / hierarchy support ----

    /**
     * @return true if this entry is a category (header) row, not a document
     */
    default boolean isCategory() { return false; }

    /**
     * @return true if this entry is a document row
     */
    default boolean isDocument() { return !isCategory(); }

    /**
     * @return the category level (0 = top-level doc, N = Nth-level category)
     */
    default int getCategoryLevel() { return 0; }

    /**
     * @return the number of immediate child entries, or 0 for document entries
     */
    default int getChildCount() { return 0; }

    /**
     * @return the number of descendant entries (children + grandchildren + ...)
     */
    default int getDescendantCount() { return 0; }

    /**
     * @return the number of sibling entries at the same level
     */
    default int getSiblingCount() { return 0; }

    /**
     * @return the hierarchical position string, e.g. "3.2.1", or "" for flat views
     */
    default String getPositionString() { return ""; }

    /**
     * @return the parent entry, or null if this is a top-level entry
     */
    default ViewEntry getParentEntry() { return null; }

    /**
     * @return the first child entry, or null if none
     */
    default ViewEntry getChild() { return null; }

    /**
     * @return the next sibling at the same level, or null if none
     */
    default ViewEntry getNextSibling() { return null; }

    /**
     * @return the previous sibling at the same level, or null if none
     */
    default ViewEntry getPrevSibling() { return null; }

    /**
     * @return the footing count for this entry (total descendants at this level)
     */
    default int getFooting() { return getDescendantCount(); }
}
