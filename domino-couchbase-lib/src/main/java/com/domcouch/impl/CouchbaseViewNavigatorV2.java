package com.domcouch.impl;

import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.domcouch.api.NotesException;
import com.domcouch.api.View;
import com.domcouch.api.ViewEntry;
import com.domcouch.api.ViewNavigator;

import java.util.*;

/**
 * ViewNavigator using N1QL cursor pagination and _categories LIKE queries.
 * <p>
 * Instead of building an in-memory index, each navigation call issues a N1QL
 * query against the pre-computed {@code _categories} field. Categories are
 * detected by comparing prefix changes between adjacent rows.
 * <p>
 * Trade-offs vs. in-memory CouchbaseViewNavigator:
 * <ul>
 *   <li>+ Build time: ~0ms (virtual categories created on-the-fly via LIKE)</li>
 *   <li>+ Memory: O(1) — only current page cached</li>
 *   <li>+ Scale: bounded by N1QL query performance, not memory</li>
 *   <li>− getNth(n): N1QL OFFSET query (slower than array access)</li>
 *   <li>− No hierarchy links (parent/sibling/position strings unsupported)</li>
 * </ul>
 */
public class CouchbaseViewNavigatorV2 implements ViewNavigator {

    private final CouchbaseView parentView;
    private final List<String> keyColumns;
    private final String baseSelect;  // SELECT ... FROM ... (no WHERE, ORDER BY added per-query)
    private final String whereClause; // WHERE _type = ... (and optional selection formula)
    private final String orderBy;

    private int cursorOffset;
    private int totalCount = -1;
    private ViewEntry currentEntry;
    private String prevCategories;  // _categories value of previous row

    public CouchbaseViewNavigatorV2(View parentView) {
        this(parentView, ((CouchbaseView) parentView).getCategoryColumns());
    }

    public CouchbaseViewNavigatorV2(View parentView, List<String> keyColumns) {
        this.parentView = (CouchbaseView) parentView;
        this.keyColumns = keyColumns != null ? keyColumns : List.of();
        this.cursorOffset = -1;

        // Build base SELECT (without WHERE/ORDER BY)
        StringBuilder select = new StringBuilder("unid, doc.*");
        this.baseSelect = select.toString();

        // Build WHERE clause
        String cp = this.parentView.getDatabase().getCollectionPath();
        StringBuilder where = new StringBuilder("doc._type = 'domcouch.document'");
        var formula = this.parentView.getSelectionFormula();
        if (formula != null && !formula.isEmpty()) {
            where.append(" AND (").append(formula).append(")");
        }
        this.whereClause = where.toString();

        // Build ORDER BY
        if (this.keyColumns.isEmpty()) {
            this.orderBy = "";
        } else {
            StringBuilder ob = new StringBuilder("doc.items._CATEGORIES[0].`values`[0]");
            this.orderBy = ob.toString();
        }
    }

    private Scope scope() { return parentView.getScope(); }

    private String collectionPath() { return parentView.getDatabase().getCollectionPath(); }

    private String fullQuery(String extraWhere, String extraOrder, String limitOffset) {
        StringBuilder sb = new StringBuilder("SELECT ").append(baseSelect)
                .append(" FROM ").append(collectionPath()).append(" AS doc")
                .append(" WHERE ").append(whereClause);
        if (extraWhere != null && !extraWhere.isEmpty()) {
            sb.append(" AND ").append(extraWhere);
        }
        if (orderBy != null && !orderBy.isEmpty()) {
            sb.append(" ORDER BY ").append(orderBy);
        }
        if (extraOrder != null && !extraOrder.isEmpty()) {
            if (orderBy != null && !orderBy.isEmpty()) sb.append(", ");
            else sb.append(" ORDER BY ");
            sb.append(extraOrder);
        }
        if (limitOffset != null) sb.append(" ").append(limitOffset);
        return sb.toString();
    }

    // ---- count / properties ----

    @Override
    public int getCount() {
        if (totalCount < 0) {
            String q = "SELECT COUNT(*) AS cnt FROM " + collectionPath()
                    + " AS doc WHERE " + whereClause;
            try {
                QueryResult r = scope().query(q);
                var rows = r.rowsAsObject();
                totalCount = rows.isEmpty() ? 0 : rows.get(0).getInt("cnt");
            } catch (Exception e) { totalCount = 0; }
        }
        return totalCount;
    }

    @Override
    public View getParentView() { return parentView; }

