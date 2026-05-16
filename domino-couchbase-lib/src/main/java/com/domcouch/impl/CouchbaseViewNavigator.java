package com.domcouch.impl;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.domcouch.api.NotesException;
import com.domcouch.api.ViewEntry;
import com.domcouch.api.ViewNavigator;
import com.domcouch.api.ViewColumn;

import java.util.*;

/**
 * Couchbase-backed ViewNavigator with category support.
 *
 * Builds an in-memory categorized index from an ORDER BY N1QL query.
 * Category rows are inserted at key-boundary changes during the index build.
 * Hierarchy links (parent, first child, next sibling) are computed during build.
 */
public class CouchbaseViewNavigator implements ViewNavigator {

    private final CouchbaseView parentView;
    private final ViewIndexService indexService;
    private final List<CouchbaseViewEntry> index;
    private final Map<String, Integer> unidToPos;
    private int cursorPos; // 0-based, -1 = before first
    private final int cacheSize;
    private final int maxLevel; // 0 = unlimited

    /**
     * Create a navigator over all entries in the view.
     */
    public CouchbaseViewNavigator(CouchbaseView parentView, int cacheSize, int maxLevel) {
        this(parentView, cacheSize, maxLevel, null, false);
        if (indexService != null) indexService.ensureIndex(parentView);
        buildCategorizedIndex();
    }

    /**
     * Create a navigator with a specific index service.
     */
    public CouchbaseViewNavigator(CouchbaseView parentView, int cacheSize, int maxLevel,
                                   ViewIndexService indexService) {
        this(parentView, cacheSize, maxLevel, indexService, false);
        if (this.indexService != null) this.indexService.ensureIndex(parentView);
        buildCategorizedIndex();
    }

    /**
     * Internal constructor that skips index building (for subset navigators loaded via loadSubset).
     */
    private CouchbaseViewNavigator(CouchbaseView parentView, int cacheSize, int maxLevel,
                                    ViewIndexService indexService, boolean skipBuild) {
        this.parentView = parentView;
        this.indexService = indexService;
        this.cacheSize = cacheSize;
        this.maxLevel = maxLevel;
        this.index = new ArrayList<>();
        this.unidToPos = new HashMap<>();
        this.cursorPos = -1;
        // buildCategorizedIndex() called by public constructors only
    }

