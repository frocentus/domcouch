package com.domcouch.impl;

import com.domcouch.api.Document;
import com.domcouch.api.DocumentCollection;
import com.domcouch.api.NotesException;

import java.util.*;

/**
 * Couchbase-backed DocumentCollection.
 */
public class CouchbaseDocumentCollection implements DocumentCollection {

    private final List<Document> documents;
    private int cursor;

    public CouchbaseDocumentCollection(List<Document> documents) {
        this.documents = documents != null ? new ArrayList<>(documents) : new ArrayList<>();
        this.cursor = 0;
    }

    public static CouchbaseDocumentCollection empty() {
        return new CouchbaseDocumentCollection(List.of());
    }

    @Override
    public Document getFirstDocument() {
        reset();
        return getNextDocument();
    }

    @Override
    public Document getNextDocument() {
        if (cursor < documents.size()) {
            return documents.get(cursor++);
        }
        return null;
    }

    @Override
    public Document getNthDocument(int n) {
        if (n < 1 || n > documents.size()) return null;
        return documents.get(n - 1);
    }

    @Override
    public int getCount() {
        return documents.size();
    }

    @Override
    public void reset() {
        cursor = 0;
    }

    @Override
    public Iterator<Document> iterator() {
        return documents.iterator();
    }

    @Override
    public void merge(DocumentCollection other) {
        if (other == null) return;
        Set<String> existingUnids = new HashSet<>();
        for (Document d : documents) {
            existingUnids.add(d.getUniversalID());
        }
        for (Document d : other) {
            if (existingUnids.add(d.getUniversalID())) {
                documents.add(d);
            }
        }
    }

    @Override
    public void intersect(DocumentCollection other) {
        if (other == null) { documents.clear(); return; }
        Set<String> otherUnids = new HashSet<>();
        for (Document d : other) {
            otherUnids.add(d.getUniversalID());
        }
        documents.removeIf(d -> !otherUnids.contains(d.getUniversalID()));
    }

    @Override
    public void subtract(DocumentCollection other) {
        if (other == null) return;
        Set<String> removeUnids = new HashSet<>();
        for (Document d : other) {
            removeUnids.add(d.getUniversalID());
        }
        documents.removeIf(d -> removeUnids.contains(d.getUniversalID()));
    }

    @Override
    public void stampAll(String itemName, Object value) throws NotesException {
        for (Document doc : documents) {
            doc.replaceItemValue(itemName, value);
            doc.save();
        }
    }

    @Override
    public void recycle() {
        documents.clear();
    }

    @Override
    public String toString() {
        return "DocumentCollection[" + documents.size() + " docs]";
    }
}
