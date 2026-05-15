package com.domcouch.api;

import java.util.List;
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

    // ---- ViewNavigator factory methods ----

    /**
     * Creates a view navigator for all entries in this view.
     */
    ViewNavigator createViewNav();

    /**
     * Creates a view navigator with a specific cache size.
     * @param cacheSize number of entries to cache (for future IIOP support)
     */
    ViewNavigator createViewNav(int cacheSize);

    /**
     * Creates a view navigator for all entries starting at a specified entry.
     */
    ViewNavigator createViewNavFrom(ViewEntry entry);

    /**
     * Creates a view navigator for entries under a specified category.
     * @param category the category value to start from (exact match on first key column)
     */
    ViewNavigator createViewNavFromCategory(String category);

    /**
     * Creates a view navigator for the immediate children of a specified entry.
     */
    ViewNavigator createViewNavFromChildren(ViewEntry entry);

    /**
     * Creates a view navigator for all the descendants of a specified entry.
     */
    ViewNavigator createViewNavFromDescendants(ViewEntry entry);

    /**
     * Creates a view navigator for all entries down to a specified category level.
     * @param maxLevel maximum category level to include (1 = top-level categories only)
     */
    ViewNavigator createViewNavMaxLevel(int maxLevel);

    /**
     * @return true if this view is categorized (has at least one key column)
     */
    boolean isCategorized();

    /**
     * @return the key column names used for categorization, or empty list
     */
    List<String> getCategoryColumns();
}
