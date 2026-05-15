package com.domcouch.impl;

import com.couchbase.client.java.json.JsonObject;
import com.domcouch.api.Document;
import com.domcouch.api.ViewEntry;

import java.util.List;
import java.util.Vector;

/**
 * Couchbase-backed ViewEntry with category and hierarchy support.
 */
public class CouchbaseViewEntry implements ViewEntry {

    private final CouchbaseView parentView;
    private final String unid;
    private final Vector<Object> columnValues;
    private final int position;
    private final JsonObject rawDoc; // cached from view query

    // hierarchy / category fields — package-private for CouchbaseViewNavigator
    private final boolean category;
    private final int level;
    int childCount;
    int descendantCount;
    int siblingCount;
    String positionString;
    CouchbaseViewEntry parentEntry;
    CouchbaseViewEntry firstChild;
    CouchbaseViewEntry nextSibling;
    CouchbaseViewEntry prevSibling;

    /** Document entry constructor. */
    public CouchbaseViewEntry(CouchbaseView parentView, String unid,
                              List<Object> columnValues, int position, JsonObject rawDoc) {
        this.parentView = parentView;
        this.unid = unid;
        this.columnValues = columnValues != null ? new Vector<>(columnValues) : new Vector<>();
        this.position = position;
        this.rawDoc = rawDoc;
        this.category = false;
        this.level = 0;
        this.childCount = 0;
        this.descendantCount = 0;
        this.siblingCount = 0;
        this.positionString = "";
    }

    /** Category entry constructor. */
    public CouchbaseViewEntry(CouchbaseView parentView, int position,
                              String categoryValue, int level, int childCount) {
        this.parentView = parentView;
        this.unid = null;
        this.columnValues = new Vector<>(List.of(categoryValue != null ? categoryValue : ""));
        this.position = position;
        this.rawDoc = null;
        this.category = true;
        this.level = level;
        this.childCount = childCount;
        this.descendantCount = 0; // computed during index build
        this.siblingCount = 0;    // computed during index build
        this.positionString = ""; // computed during index build
    }

    @Override
    public Vector<Object> getColumnValues() {
        return columnValues;
    }

    @Override
    public Object getColumnValue(int index) {
        if (index < 0 || index >= columnValues.size()) return null;
        return columnValues.get(index);
    }

    @Override
    public Document getDocument() {
        if (isCategory()) return null;
        try {
            if (rawDoc != null) {
                return new CouchbaseDocument(parentView.getDatabase(), rawDoc);
            }
            return parentView.getDatabase().getDocumentByUNID(unid);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getUniversalID() {
        return unid;
    }

    @Override
    public boolean isValid() {
        return isCategory() || (unid != null && !unid.isEmpty());
    }

    @Override
    public int getPosition() {
        return position;
    }

    // ---- category / hierarchy support ----

    @Override
    public boolean isCategory() {
        return category;
    }

    @Override
    public boolean isDocument() {
        return !category;
    }

    @Override
    public int getCategoryLevel() {
        return level;
    }

    @Override
    public int getChildCount() {
        return childCount;
    }

    @Override
    public int getDescendantCount() {
        return descendantCount;
    }

    @Override
    public int getSiblingCount() {
        return siblingCount;
    }

    @Override
    public String getPositionString() {
        return positionString;
    }

    @Override
    public ViewEntry getParentEntry() {
        return parentEntry;
    }

    @Override
    public ViewEntry getChild() {
        return firstChild;
    }

    @Override
    public ViewEntry getNextSibling() {
        return nextSibling;
    }

    @Override
    public ViewEntry getPrevSibling() {
        return prevSibling;
    }

    boolean isCategoryRaw() {
        return category;
    }

    @Override
    public String toString() {
        if (isCategory()) {
            return "CouchbaseViewEntry[#" + position + " CATEGORY lvl=" + level
                    + " \"" + (columnValues.isEmpty() ? "" : columnValues.get(0)) + "\"]";
        }
        return "CouchbaseViewEntry[#" + position + ", " + unid + "]";
    }
}
