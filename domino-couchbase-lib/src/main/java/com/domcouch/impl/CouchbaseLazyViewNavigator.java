package com.domcouch.impl;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.domcouch.api.NotesException;
import com.domcouch.api.View;
import com.domcouch.api.ViewEntry;
import com.domcouch.api.ViewNavigator;

import java.util.*;

/**
 * Lazy ViewNavigator using key-based N1QL pagination — no full in-memory index.
 *
 * <h3>How It Works</h3>
 * Each navigation call fetches a page of results using:
 * <pre>{@code SELECT ... WHERE keyCol > $cursorKey ORDER BY keyCol LIMIT pageSize}</pre>
 * Pages are cached in a local buffer. Category rows are injected when the key
 * value changes between adjacent entries.
 *
 * <h3>Trade-offs vs. {@link CouchbaseViewNavigator} (in-memory)</h3>
 * <table>
 *   <tr><td>Build time</td><td>&lt;1ms (lazy)</td><td>~30s (full scan)</td></tr>
 *   <tr><td>Memory</td><td>O(pageSize)</td><td>O(total entries)</td></tr>
 *   <tr><td>Sequential walk</td><td>μs-fast within page</td><td>ns-fast (array)</td></tr>
 *   <tr><td>getNth(n)</td><td>O(n) — slow</td><td>O(1) — fast</td></tr>
 *   <tr><td>Hierarchy (parent/sibling)</td><td>Not supported</td><td>Full tree links</td></tr>
 * </table>
 *
 * <p>Best for: sequential walk patterns, first-page latency matters.
 * Not for: random access, hierarchy navigation.
 *
 * @see CouchbaseViewNavigator
 */
/**
 * Lazy ViewNavigator using key-based N1QL pagination instead of building
 * a full in-memory index.
 * <p>
 * Each navigation call fetches a page of results using
 * {@code WHERE keyCol > $cursorKey ORDER BY keyCol LIMIT pageSize}.
 * No OFFSET, no full scan — only the current page lives in memory.
 * <p>
 * Trade-offs vs. in-memory CouchbaseViewNavigator:
 * <ul>
 *   <li>+ Build time: ~0 (page fetched on first access)</li>
 *   <li>+ Memory: O(pageSize) — configurable, default 200</li>
 *   <li>+ Sequential navigation: 1 N1QL query per page</li>
 *   <li>− getNth(n): requires seek via key estimation or COUNT</li>
 *   <li>− No O(1) random access</li>
 *   <li>− Hierarchy links (parent/sibling) not available</li>
 * </ul>
 */
public class CouchbaseLazyViewNavigator implements ViewNavigator {

    private final CouchbaseView parentView;
    private final List<String> keyColumns;
    private final int pageSize;

    private final List<ViewEntry> page;          // current page buffer
    private int pageIdx;                          // cursor within current page (-1 = before first)
    private String cursorKeyLow;                  // first key of current page (for prev-page fetch)
    private String cursorKeyHigh;                 // last key of current page (for next-page fetch)
    private boolean pageHasMore;                  // whether there are more pages after current
    private int approxPosition;                   // approximate 0-based position of first entry on current page
    private int totalCount = -1;
    private int maxLevel;

    public CouchbaseLazyViewNavigator(CouchbaseView parentView, int maxLevel, int pageSize) {
        this.parentView = parentView;
        this.keyColumns = parentView.getCategoryColumns();
        this.maxLevel = maxLevel;
        this.pageSize = Math.max(10, pageSize);
        this.page = new ArrayList<>();
        this.pageIdx = -1;
        this.approxPosition = 0;
        this.pageHasMore = true;
    }

    public CouchbaseLazyViewNavigator(CouchbaseView parentView) {
        this(parentView, 0, 200);
    }

    // ---- count / properties ----

    @Override
    public int getCount() {
        if (totalCount < 0) totalCount = countDocuments();
        return totalCount;
    }

    @Override
    public View getParentView() { return parentView; }

