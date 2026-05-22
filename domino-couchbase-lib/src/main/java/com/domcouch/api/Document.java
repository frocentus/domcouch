package com.domcouch.api;

import java.util.Vector;

/**
 * A single document (record) within a Database.
 * Mirrors lotus.domino.Document.
 */
public interface Document {

    /**
     * @param name the item (field) name
     * @return the first Item with the given name, or null
     */
    Item getFirstItem(String name);

    /**
     * @return all items in this document
     */
    Vector<Item> getItems();

    /**
     * @param name the item name
     * @return true if this document has an item with the given name
     */
    boolean hasItem(String name);

    /**
     * Replace or create an item and assign a value.
     *
     * @param name  the item name
     * @param value the value (String, Number, Date, or Vector thereof)
     * @return the newly created/replaced Item
     */
    Item replaceItemValue(String name, Object value);

    /**
     * Remove an item from this document.
     * @param name the item name
     */
    void removeItem(String name);

    /**
     * Persist all changes to Couchbase.
     *
     * @return true on success
     * @throws NotesException on persistence failure
     */
    boolean save() throws NotesException;

    /**
     * Delete this document from the database.
     *
     * @return true on success
     */
    boolean remove() throws NotesException;

    /**
     * @return the 32-character universal ID (mapped to Couchbase document key)
     */
    String getUniversalID();

    /**
     * @return the creation timestamp
     */
    DateTime getCreated();

    /**
     * @return the last-modification timestamp
     */
    DateTime getLastModified();

    /**
     * @return true if this document has unsaved changes
     */
    boolean isDirty();

    // ---- document hierarchy ----

    /**
     * Copy this document to another database.
     *
     * @param targetDb the destination database
     * @return the new document in the target database
     * @throws NotesException on failure
     */
    Document copyToDatabase(Database targetDb) throws NotesException;

    /**
     * Make this document a response to a parent document.
     *
     * @param parent the parent document
     */
    void makeResponse(Document parent);

    /**
     * @return the UNID of the parent document, or "" if this is not a response
     */
    String getParentDocumentUNID();

    /**
     * @return all immediate response documents to this document
     * @throws NotesException on query failure
     */
    DocumentCollection getResponses() throws NotesException;

    /**
     * @return true if this document is a response to another document
     */
    boolean isResponse();

    // ---- folders ----

    /**
     * Add this document to a folder.
     *
     * @param folderName the folder name
     */
    void putInFolder(String folderName);

    /**
     * Remove this document from a folder.
     *
     * @param folderName the folder name
     */
    void removeFromFolder(String folderName);

    /**
     * @return all folder names this document belongs to
     */
    java.util.List<String> getFolderNames();

    /**
     * Attach a file to this document (document-level).
     * @param name file name
     * @param bytes binary content
     * @param mimeType content type (e.g., "application/pdf")
     * @return the embedded object
     */
    EmbeddedObject embedObject(String name, byte[] bytes, String mimeType) throws NotesException;

    /**
     * Attach a file to a specific item in this document.
     * @param itemName the item to associate the attachment with
     */
    EmbeddedObject embedObject(String itemName, String name, byte[] bytes, String mimeType) throws NotesException;

    /** @return all file attachments in this document (document-level + item-level) */
    java.util.List<EmbeddedObject> getEmbeddedObjects();

    /**
     * Evaluate computed fields and apply defaults according to a form definition.
     * @param form the form to compute against
     * @param computeAll if true, re-evaluate all computed fields; if false, only computed-when-composed
     * @param validateOnly if true, only validate without computing values
     */
    void computeWithForm(Form form, boolean computeAll, boolean validateOnly) throws NotesException;

    /**
     * Evaluate computed fields using the document's own Form definition.
     * The form is determined from the document's Form item value.
     * Mirrors: {@code lotus.domino.Document.computeWithForm(boolean, boolean)}
     *
     * @param computeAll if true, recompute all fields; if false, only compute fields
     *                   marked as computed. In Domino this is {@code additionalProcessing}.
     * @param validateOnly if true, only validate without storing computed values.
     */
    void computeWithForm(boolean computeAll, boolean validateOnly) throws NotesException;

    /**
     * Find an attachment by name, searching both document-level and item-level.
     * @param name the file name
     * @return the attachment, or null if not found
     */
    EmbeddedObject getAttachment(String name);

    /**
     * Release Couchbase resources held by this document.
     */
    void recycle();
}