    void loadSubset(List<CouchbaseViewEntry> entries) {
        this.index.addAll(entries);
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            if (!e.isCategoryRaw() && e.getUniversalID() != null) {
                this.unidToPos.put(e.getUniversalID(), i);
            }
        }
    }

    // ---- index building ----

    private void buildCategorizedIndex() {
        List<String> keyCols = parentView.getCategoryColumns();
        if (keyCols.isEmpty()) {
            // Flat (non-categorized) view: simple ordered query
            buildFlatIndex();
            return;
        }
        buildCategorizedIndex(keyCols);
    }

    private void buildFlatIndex() {
        String stmt = parentView.buildNavigatorSelect();
        QueryResult result = parentView.getScope().query(stmt);
        int pos = 0;
        for (JsonObject row : result.rowsAsObject()) {
            if (!parentView.isReadableRow(row)) continue;
            pos++;
            String unid = row.getString("unid");
            List<Object> cols = parentView.extractColumnValues(row);
            CouchbaseViewEntry entry = new CouchbaseViewEntry(parentView, unid, cols, pos, row);
            index.add(entry);
            if (unid != null) unidToPos.put(unid, index.size() - 1);
        }
    }

    private void buildCategorizedIndex(List<String> keyCols) {
        // Build ORDER BY clause from key columns
        StringBuilder orderBy = new StringBuilder();
        for (int i = 0; i < keyCols.size(); i++) {
            if (i > 0) orderBy.append(", ");
            orderBy.append(parentView.buildKeyColumnRef(keyCols.get(i)));
        }

        // Use navigator-specific SELECT that avoids doc.*
        String baseStmt = parentView.buildNavigatorSelect();
        // Append ORDER BY after the WHERE clause (N1QL requires WHERE before ORDER BY)
        String stmt;
        if (baseStmt.contains("WHERE")) {
            stmt = baseStmt + " ORDER BY " + orderBy;
        } else {
            stmt = baseStmt + " ORDER BY " + orderBy;
        }

        QueryResult result = parentView.getScope().query(stmt);
        List<JsonObject> rows = new ArrayList<>();
        for (JsonObject row : result.rowsAsObject()) {
            if (parentView.isReadableRow(row)) rows.add(row);
        }

        if (rows.isEmpty()) return;

        // Walk sorted rows, insert category headers at key-boundary changes.
        // Track previous key values per level to detect category changes.
        int globalPos = 0;
        List<String> prevKeys = new ArrayList<>(Collections.nCopies(keyCols.size(), null));
        // Stack of open category entries per level
        Deque<CouchbaseViewEntry> categoryStack = new ArrayDeque<>();

        for (JsonObject row : rows) {
            String unid = row.getString("unid");
            List<Object> cols = parentView.extractColumnValues(row);

            // Determine which levels have key changes
            int changeLevel = -1; // level where category change starts (0-based)
            for (int lvl = 0; lvl < keyCols.size(); lvl++) {
                if (maxLevel > 0 && lvl >= maxLevel) break;
                String currentKey = extractKeyValue(row, keyCols.get(lvl), lvl, cols);
                String prevKey = prevKeys.get(lvl);
                if (!Objects.equals(currentKey, prevKey)) {
                    if (changeLevel < 0) changeLevel = lvl;
                    prevKeys.set(lvl, currentKey);
                    // Reset deeper levels since a parent changed
                    for (int d = lvl + 1; d < keyCols.size(); d++) {
                        prevKeys.set(d, null);
                        extractKeyValue(row, keyCols.get(d), d, cols); // prime prev keys
                        // Actually, need to extract the real value for deeper levels too
                    }
                }
            }

            if (changeLevel >= 0 && maxLevel == 0 || (changeLevel >= 0 && changeLevel < maxLevel)) {
                // Close categories at and below changeLevel
                while (categoryStack.size() > changeLevel) {
                    categoryStack.pop();
                }
                // Insert new category rows for levels from changeLevel up to maxLevel
                int maxLvl = maxLevel == 0 ? keyCols.size() : Math.min(maxLevel, keyCols.size());
                for (int lvl = changeLevel; lvl < maxLvl; lvl++) {
                    String keyVal = prevKeys.get(lvl);
                    if (keyVal == null) {
                        keyVal = extractKeyValue(row, keyCols.get(lvl), lvl, cols);
                        prevKeys.set(lvl, keyVal);
                    }
                    globalPos++;
                    CouchbaseViewEntry catEntry = new CouchbaseViewEntry(
                            parentView, globalPos, keyVal, lvl + 1, 0);
                    // Set parent
                    if (!categoryStack.isEmpty()) {
                        catEntry.parentEntry = categoryStack.peek();
                    }
                    // Link siblings
                    CouchbaseViewEntry parent = categoryStack.isEmpty() ? null : categoryStack.peek();
                    if (parent != null && parent.firstChild == null) {
                        parent.firstChild = catEntry;
                    } else if (parent != null) {
                        // Find last child and set as prev sibling
                        CouchbaseViewEntry lastChild = parent.firstChild;
                        while (lastChild.nextSibling != null) lastChild = lastChild.nextSibling;
                        lastChild.nextSibling = catEntry;
                        catEntry.prevSibling = lastChild;
                    }
                    categoryStack.push(catEntry);
                    index.add(catEntry);
                }
            }

            // Add document row
            globalPos++;
            CouchbaseViewEntry docEntry = new CouchbaseViewEntry(
                    parentView, unid, cols, globalPos, row);
            // Link to parent category
            if (!categoryStack.isEmpty()) {
                CouchbaseViewEntry cat = categoryStack.peek();
                docEntry.parentEntry = cat;
                if (cat.firstChild == null) {
                    cat.firstChild = docEntry;
                } else {
                    CouchbaseViewEntry lastChild = cat.firstChild;
                    while (lastChild.nextSibling != null) lastChild = lastChild.nextSibling;
                    lastChild.nextSibling = docEntry;
                    docEntry.prevSibling = lastChild;
                }
            }
            index.add(docEntry);
            if (unid != null) unidToPos.put(unid, index.size() - 1);

            // Increment child counts up the stack
            for (var cat : categoryStack) {
                cat.childCount = cat.childCount + 1;
            }
        }

        // Compute descendant counts and position strings (post-order)
        computeDescendantCounts(0, index.size() - 1, "");
        computeSiblingCounts();
    }

    private String extractKeyValue(JsonObject row, String colName, int colIndex, List<Object> cols) {
        // Try from extracted column values first
        if (colIndex < cols.size()) {
            Object v = cols.get(colIndex);
            return v != null ? v.toString() : "";
        }
        // Try from raw JSON
        JsonObject items = row.getObject("items");
        if (items != null) {
            var arr = items.getArray(colName);
            if (arr != null && !arr.isEmpty()) {
                var itemObj = arr.getObject(0);
                if (itemObj != null) {
                    var vals = itemObj.getArray("values");
                    if (vals != null && !vals.isEmpty()) {
                        Object v = vals.get(0);
                        return v != null ? v.toString() : "";
                    }
                }
            }
        }
        return "";
    }

    /**
     * Compute descendant counts for each category entry in [start, end].
     * Returns the number of direct descendants contributed by this subtree.
     */
    private int computeDescendantCounts(int start, int end, String prefix) {
        int posInLevel = 0;
        int i = start;
        for (; i <= end; ) {
            CouchbaseViewEntry entry = index.get(i);
            if (!entry.isCategoryRaw()) {
                // Document entry
                String ps = prefix.isEmpty() ? String.valueOf(posInLevel + 1)
                        : prefix + "." + (posInLevel + 1);
                entry.positionString = ps;
                posInLevel++;
                i++;
            } else {
                // Category entry: find its range
                int catLevel = entry.getCategoryLevel();
                int catEnd = i + 1;
                while (catEnd <= end) {
                    CouchbaseViewEntry e = index.get(catEnd);
                    if (e.isCategoryRaw() && e.getCategoryLevel() <= catLevel) break;
                    catEnd++;
                }
                catEnd--; // last entry in this category's range

                String ps = prefix.isEmpty() ? String.valueOf(posInLevel + 1)
                        : prefix + "." + (posInLevel + 1);
                entry.positionString = ps;

                int descendantTotal = computeDescendantCounts(i + 1, catEnd, ps);
                entry.descendantCount = descendantTotal;
                posInLevel++;
                i = catEnd + 1;
            }
        }
        return i - start; // total entries in this range
    }

    private void computeSiblingCounts() {
        for (CouchbaseViewEntry entry : index) {
            if (!entry.isCategoryRaw() && entry.getParentEntry() == null) continue;
            // Count siblings by walking from parent's firstChild
            CouchbaseViewEntry parent = (CouchbaseViewEntry) entry.getParentEntry();
            if (parent == null) {
                // Top-level entries: count consecutive same-level siblings
                int count = 0;
                CouchbaseViewEntry sib = index.isEmpty() ? null : index.get(0);
                while (sib != null) {
                    count++;
                    sib = (CouchbaseViewEntry) sib.getNextSibling();
                }
                for (CouchbaseViewEntry e : index) {
                    if (e.getParentEntry() == null) e.siblingCount = count;
                }
            } else {
                int count = 0;
                for (CouchbaseViewEntry child = (CouchbaseViewEntry) parent.getChild();
                     child != null; child = (CouchbaseViewEntry) child.getNextSibling()) {
                    count++;
                }
                for (CouchbaseViewEntry child = (CouchbaseViewEntry) parent.getChild();
                     child != null; child = (CouchbaseViewEntry) child.getNextSibling()) {
                    child.siblingCount = count;
                }
            }
        }
    }

    // ---- count / properties ----

    @Override
    public int getCount() {
        return index.size();
    }

    @Override
    public CouchbaseView getParentView() {
        return parentView;
    }

    @Override
    public int getMaxLevel() {
        List<String> keyCols = parentView.getCategoryColumns();
        if (maxLevel > 0) return Math.min(maxLevel, keyCols.size());
        return keyCols.size();
    }

    // ---- get methods ----

    @Override
    public ViewEntry getFirst() throws NotesException {
        if (index.isEmpty()) return null;
        cursorPos = 0;
        return index.get(0);
    }

    @Override
    public ViewEntry getLast() throws NotesException {
        if (index.isEmpty()) return null;
        cursorPos = index.size() - 1;
        return index.get(cursorPos);
    }

    @Override
    public ViewEntry getNext() throws NotesException {
        return getNext(currentEntry());
    }

    @Override
    public ViewEntry getNext(ViewEntry entry) throws NotesException {
        int pos = findPos(entry);
        if (pos < 0 || pos + 1 >= index.size()) return null;
        cursorPos = pos + 1;
        return index.get(cursorPos);
    }

    @Override
    public ViewEntry getPrev() throws NotesException {
        return getPrev(currentEntry());
    }

    @Override
    public ViewEntry getPrev(ViewEntry entry) throws NotesException {
        int pos = findPos(entry);
        if (pos <= 0) return null;
        cursorPos = pos - 1;
        return index.get(cursorPos);
    }

    @Override
    public ViewEntry getCurrent() throws NotesException {
        return currentEntry();
    }

    @Override
    public ViewEntry getNth(int n) throws NotesException {
        if (n < 1 || n > index.size()) return null;
        cursorPos = n - 1;
        return index.get(cursorPos);
    }

    @Override
    public ViewEntry getPos(String pos) throws NotesException {
        for (int i = 0; i < index.size(); i++) {
            if (pos.equals(index.get(i).getPositionString())) {
                cursorPos = i;
                return index.get(i);
            }
        }
        return null;
    }

    @Override
    public ViewEntry getFirstDocument() throws NotesException {
        for (int i = 0; i < index.size(); i++) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return index.get(i);
            }
        }
        return null;
    }

    @Override
    public ViewEntry getLastDocument() throws NotesException {
        for (int i = index.size() - 1; i >= 0; i--) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return index.get(i);
            }
        }
        return null;
    }

    @Override
    public ViewEntry getNextDocument() throws NotesException {
        return getNextDocument(currentEntry());
    }

    private ViewEntry getNextDocument(ViewEntry entry) {
        int pos = findPos(entry);
        if (pos < 0) return null;
        for (int i = pos + 1; i < index.size(); i++) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return index.get(i);
            }
        }
        return null;
    }

    @Override
    public ViewEntry getPrevDocument() throws NotesException {
        return getPrevDocument(currentEntry());
    }

    private ViewEntry getPrevDocument(ViewEntry entry) {
        int pos = findPos(entry);
        if (pos < 0) return null;
        for (int i = pos - 1; i >= 0; i--) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return index.get(i);
            }
        }
        return null;
    }

    @Override
    public ViewEntry getNextCategory() throws NotesException {
        return getNextCategory(currentEntry());
    }

    private ViewEntry getNextCategory(ViewEntry entry) {
        int pos = findPos(entry);
        if (pos < 0) return null;
        for (int i = pos + 1; i < index.size(); i++) {
            if (index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return index.get(i);
            }
        }
        return null;
    }

    @Override
    public ViewEntry getPrevCategory() throws NotesException {
        return getPrevCategory(currentEntry());
    }

    private ViewEntry getPrevCategory(ViewEntry entry) {
        int pos = findPos(entry);
        if (pos < 0) return null;
        for (int i = pos - 1; i >= 0; i--) {
            if (index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return index.get(i);
            }
        }
        return null;
    }

    @Override
    public ViewEntry getChild() throws NotesException {
        return getChild(currentEntry());
    }

    @Override
    public ViewEntry getChild(ViewEntry entry) throws NotesException {
        if (entry == null) return null;
        ViewEntry child = entry.getChild();
        if (child != null) {
            cursorPos = findPos(child);
        }
        return child;
    }

    @Override
    public ViewEntry getParent() throws NotesException {
        return getParent(currentEntry());
    }

    @Override
    public ViewEntry getParent(ViewEntry entry) throws NotesException {
        if (entry == null) return null;
        ViewEntry parent = entry.getParentEntry();
        if (parent != null) {
            cursorPos = findPos(parent);
        }
        return parent;
    }

    @Override
    public ViewEntry getNextSibling() throws NotesException {
        return getNextSibling(currentEntry());
    }

    @Override
    public ViewEntry getNextSibling(ViewEntry entry) throws NotesException {
        if (entry == null) return null;
        ViewEntry sib = entry.getNextSibling();
        if (sib != null) {
            cursorPos = findPos(sib);
        }
        return sib;
    }

    @Override
    public ViewEntry getPrevSibling() throws NotesException {
        return getPrevSibling(currentEntry());
    }

    @Override
    public ViewEntry getPrevSibling(ViewEntry entry) throws NotesException {
        if (entry == null) return null;
        ViewEntry sib = entry.getPrevSibling();
        if (sib != null) {
            cursorPos = findPos(sib);
        }
        return sib;
    }

    // ---- goto methods ----

    @Override
    public void gotoFirst() throws NotesException {
        if (!index.isEmpty()) cursorPos = 0;
    }

    @Override
    public void gotoLast() throws NotesException {
        if (!index.isEmpty()) cursorPos = index.size() - 1;
    }

    @Override
    public void gotoNext() throws NotesException {
        gotoNext(currentEntry());
    }

    @Override
    public void gotoNext(ViewEntry entry) throws NotesException {
        int pos = findPos(entry);
        if (pos >= 0 && pos + 1 < index.size()) cursorPos = pos + 1;
    }

    @Override
    public void gotoPrev() throws NotesException {
        gotoPrev(currentEntry());
    }

    @Override
    public void gotoPrev(ViewEntry entry) throws NotesException {
        int pos = findPos(entry);
        if (pos > 0) cursorPos = pos - 1;
    }

    @Override
    public void gotoEntry(Object entry) throws NotesException {
        if (entry instanceof ViewEntry) {
            gotoEntry((ViewEntry) entry);
        }
    }

    @Override
    public void gotoEntry(ViewEntry entry) throws NotesException {
        int pos = findPos(entry);
        if (pos >= 0) cursorPos = pos;
    }

    @Override
    public void gotoPos(String pos) throws NotesException {
        for (int i = 0; i < index.size(); i++) {
            if (pos.equals(index.get(i).getPositionString())) {
                cursorPos = i;
                return;
            }
        }
    }

    @Override
    public void gotoNth(int n) throws NotesException {
        if (n >= 1 && n <= index.size()) cursorPos = n - 1;
    }

    @Override
    public void gotoFirstDocument() throws NotesException {
        for (int i = 0; i < index.size(); i++) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return;
            }
        }
    }

    @Override
    public void gotoLastDocument() throws NotesException {
        for (int i = index.size() - 1; i >= 0; i--) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return;
            }
        }
    }

    @Override
    public void gotoNextDocument() throws NotesException {
        int pos = Math.max(cursorPos, 0);
        for (int i = pos + 1; i < index.size(); i++) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return;
            }
        }
    }

    @Override
    public void gotoPrevDocument() throws NotesException {
        int pos = Math.min(cursorPos, index.size() - 1);
        if (pos < 0) pos = index.size();
        for (int i = pos - 1; i >= 0; i--) {
            if (!index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return;
            }
        }
    }

    @Override
    public void gotoNextCategory() throws NotesException {
        int pos = Math.max(cursorPos, 0);
        for (int i = pos + 1; i < index.size(); i++) {
            if (index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return;
            }
        }
    }

    @Override
    public void gotoPrevCategory() throws NotesException {
        int pos = Math.min(cursorPos, index.size() - 1);
        if (pos < 0) pos = index.size();
        for (int i = pos - 1; i >= 0; i--) {
            if (index.get(i).isCategoryRaw()) {
                cursorPos = i;
                return;
            }
        }
    }

    @Override
    public void gotoChild() throws NotesException {
        gotoChild(currentEntry());
    }

    @Override
    public void gotoChild(ViewEntry entry) throws NotesException {
        if (entry != null && entry.getChild() != null) {
            cursorPos = findPos(entry.getChild());
        }
    }

    @Override
    public void gotoParent() throws NotesException {
        gotoParent(currentEntry());
    }

    @Override
    public void gotoParent(ViewEntry entry) throws NotesException {
        if (entry != null && entry.getParentEntry() != null) {
            cursorPos = findPos(entry.getParentEntry());
        }
    }

    @Override
    public void gotoNextSibling() throws NotesException {
        gotoNextSibling(currentEntry());
    }

    @Override
    public void gotoNextSibling(ViewEntry entry) throws NotesException {
        if (entry != null && entry.getNextSibling() != null) {
            cursorPos = findPos(entry.getNextSibling());
        }
    }

    @Override
    public void gotoPrevSibling() throws NotesException {
        gotoPrevSibling(currentEntry());
    }

    @Override
    public void gotoPrevSibling(ViewEntry entry) throws NotesException {
        if (entry != null && entry.getPrevSibling() != null) {
            cursorPos = findPos(entry.getPrevSibling());
        }
    }

    // ---- mark methods (no-ops for Couchbase) ----

    @Override
    public void markAllRead() {}

    @Override
    public void markAllRead(String userName) {}

    @Override
    public void markAllUnread() {}

    @Override
    public void markAllUnread(String userName) {}

    @Override
    public void recycle() {
        if (indexService != null) indexService.dropIndex(parentView);
        index.clear();
        unidToPos.clear();
    }

    // ---- internal ----

    private CouchbaseViewEntry currentEntry() {
        if (cursorPos < 0 || cursorPos >= index.size()) return null;
        return index.get(cursorPos);
    }

    private int findPos(ViewEntry entry) {
        if (entry == null) return -1;
        // Try direct position
        int pos = entry.getPosition();
        if (pos > 0 && pos <= index.size() && index.get(pos - 1) == entry) {
            return pos - 1;
        }
        // Try by UNID
        if (!entry.isCategory() && entry.getUniversalID() != null) {
            Integer p = unidToPos.get(entry.getUniversalID());
            if (p != null) return p;
        }
        // Fallback: linear scan
        for (int i = 0; i < index.size(); i++) {
            if (index.get(i) == entry) return i;
        }
        return -1;
    }

    /**
     * Create a sub-navigator from a subset of this navigator's index.
     * @param startInclusive 0-based inclusive start index
     * @param endExclusive   0-based exclusive end index (matches List.subList semantics)
     */
    CouchbaseViewNavigator createSubset(int startInclusive, int endExclusive) {
        CouchbaseViewNavigator nav = new CouchbaseViewNavigator(parentView, cacheSize, maxLevel, null, false);
        nav.loadSubset(new ArrayList<>(index.subList(startInclusive, endExclusive)));
        return nav;
    }
}