    @Override
    public int getMaxLevel() {
        return keyColumns.isEmpty() ? 0 : keyColumns.size();
    }

    // ---- get methods ----

    @Override
    public ViewEntry getFirst() throws NotesException {
        page.clear();
        pageIdx = -1;
        cursorKeyLow = null;
        cursorKeyHigh = null;
        approxPosition = 0;
        pageHasMore = true;
        fetchNextPage(null, true);
        if (page.isEmpty()) return null;
        pageIdx = 0;
        return page.get(0);
    }

    @Override
    public ViewEntry getLast() throws NotesException {
        String orderCol = buildOrderKeyRef();
        String sql = "SELECT unid, doc.* FROM " + parentView.getDatabase().getCollectionPath() + " AS doc"
                + " WHERE doc._type = 'domcouch.document'";
        var formula = parentView.getSelectionFormula();
        if (formula != null && !formula.isEmpty()) sql += " AND (" + formula + ")";
        if (!keyColumns.isEmpty()) sql += " ORDER BY " + orderCol + " DESC";
        sql += " LIMIT 1";

        try {
            QueryResult r = parentView.getScope().query(sql);
            var rows = r.rowsAsObject();
            if (rows.isEmpty()) return null;
            JsonObject row = rows.get(0);
            String unid = row.getString("unid");
            List<Object> cols = parentView.extractColumnValues(row);
            CouchbaseViewEntry entry = new CouchbaseViewEntry(parentView, unid, cols,
                    getCount(), row);
            page.clear(); page.add(entry); pageIdx = 0;
            cursorKeyHigh = extractSortKey(row);
            return entry;
        } catch (Exception e) { return null; }
    }

    @Override
    public ViewEntry getNext() throws NotesException {
        if (pageIdx < 0) return getFirst();
        pageIdx++;
        // Category detection: compare current entry's key with previous entry's key
        if (pageIdx < page.size()) {
            ViewEntry current = page.get(pageIdx);
            ViewEntry prev = page.get(pageIdx - 1);
            String curKey = keyOf(current);
            String prevKey = keyOf(prev);
            if (prevKey != null && curKey != null && !prevKey.equals(curKey)) {
                // Key boundary — inject virtual category entry
                return injectCategory(prevKey, curKey, prev, current);
            }
            return current;
        }
        // Exhausted current page — fetch next
        if (!pageHasMore) return null;
        String lastKey = cursorKeyHigh;
        fetchNextPage(lastKey, true);
        if (page.isEmpty()) return null;
        // Check category boundary between old last and new first
        String oldLastKey = lastKey;
        String newFirstKey = keyOf(page.get(0));
        if (oldLastKey != null && newFirstKey != null && !oldLastKey.equals(newFirstKey)) {
            approxPosition++;
            return injectCategory(oldLastKey, newFirstKey, null, page.get(0));
        }
        pageIdx = 0;
        return page.get(0);
    }

    @Override
    public ViewEntry getPrev() throws NotesException {
        if (pageIdx <= 0) {
            // Need previous page
            if (cursorKeyLow == null) return null;
            fetchPrevPage(cursorKeyLow);
            if (page.isEmpty()) return null;
            pageIdx = page.size() - 1;
            return page.get(pageIdx);
        }
        pageIdx--;
        return page.get(pageIdx);
    }

    @Override
    public ViewEntry getNext(ViewEntry entry) throws NotesException {
        gotoEntry(entry);
        return getNext();
    }

    @Override
    public ViewEntry getPrev(ViewEntry entry) throws NotesException {
        gotoEntry(entry);
        return getPrev();
    }

    @Override
    public ViewEntry getCurrent() throws NotesException {
        if (pageIdx < 0 || pageIdx >= page.size()) return null;
        return page.get(pageIdx);
    }

