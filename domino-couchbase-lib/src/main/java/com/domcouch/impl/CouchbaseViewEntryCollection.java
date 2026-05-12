package com.domcouch.impl;

import com.domcouch.api.ViewEntry;
import com.domcouch.api.ViewEntryCollection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Couchbase-backed ViewEntryCollection.
 */
public class CouchbaseViewEntryCollection implements ViewEntryCollection {

    private final List<ViewEntry> entries;
    private int cursor; // points to the "next" entry for getNextEntry()

    public CouchbaseViewEntryCollection(List<ViewEntry> entries) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        this.cursor = 0;
    }

    public static CouchbaseViewEntryCollection empty() {
        return new CouchbaseViewEntryCollection(List.of());
    }

    @Override
    public ViewEntry getFirstEntry() {
        reset();
        return getNextEntry();
    }

    @Override
    public ViewEntry getNextEntry() {
        if (cursor < entries.size()) {
            return entries.get(cursor++);
        }
        return null;
    }

    @Override
    public ViewEntry getNthEntry(int n) {
        if (n < 1 || n > entries.size()) return null;
        return entries.get(n - 1);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public void reset() {
        cursor = 0;
    }

    @Override
    public Iterator<ViewEntry> iterator() {
        return entries.iterator();
    }

    @Override
    public void recycle() {
        entries.clear();
    }
}
