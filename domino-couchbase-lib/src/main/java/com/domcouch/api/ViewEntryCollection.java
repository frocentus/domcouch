package com.domcouch.api;

import java.util.Iterator;

/**
 * A collection of ViewEntry objects, typically obtained from a View.
 * Mirrors lotus.domino.ViewEntryCollection.
 */
public interface ViewEntryCollection extends Iterable<ViewEntry> {

    /**
     * @return the first entry in the collection, or null if empty
     */
    ViewEntry getFirstEntry();

    /**
     * @return the next entry in the collection, or null after the last entry
     */
    ViewEntry getNextEntry();

    /**
     * Get an entry by its 1-based position.
     *
     * @param n 1-based position
     * @return the entry at position n, or null
     */
    ViewEntry getNthEntry(int n);

    /**
     * @return number of entries in this collection
     */
    int getCount();

    /**
     * Reset the internal cursor so getNextEntry() starts from the beginning.
     */
    void reset();

    /**
     * @return an iterator over all remaining entries
     */
    @Override
    Iterator<ViewEntry> iterator();

    /**
     * Release Couchbase resources.
     */
    void recycle();
}