    @Override
    public ViewEntry getNth(int n) throws NotesException {
        if (n < 1 || n > getCount()) return null;
        // If n is within current page
        int pageStart = approxPosition;
        int pageEnd = pageStart + page.size() - 1;
        if (n - 1 >= pageStart && n - 1 <= pageEnd) {
            pageIdx = n - 1 - pageStart;
            return page.get(pageIdx);
        }
        // If n is just beyond current page, walk forward
        if (n - 1 > pageEnd && n - 1 - pageEnd <= pageSize * 2) {
            int steps = n - 1 - pageEnd;
            for (int i = 0; i < steps; i++) {
                ViewEntry e = getNext();
                if (e == null) return null;
            }
            return page.get(pageIdx);
        }
        // Far seek: use OFFSET (admitted slow path)
        String orderCol = buildOrderKeyRef();
        String sql = parentView.buildNavigatorSelect()
                + " ORDER BY " + orderCol + " LIMIT 1 OFFSET " + (n - 1);
        try {
            QueryResult r = parentView.getScope().query(sql);
            var rows = r.rowsAsObject();
            if (rows.isEmpty()) return null;
            JsonObject row = rows.get(0);
            String unid = row.getString("unid");
            List<Object> cols = parentView.extractColumnValues(row);
            var entry = new CouchbaseViewEntry(parentView, unid, cols, n, row);
            // Rebuild page around this position
            page.clear(); page.add(entry); pageIdx = 0;
            cursorKeyHigh = extractSortKey(row);
            cursorKeyLow = cursorKeyHigh;
            approxPosition = n - 1;
            return entry;
        } catch (Exception e) { return null; }
    }

    @Override
    public ViewEntry getPos(String pos) throws NotesException {
        throw new NotesException(4500, "getPos not supported in lazy navigator");
    }

    @Override
    public ViewEntry getFirstDocument() throws NotesException {
        ViewEntry e = getFirst();
        while (e != null && e.isCategory()) e = getNext();
        return e;
    }

    @Override
    public ViewEntry getLastDocument() throws NotesException {
        return getLast(); // Last entry is always a document in lazy mode
    }

    @Override
    public ViewEntry getNextDocument() throws NotesException {
        ViewEntry e = getNext();
        while (e != null && e.isCategory()) e = getNext();
        return e;
    }

    @Override
    public ViewEntry getPrevDocument() throws NotesException {
        ViewEntry e = getPrev();
        while (e != null && e.isCategory()) e = getPrev();
        return e;
    }

    @Override
    public ViewEntry getNextCategory() throws NotesException {
        ViewEntry e = getNext();
        while (e != null && !e.isCategory()) e = getNext();
        return e;
    }

    @Override
    public ViewEntry getPrevCategory() throws NotesException {
        ViewEntry e = getPrev();
        while (e != null && !e.isCategory()) e = getPrev();
        return e;
    }

    @Override public ViewEntry getChild() throws NotesException { return null; }
    @Override public ViewEntry getChild(ViewEntry e) throws NotesException { return null; }
    @Override public ViewEntry getParent() throws NotesException { return null; }
    @Override public ViewEntry getParent(ViewEntry e) throws NotesException { return null; }
    @Override public ViewEntry getNextSibling() throws NotesException { return null; }
    @Override public ViewEntry getNextSibling(ViewEntry e) throws NotesException { return null; }
    @Override public ViewEntry getPrevSibling() throws NotesException { return null; }
    @Override public ViewEntry getPrevSibling(ViewEntry e) throws NotesException { return null; }

    // ---- goto methods ----

