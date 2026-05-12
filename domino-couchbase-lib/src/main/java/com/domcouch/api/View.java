package com.domcouch.api;

import java.util.Vector;

/**
 * A View which may contain a collection of ViewEntry objects.
 * Mirrors lotus.domino.View.
 */
public interface View {

    /**
     * @return the programmatic name of this view
     */
    String getName();

    /**
     * @return all entries in this view
     */
    ViewEntryCollection getAllEntries();

    /**
     * @param key the key to look up (exact match on first sorted column)
     * @return entries matching the given key
     */
    ViewEntryCollection getAllEntriesByKey(Object key);

    /**
     * @param key the key to look up
     * @return the first entry matching the given key, or null
     */
    ViewEntry getEntryByKey(Object key);

    /**
     * Full-text search within this view's documents.
     *
     * @param query the FTS query string
     * @return entry collection with ranked results
     */
    ViewEntryCollection FTSearch(String query);

    /**
     * @param maxDocs maximum number of documents to return
     * @return entry collection with ranked results, capped at maxDocs
     */
    ViewEntryCollection FTSearch(String query, int maxDocs);

    /**
     * @return total number of entries in this view
     */
    int getEntryCount();

    /**
     * Refresh the view index so it reflects recent changes.
     */
    void refresh();

    /**
     * Release Couchbase resources held by this view.
     */
    void recycle();
}
