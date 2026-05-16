package com.domcouch.impl;

import com.domcouch.api.View;

/**
 * Service for managing N1QL indexes used by categorized ViewNavigators.
 * <p>
 * Implementations control the lifecycle of sort indexes — creation, reuse,
 * and cleanup. The default {@link SimpleViewIndexService} creates indexes
 * on first navigator access and drops them on {@link #dropIndex}.
 * <p>
 * Swap implementations for different strategies:
 * <ul>
 *   <li>TTL-based: auto-drop after idle time</li>
 *   <li>Pre-warming: create all indexes on database open</li>
 *   <li>No-op: skip index creation (fall back to full scan)</li>
 * </ul>
 */
public interface ViewIndexService {

    /**
     * Ensure an optimized index exists for this view's key columns.
     * Called before the first ORDER BY query in a ViewNavigator.
     * Must be idempotent — subsequent calls are no-ops if the index exists.
     *
     * @param view the categorized view
     * @return name of the index (for diagnostic/logging)
     */
    String ensureIndex(CouchbaseView view);

    /**
     * Drop the index for this view. Called when the navigator is recycled.
     *
     * @param view the categorized view
     */
    void dropIndex(CouchbaseView view);

    /**
     * Check whether an index exists for this view.
     *
     * @param view the categorized view
     */
    boolean hasIndex(CouchbaseView view);

    /**
     * Build a predictable index name from the view name.
     * @param view the view
     * @return N1QL-safe index name
     */
    default String getIndexName(CouchbaseView view) {
        return "idx_nav_" + view.getName().replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
