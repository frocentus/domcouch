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
     * @return the last entry in the collection, or null if empty
     */
    ViewEntry getLastEntry();

    /**
     * @return the next entry in the collection, or null after the last entry
     */
    ViewEntry getNextEntry();

    /**
     * @return the previous entry in the collection, or null before the first entry
     */
    ViewEntry getPrevEntry();

    /**
     * Get an entry by its 1-based position.
     *
     * @param n 1-based position
     * @return the entry at position n, or null
     */
    ViewEntry getNthEntry(int n);

    /**
     * Look up an entry by object reference or key.
     *
     * @param entry a ViewEntry object or a key value
     * @return the matching entry, or null
     */
    ViewEntry getEntry(Object entry);

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
