package com.domcouch.impl;

import com.domcouch.api.EmbeddedObject;

/**
 * Couchbase-backed EmbeddedObject — stores file metadata in document JSON,
 * binary content as Couchbase sub-document binary attachment.
 */
public class CouchbaseEmbeddedObject implements EmbeddedObject {

    private final String name;
    private final String type;
    private final long fileSize;
    private final byte[] bytes;
    private final String itemName; // null = document-level

    public CouchbaseEmbeddedObject(String name, String type, long fileSize, byte[] bytes) {
        this(name, type, fileSize, bytes, null);
    }

    public CouchbaseEmbeddedObject(String name, String type, long fileSize, byte[] bytes, String itemName) {
        this.name = name;
        this.type = type;
        this.fileSize = fileSize;
        this.bytes = bytes;
        this.itemName = itemName;
    }

    @Override public String getName() { return name; }
    @Override public String getType() { return type; }
    @Override public long getFileSize() { return fileSize; }
    @Override public byte[] getBytes() { return bytes; }
    @Override public String getItemName() { return itemName; }

    @Override
    public String toString() {
        return "CouchbaseEmbeddedObject[" + name + ", " + type + ", " + fileSize + " bytes]";
    }
}