    @Override
    public int getMaxLevel() { return keyColumns.size(); }

    // ---- get methods ----

    @Override
    public ViewEntry getFirst() throws NotesException {
        cursorOffset = 0;
        prevCategories = null;
        return fetchAt(0);
    }

    @Override
    public ViewEntry getLast() throws NotesException {
        return fetchAt(getCount() - 1);
    }

    @Override
    public ViewEntry getNext() throws NotesException {
        if (cursorOffset < 0) return getFirst();
        cursorOffset++;
        // Track previous _categories for category detection
        String prevCat = prevCategories;
        ViewEntry e = fetchAt(cursorOffset);
        if (e != null && !e.isCategory() && prevCat != null) {
            String currentCat = extractCategories(e);
            if (!Objects.equals(prevCat, currentCat)) {
                // Category boundary — inject a virtual category entry
                String catPrefix = commonPrefix(prevCat, currentCat);
                CouchbaseViewEntry catEntry = new CouchbaseViewEntry(
                        parentView, cursorOffset + 1, catPrefix, findCategoryLevel(prevCat, currentCat), -1);
                currentEntry = catEntry;
                prevCategories = prevCat; // don't advance prevCategories past category
                return catEntry;
            }
        }
        if (e != null) prevCategories = extractCategories(e);
        return e;
    }

    @Override
    public ViewEntry getPrev() throws NotesException {
        if (cursorOffset <= 0) return null;
        cursorOffset--;
        // For prev, category detection is harder — just return raw entry
        ViewEntry e = fetchAt(cursorOffset);
        if (e != null) prevCategories = extractCategories(e);
        return e;
    }

    @Override
    public ViewEntry getNext(ViewEntry entry) throws NotesException {
        if (entry == null) return null;
        cursorOffset = entry.getPosition();
        return getNext();
    }

    @Override
    public ViewEntry getPrev(ViewEntry entry) throws NotesException {
        if (entry == null) return null;
        cursorOffset = entry.getPosition() - 2;
        return getPrev();
    }

    @Override
    public ViewEntry getCurrent() throws NotesException { return currentEntry; }

    @Override
    public ViewEntry getNth(int n) throws NotesException {
        cursorOffset = n - 1;
        return fetchAt(cursorOffset);
    }

    @Override
    public ViewEntry getPos(String pos) throws NotesException {
        throw new NotesException(4500, "getPos not supported in cursor navigator");
    }

    @Override
    public ViewEntry getFirstDocument() throws NotesException {
        cursorOffset = 0;
        ViewEntry e;
        while ((e = fetchAt(cursorOffset)) != null && e.isCategory()) cursorOffset++;
        return e;
    }

    @Override
    public ViewEntry getLastDocument() throws NotesException {
        int total = getCount();
        for (int i = total - 1; i >= 0; i--) {
            ViewEntry e = fetchAt(i);
            if (e != null && !e.isCategory()) return e;
        }
        return null;
    }

    @Override
    public ViewEntry getNextDocument() throws NotesException {
        while (cursorOffset + 1 < getCount()) {
            cursorOffset++;
            ViewEntry e = fetchAt(cursorOffset);
            if (e != null && !e.isCategory()) return e;
        }
        return null;
    }

    @Override
    public ViewEntry getPrevDocument() throws NotesException {
        while (cursorOffset > 0) {
            cursorOffset--;
            ViewEntry e = fetchAt(cursorOffset);
            if (e != null && !e.isCategory()) return e;
        }
        return null;
    }

    @Override
    public ViewEntry getNextCategory() throws NotesException {
        // Use _categories LIKE to find next document that starts a new category prefix
        if (prevCategories == null) {
            ViewEntry first = getFirst();
            if (first != null) {
                // Walk until we find a category boundary
                while (cursorOffset + 1 < getCount()) {
                    cursorOffset++;
                    ViewEntry e = fetchAt(cursorOffset);
                    String cat = extractCategories(e);
                    if (cat != null && !cat.equals(prevCategories) && !cat.startsWith(prevCategories + "||")) {
                        // Different top-level category
                        String catVal = cat.split("\\|\\|")[0];
                        CouchbaseViewEntry catEntry = new CouchbaseViewEntry(
                                parentView, cursorOffset + 1, catVal, 1, -1);
                        currentEntry = catEntry;
                        prevCategories = cat;
                        return catEntry;
                    }
                    prevCategories = cat;
                }
            }
        }
        return null;
    }

