package com.domcouch.impl;

import com.domcouch.api.ViewEntry;
import com.domcouch.api.ViewEntryCollection;

import java.util.*;

/**
 * Couchbase-backed ViewEntryCollection with forward/backward cursor.
 */
public class CouchbaseViewEntryCollection implements ViewEntryCollection {

    private final List<ViewEntry> entries;
    private int cursor; // points to the "next" entry for getNextEntry(), -1 = before first

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
    public ViewEntry getLastEntry() {
        if (entries.isEmpty()) return null;
        cursor = entries.size() - 1;
        return entries.get(cursor);
    }

    @Override
    public ViewEntry getNextEntry() {
        if (cursor < entries.size()) {
            return entries.get(cursor++);
        }
        return null;
    }

    @Override
    public ViewEntry getPrevEntry() {
        if (cursor > 0) {
            // cursor points to "next", so prev is cursor-1 (the just-returned entry)
            // then move cursor back by 2 to point before the prev entry
            cursor -= 2;
            return entries.get(cursor);
        }
        return null;
    }

    @Override
    public ViewEntry getNthEntry(int n) {
        if (n < 1 || n > entries.size()) return null;
        cursor = n;
        return entries.get(n - 1);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public ViewEntry getEntry(Object entry) {
        if (entry == null) return null;
        // Lookup by ViewEntry object reference
        if (entry instanceof ViewEntry ve) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i) == ve) return entries.get(i);
            }
            // Try by UNID
            String unid = ve.getUniversalID();
            if (unid != null) {
                for (ViewEntry e : entries) {
                    if (unid.equals(e.getUniversalID())) {
                        cursor = e.getPosition();
                        return e;
                    }
                }
            }
        }
        // Lookup by key string
        String key = entry.toString();
        for (int i = 0; i < entries.size(); i++) {
            ViewEntry e = entries.get(i);
            if (!e.getColumnValues().isEmpty()
                    && key.equals(String.valueOf(e.getColumnValues().get(0)))) {
                cursor = i + 1;
                return e;
            }
        }
        return null;
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
