package com.domcouch.api;

/**
 * A file attachment embedded in a document.
 * Mirrors lotus.domino.EmbeddedObject (simplified — no RichText/MIME).
 */
public interface EmbeddedObject {

    /** @return the file name of this attachment */
    String getName();

    /** @return the content type (MIME type) */
    String getType();

    /** @return the size in bytes */
    long getFileSize();

    /** @return the binary content, or null if not loaded */
    byte[] getBytes();
}