    @Override
    public ViewEntry getPrevCategory() throws NotesException {
        throw new NotesException(4500, "getPrevCategory not supported in cursor navigator");
    }

    @Override
    public ViewEntry getChild() throws NotesException {
        // First child = next document entry after current category
        if (currentEntry == null || !currentEntry.isCategory()) return null;
        cursorOffset++;
        ViewEntry e = fetchAt(cursorOffset);
        while (e != null && e.isCategory()) {
            cursorOffset++;
            e = fetchAt(cursorOffset);
        }
        return e;
    }

    @Override
    public ViewEntry getChild(ViewEntry entry) throws NotesException {
        gotoEntry(entry);
        return getChild();
    }

    // Hierarchy methods not supported in cursor mode
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
        cursorOffset = entry.getPosition() - 1;
    }
    @Override public void gotoPos(String pos) throws NotesException {
        throw new NotesException(4500, "gotoPos not supported");
    }
    @Override public void gotoNth(int n) throws NotesException { getNth(n); }
    @Override public void gotoFirstDocument() throws NotesException { getFirstDocument(); }
    @Override public void gotoLastDocument() throws NotesException { getLastDocument(); }
    @Override public void gotoNextDocument() throws NotesException { getNextDocument(); }
    @Override public void gotoPrevDocument() throws NotesException { getPrevDocument(); }
    @Override public void gotoNextCategory() throws NotesException { getNextCategory(); }
    @Override public void gotoPrevCategory() throws NotesException { getPrevCategory(); }
    @Override public void gotoParent() throws NotesException {}
    @Override public void gotoParent(ViewEntry e) throws NotesException {}
    @Override public void gotoChild() throws NotesException { getChild(); }
    @Override public void gotoChild(ViewEntry e) throws NotesException { getChild(e); }
    @Override public void gotoNextSibling() throws NotesException {}
    @Override public void gotoNextSibling(ViewEntry e) throws NotesException {}
    @Override public void gotoPrevSibling() throws NotesException {}
    @Override public void gotoPrevSibling(ViewEntry e) throws NotesException {}

    @Override public void markAllRead() {}
    @Override public void markAllRead(String u) {}
    @Override public void markAllUnread() {}
    @Override public void markAllUnread(String u) {}
    @Override public void recycle() { currentEntry = null; }

    // ---- internal ----

    private ViewEntry fetchAt(int offset) {
        if (offset < 0) return null;
        String q = fullQuery(null, orderBy.isEmpty() ? null : null,
                "LIMIT 1 OFFSET " + offset);
        try {
            QueryResult r = scope().query(q);
            var rows = r.rowsAsObject();
            if (rows.isEmpty()) return null;
            JsonObject row = rows.get(0);
            String unid = row.getString("unid");
            List<Object> cols = parentView.extractColumnValues(row);
            CouchbaseViewEntry entry = new CouchbaseViewEntry(
                    parentView, unid, cols, offset + 1, row);
            currentEntry = entry;
            return entry;
        } catch (Exception e) { return null; }
    }

    private String extractCategories(ViewEntry entry) {
        if (entry == null || entry.isCategory()) return null;
        // Try from column values or from the raw JSON
        Object cat = entry.getColumnValues().isEmpty() ? null : entry.getColumnValues().get(0);
        if (cat == null && entry instanceof CouchbaseViewEntry cve) {
            // Could extract from rawDoc but not exposed publicly — use column extraction instead
        }
        return cat != null ? cat.toString() : null;
    }

    private String commonPrefix(String a, String b) {
        if (a == null || b == null) return "";
        String[] pa = a.split("\\|\\|");
        String[] pb = b.split("\\|\\|");
        StringBuilder prefix = new StringBuilder();
        int minLen = Math.min(pa.length, pb.length);
        for (int i = 0; i < minLen; i++) {
            if (!pa[i].equals(pb[i])) break;
            if (i > 0) prefix.append("||");
            prefix.append(pa[i]);
        }
        return prefix.toString();
    }

    private int findCategoryLevel(String prevCat, String currentCat) {
        if (prevCat == null || currentCat == null) return 1;
        String[] pa = prevCat.split("\\|\\|");
        String[] pb = currentCat.split("\\|\\|");
        int same = 0;
        for (int i = 0; i < pa.length && i < pb.length; i++) {
            if (!pa[i].equals(pb[i])) break;
            same++;
        }
        return same + 1; // next level after common prefix
    }
}
