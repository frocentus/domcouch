package com.domcouch.api;

import java.util.Iterator;

/**
 * A collection of Document objects returned by search, FTSearch, or getAllDocuments.
 * Mirrors lotus.domino.DocumentCollection.
 */
public interface DocumentCollection extends Iterable<Document> {

    /**
     * @return the first document in the collection, or null if empty
     */
    Document getFirstDocument();

    /**
     * @return the next document in the collection, or null after the last
     */
    Document getNextDocument();

    /**
     * Get a document by its 1-based position.
     *
     * @param n 1-based position
     * @return the document at position n, or null
     */
    Document getNthDocument(int n);

    /**
     * @return number of documents in this collection
     */
    int getCount();

    /**
     * Reset the internal cursor to the beginning.
     */
    void reset();

    /**
     * @return an iterator over all documents
     */
    @Override
    Iterator<Document> iterator();

    /**
     * Merge another DocumentCollection into this one (union).
     *
     * @param other the collection to merge
     */
    void merge(DocumentCollection other);

    /**
     * Intersect this collection with another (set intersection).
     *
     * @param other the collection to intersect with
     */
    void intersect(DocumentCollection other);

    /**
     * Subtract another collection from this one (set difference).
     *
     * @param other the collection to subtract
     */
    void subtract(DocumentCollection other);

    /**
     * Stamp all documents in the collection by replacing an item value.
     *
     * @param itemName  the item name
     * @param value     the value to set
     */
    void stampAll(String itemName, Object value) throws NotesException;

    /**
     * Release resources.
     */
    void recycle();
}
