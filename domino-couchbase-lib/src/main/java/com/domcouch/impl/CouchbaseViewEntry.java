package com.domcouch.impl;

import com.couchbase.client.java.json.JsonObject;
import com.domcouch.api.Document;
import com.domcouch.api.ViewEntry;

import java.util.List;
import java.util.Vector;

/**
 * Couchbase-backed ViewEntry.
 */
public class CouchbaseViewEntry implements ViewEntry {

    private final CouchbaseView parentView;
    private final String unid;
    private final Vector<Object> columnValues;
    private final int position;
    private final JsonObject rawDoc; // cached from view query to avoid N+1 point read

    public CouchbaseViewEntry(CouchbaseView parentView, String unid,
                              List<Object> columnValues, int position, JsonObject rawDoc) {
        this.parentView = parentView;
        this.unid = unid;
        this.columnValues = columnValues != null ? new Vector<>(columnValues) : new Vector<>();
        this.position = position;
        this.rawDoc = rawDoc;
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
        try {
            // Reuse raw document from view query if available, otherwise point read
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
        return unid != null && !unid.isEmpty();
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "CouchbaseViewEntry[#" + position + ", " + unid + "]";
    }
}