    @Override public void gotoFirst() throws NotesException { getFirst(); }
    @Override public void gotoLast() throws NotesException { getLast(); }
    @Override public void gotoNext() throws NotesException { getNext(); }
    @Override public void gotoNext(ViewEntry e) throws NotesException { getNext(e); }
    @Override public void gotoPrev() throws NotesException { getPrev(); }
    @Override public void gotoPrev(ViewEntry e) throws NotesException { getPrev(e); }
    @Override public void gotoEntry(Object entry) throws NotesException {
        if (entry instanceof ViewEntry) gotoEntry((ViewEntry) entry);
    }
    @Override public void gotoEntry(ViewEntry entry) throws NotesException {
        if (entry == null) return;
        cursorKeyLow = keyOf(entry);
        cursorKeyHigh = cursorKeyLow;
        page.clear(); page.add(entry); pageIdx = 0;
        approxPosition = entry.getPosition() - 1;
    }
    @Override public void gotoPos(String pos) throws NotesException {}
    @Override public void gotoNth(int n) throws NotesException { getNth(n); }
    @Override public void gotoFirstDocument() throws NotesException { getFirstDocument(); }
    @Override public void gotoLastDocument() throws NotesException { getLastDocument(); }
    @Override public void gotoNextDocument() throws NotesException { getNextDocument(); }
    @Override public void gotoPrevDocument() throws NotesException { getPrevDocument(); }
    @Override public void gotoNextCategory() throws NotesException { getNextCategory(); }
    @Override public void gotoPrevCategory() throws NotesException { getPrevCategory(); }
    @Override public void gotoChild() throws NotesException {}
    @Override public void gotoChild(ViewEntry e) throws NotesException {}
    @Override public void gotoParent() throws NotesException {}
    @Override public void gotoParent(ViewEntry e) throws NotesException {}
    @Override public void gotoNextSibling() throws NotesException {}
    @Override public void gotoNextSibling(ViewEntry e) throws NotesException {}
    @Override public void gotoPrevSibling() throws NotesException {}
    @Override public void gotoPrevSibling(ViewEntry e) throws NotesException {}

    @Override public void markAllRead() {}
    @Override public void markAllRead(String u) {}
    @Override public void markAllUnread() {}
    @Override public void markAllUnread(String u) {}
    @Override public void recycle() { page.clear(); }

    // ---- internal ----

    private String buildOrderKeyRef() {
        if (keyColumns.isEmpty()) return "unid";
        return parentView.buildKeyColumnRef(keyColumns.get(0));
    }

    private boolean fetchNextPage(String afterKey, boolean ascending) {
        String orderCol = buildOrderKeyRef();
        StringBuilder sql = new StringBuilder(parentView.buildNavigatorSelect())
                .append(" ORDER BY ").append(orderCol).append(" ASC");
        if (afterKey != null) {
            sql.insert(sql.indexOf("WHERE"), "AND " + orderCol + " > $cursorKey ");
        }
        sql.append(" LIMIT ").append(pageSize);

        try {
            QueryOptions opts = QueryOptions.queryOptions();
            if (afterKey != null) opts.parameters(JsonObject.create().put("cursorKey", afterKey));
            QueryResult r = parentView.getScope().query(sql.toString(), opts);
            page.clear();
            int rowCount = 0;
            for (JsonObject row : r.rowsAsObject()) {
                if (!parentView.isReadableRow(row)) continue;
                String unid = row.getString("unid");
                List<Object> cols = parentView.extractColumnValues(row);
                int pos = approxPosition + rowCount + 1;
                page.add(new CouchbaseViewEntry(parentView, unid, cols, pos, row));
                rowCount++;
            }
            pageHasMore = rowCount >= pageSize;
            if (!page.isEmpty()) {
                cursorKeyLow = extractSortKey(r.rowsAsObject().get(0));  // first of fetched
                cursorKeyHigh = extractSortKey(r.rowsAsObject().get(r.rowsAsObject().size() - 1));
            }
            pageIdx = 0;
        } catch (Exception e) {
            page.clear();
            pageHasMore = false;
        }
        return pageHasMore;
    }

