package com.domcouch.api;

import java.util.Vector;

/**
 * Represents a single item (field) on a Document.
 * Mirrors lotus.domino.Item.
 */
public interface Item {

    int TEXT      = 0;
    int NUMBERS   = 1;
    int DATETIMES = 2;
    int AUTHORS   = 3;
    int READERS   = 4;
    int RICHTEXT  = 5;

    /**
     * @return the programmatic name of this item
     */
    String getName();

    /**
     * @return the type constant for this item
     */
    int getType();

    /**
     * @return all values of this item as a Vector (multi-value aware)
     */
    Vector<Object> getValues();

    /**
     * @return the first value as a String, or "" if empty
     */
    String getValueString();

    /**
     * @return the first value as an int, or 0 if non-numeric
     */
    int getValueInt();

    /**
     * @return the first value as a double, or 0.0 if non-numeric
     */
    double getValueDouble();

    /**
     * @return the first value as a DateTime, or null
     */
    DateTime getValueDateTime();

    /**
     * @return a custom object attached to this item, or null
     */
    Object getValueCustomData();

    /**
     * Attach a custom Java object to this item.
     *
     * @param data the object to attach
     */
    void setValueCustomData(Object data);

    /**
     * @return true if this item is a Readers-type item
     */
    boolean isReaders();

    /**
     * @return true if this item is an Authors-type item
     */
    boolean isAuthors();

    /**
     * Mark/unmark this item as a Readers-type item.
     * In Domino, Reader items control who can read the document.
     *
     * @param flag true to mark as Readers, false to unmark
     */
    void setReaders(boolean flag);

    /**
     * Mark/unmark this item as an Authors-type item.
     * In Domino, Author items control who can edit the document.
     *
     * @param flag true to mark as Authors, false to unmark
     */
    void setAuthors(boolean flag);

    /** @return file attachments embedded in this item */
    java.util.List<EmbeddedObject> getEmbeddedObjects();

    /**
     * Remove this item from its parent document.
     */
    void remove();

    /**
     * Copy this item to another document.
     * @param target the target document
     * @return the new item in the target document
     */
    Item copyItemToDocument(Document target);
}
