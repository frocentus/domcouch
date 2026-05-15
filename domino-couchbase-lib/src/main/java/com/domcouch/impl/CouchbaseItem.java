package com.domcouch.impl;

import com.domcouch.api.DateTime;
import com.domcouch.api.Item;

import java.util.List;
import java.util.Vector;

/**
 * Couchbase-backed Item implementation.
 * Stores values as a JSON-compatible List, supporting multi-value fields.
 */
public class CouchbaseItem implements Item {

    private final String name;
    private int type;
    private final Vector<Object> values;
    private Object customData;
    private CouchbaseDocument parent; // set after construction for getEmbeddedObjects()

    public CouchbaseItem(String name, int type, List<Object> rawValues) {
        this.name = name;
        this.type = type;
        this.values = rawValues != null ? new Vector<>(rawValues) : new Vector<>();
    }

    public CouchbaseItem(String name, int type, Object singleValue) {
        this.name = name;
        this.type = type;
        this.values = new Vector<>();
        if (singleValue != null) {
            this.values.add(singleValue);
        }
    }

    public static CouchbaseItem textItem(String name, String value) {
        return new CouchbaseItem(name, Item.TEXT, value);
    }

    public static CouchbaseItem numericItem(String name, Number value) {
        return new CouchbaseItem(name, Item.NUMBERS, value);
    }

    public static CouchbaseItem dateItem(String name, DateTime value) {
        return new CouchbaseItem(name, Item.DATETIMES, value);
    }

    public static CouchbaseItem readersItem(String name, List<String> values) {
        CouchbaseItem item = new CouchbaseItem(name, Item.READERS, (List<Object>) (List<?>) values);
        return item;
    }

    public static CouchbaseItem authorsItem(String name, List<String> values) {
        CouchbaseItem item = new CouchbaseItem(name, Item.AUTHORS, (List<Object>) (List<?>) values);
        return item;
    }

    public void addValue(Object value) {
        this.values.add(value);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public Vector<Object> getValues() {
        return values;
    }

    @Override
    public String getValueString() {
        if (values.isEmpty()) return "";
        Object val = values.get(0);
        if (val == null) return "";
        if (val instanceof DateTime dt) return dt.getLocalTime();
        return val.toString();
    }

    @Override
    public int getValueInt() {
        if (values.isEmpty()) return 0;
        Object val = values.get(0);
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public double getValueDouble() {
        if (values.isEmpty()) return 0.0;
        Object val = values.get(0);
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public DateTime getValueDateTime() {
        if (values.isEmpty()) return null;
        Object val = values.get(0);
        if (val instanceof DateTime dt) return dt;
        return null;
    }

    @Override
    public Object getValueCustomData() {
        return customData;
    }

    @Override
    public void setValueCustomData(Object data) {
        this.customData = data;
    }

    @Override
    public boolean isReaders() {
        return type == Item.READERS;
    }

    @Override
    public boolean isAuthors() {
        return type == Item.AUTHORS;
    }

    @Override
    public void setReaders(boolean flag) {
        this.type = flag ? Item.READERS : Item.TEXT;
    }

    @Override
    public void setAuthors(boolean flag) {
        this.type = flag ? Item.AUTHORS : Item.TEXT;
    }

    @Override
    public String toString() {
        return "CouchbaseItem[" + name + "=" + values + "]";
    }

    void setParent(CouchbaseDocument parent) { this.parent = parent; }

    @Override
    public java.util.List<com.domcouch.api.EmbeddedObject> getEmbeddedObjects() {
        if (parent == null) return java.util.List.of();
        return parent.getEmbeddedObjects().stream()
                .filter(eo -> name.equals(eo.getItemName()))
                .toList();
    }
}