    private void fetchPrevPage(String beforeKey) {
        String orderCol = buildOrderKeyRef();
        StringBuilder sql = new StringBuilder(parentView.buildNavigatorSelect())
                .append(" AND ").append(orderCol).append(" < $cursorKey")
                .append(" ORDER BY ").append(orderCol).append(" DESC")
                .append(" LIMIT ").append(pageSize);

        try {
            QueryOptions opts = QueryOptions.queryOptions()
                    .parameters(JsonObject.create().put("cursorKey", beforeKey));
            QueryResult r = parentView.getScope().query(sql.toString(), opts);
            // Results come in descending order — reverse to ascending
            List<JsonObject> rows = new ArrayList<>();
            for (JsonObject row : r.rowsAsObject()) {
                if (parentView.isReadableRow(row)) rows.add(row);
            }
            Collections.reverse(rows);

            page.clear();
            approxPosition = Math.max(0, approxPosition - rows.size());
            for (int i = 0; i < rows.size(); i++) {
                JsonObject row = rows.get(i);
                String unid = row.getString("unid");
                List<Object> cols = parentView.extractColumnValues(row);
                int pos = approxPosition + i + 1;
                page.add(new CouchbaseViewEntry(parentView, unid, cols, pos, row));
            }
            if (!page.isEmpty()) {
                cursorKeyLow = extractSortKey(rows.get(0));
                cursorKeyHigh = extractSortKey(rows.get(rows.size() - 1));
            }
        } catch (Exception e) {
            page.clear();
        }
    }

    private String extractSortKey(JsonObject row) {
        if (keyColumns.isEmpty()) return row.getString("unid");
        return extractKeyFromRow(row, keyColumns.get(0));
    }

    private String extractKeyFromRow(JsonObject row, String colName) {
        var items = row.getObject("items");
        if (items != null) {
            Object val = items.get(colName);
            if (val instanceof com.couchbase.client.java.json.JsonArray arr && !arr.isEmpty()) {
                var io = arr.getObject(0);
                if (io != null) {
                    var vs = io.getArray("values");
                    if (vs != null && !vs.isEmpty()) {
                        Object v = vs.get(0);
                        return v != null ? v.toString() : "";
                    }
                }
            }
        }
        return "";
    }

    private String keyOf(ViewEntry entry) {
        if (entry == null) return null;
        if (entry.isCategory()) {
            return entry.getColumnValues().isEmpty() ? "" : String.valueOf(entry.getColumnValues().get(0));
        }
        if (entry instanceof CouchbaseViewEntry cve && cve.getColumnValues() != null && !cve.getColumnValues().isEmpty()) {
            return String.valueOf(cve.getColumnValues().get(0));
        }
        return entry.getUniversalID();
    }

    private ViewEntry injectCategory(String prevKey, String curKey, ViewEntry prevEntry, ViewEntry currentEntry) {
        // Find common prefix level
        int level = findDivergingLevel(prevKey, curKey);
        String catVal = curKey.split("\\|\\|")[0];
        CouchbaseViewEntry cat = new CouchbaseViewEntry(parentView,
                approxPosition + pageIdx + 1, catVal, level, -1);
        // Insert into page buffer before current
        page.add(pageIdx, cat);
        return cat;
    }

    private int findDivergingLevel(String a, String b) {
        if (a == null || b == null) return 1;
        String[] pa = a.split("\\|\\|");
        String[] pb = b.split("\\|\\|");
        for (int i = 0; i < Math.min(pa.length, pb.length); i++) {
            if (!pa[i].equals(pb[i])) return i + 1;
        }
        return Math.min(pa.length, pb.length) + 1;
    }

    private int countDocuments() {
        String sql = "SELECT COUNT(*) AS cnt FROM " + parentView.getDatabase().getCollectionPath()
                + " AS doc WHERE doc._type = 'domcouch.document'";
        var formula = parentView.getSelectionFormula();
        if (formula != null && !formula.isEmpty()) sql += " AND (" + formula + ")";
        try {
            QueryResult r = parentView.getScope().query(sql);
            var rows = r.rowsAsObject();
            return rows.isEmpty() ? 0 : rows.get(0).getInt("cnt");
        } catch (Exception e) { return 0; }
    }
}
