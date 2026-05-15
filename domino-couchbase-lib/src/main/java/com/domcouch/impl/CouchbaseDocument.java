package com.domcouch.impl;

import com.couchbase.client.java.json.JsonObject;
import com.domcouch.api.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Couchbase-backed Document implementation.
 *
 * The Couchbase JSON schema for a document:
 * <pre>
 * {
 *   "_type": "domcouch.document",
 *   "unid": "...",
 *   "form": "Person",
 *   "items": {
 *     "FieldName": { "type": 0, "values": [...] }
 *   },
 *   "created": "2024-01-01T00:00:00Z",
 *   "lastModified": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 */
public class CouchbaseDocument implements Document {

    private final CouchbaseDatabase database;
    private final Map<String, CouchbaseItem> items;
    private String unid;
    private String form;
    private boolean dirty;
    private Instant created;
    private Instant lastModified;
    private boolean isNew;
    private String parentUNID;
    private final List<String> folders;

    /** Construct a brand-new document with a generated UNID. */
    public CouchbaseDocument(CouchbaseDatabase database) {
        this.database = database;
        this.items = new ConcurrentHashMap<>();
        this.folders = new ArrayList<>();
        this.unid = generateUNID();
        this.isNew = true;
        this.dirty = true;
        this.created = Instant.now();
        this.lastModified = this.created;
    }

    /** Load an existing document from a Couchbase JsonObject. */
    public CouchbaseDocument(CouchbaseDatabase database, JsonObject doc) {
        this.database = database;
        this.items = new ConcurrentHashMap<>();
        this.folders = new ArrayList<>();
        this.isNew = false;
        this.dirty = false;
        loadFromJson(doc);
    }

    // ---- public API ----

    @Override
    public Item getFirstItem(String name) {
        return items.get(name.toUpperCase());
    }

    @Override
    public Vector<Item> getItems() {
        return new Vector<>(items.values());
    }

    @Override
    public boolean hasItem(String name) {
        return items.containsKey(name.toUpperCase());
    }

    @Override
    public Item replaceItemValue(String name, Object value) {
        String normalizedName = name.toUpperCase();
        CouchbaseItem item;
        if (value instanceof Vector<?> v) {
            item = new CouchbaseItem(normalizedName, inferType(v), new ArrayList<>(v));
        } else {
            item = new CouchbaseItem(normalizedName, inferType(value), value);
        }
        items.put(normalizedName, item);
        dirty = true;
        return item;
    }

    @Override
    public boolean save() throws NotesException {
        if (!dirty) return true;

        // Author-field enforcement: if author items exist, current user must be in at least one
        String currentUser = database.getCurrentUserName();
        if (!isEditableBy(currentUser)) {
            throw new NotesException(4010,
                    "User '" + currentUser + "' is not an author of document " + unid);
        }

        try {
            JsonObject json = toJson();
            database.upsertDocument(unid, json);
            dirty = false;
            isNew = false;
            lastModified = Instant.now();
            return true;
        } catch (Exception e) {
            throw new NotesException(4000, "Failed to save document " + unid, e);
        }
    }

    @Override
    public boolean remove() throws NotesException {
        // Author-field enforcement: same check as save()
        String currentUser = database.getCurrentUserName();
        if (!isEditableBy(currentUser)) {
            throw new NotesException(4010,
                    "User '" + currentUser + "' is not an author of document " + unid);
        }

        try {
            database.removeDocument(unid);
            dirty = true;
            return true;
        } catch (Exception e) {
            throw new NotesException(4001, "Failed to remove document " + unid, e);
        }
    }

    @Override
    public String getUniversalID() {
        return unid;
    }

    @Override
    public DateTime getCreated() {
        return created != null ? new CouchbaseDateTime(created) : null;
    }

    @Override
    public DateTime getLastModified() {
        return lastModified != null ? new CouchbaseDateTime(lastModified) : null;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    // ---- document hierarchy ----

    @Override
    public Document copyToDatabase(Database targetDb) throws NotesException {
        Document copy = targetDb.createDocument();
        copy.replaceItemValue("Form", form);
        for (Map.Entry<String, CouchbaseItem> entry : items.entrySet()) {
            copy.replaceItemValue(entry.getKey(), entry.getValue().getValues());
        }
        copy.save();
        return copy;
    }

    @Override
    public void makeResponse(Document parent) {
        this.parentUNID = parent.getUniversalID();
        dirty = true;
    }

    @Override
    public String getParentDocumentUNID() {
        return parentUNID != null ? parentUNID : "";
    }

    @Override
    public DocumentCollection getResponses() throws NotesException {
        return database.search("doc.parentUNID = '" + unid + "'");
    }

    @Override
    public boolean isResponse() {
        return parentUNID != null && !parentUNID.isEmpty();
    }

    // ---- folders ----

    @Override
    public void putInFolder(String folderName) {
        if (!folders.contains(folderName)) {
            folders.add(folderName);
            dirty = true;
        }
    }

    @Override
    public void removeFromFolder(String folderName) {
        if (folders.remove(folderName)) {
            dirty = true;
        }
    }

    @Override
    public List<String> getFolderNames() {
        return new ArrayList<>(folders);
    }

    @Override
    public void recycle() {
        items.clear();
        folders.clear();
    }

    /**
     * Check if the given user is allowed to READ this document based on Reader items.
     * <p>
     * Domino semantics:
     * <ul>
     *   <li>If the document has NO reader items → readable by everyone</li>
     *   <li>If the document has ONE OR MORE reader items → the user must appear in at
     *       least one of them to read</li>
     * </ul>
     *
     * @param userName the user name to check
     * @return true if the user may read this document
     */
    public boolean isReadableBy(String userName) {
        boolean hasReaderField = false;
        for (CouchbaseItem item : items.values()) {
            if (item.isReaders()) {
                hasReaderField = true;
                for (Object val : item.getValues()) {
                    if (val != null && val.toString().equals(userName)) {
                        return true;
                    }
                }
            }
        }
        // No reader fields → public document
        return !hasReaderField;
    }

    /**
     * Check if the given user is allowed to EDIT this document based on Author items.
     * <p>
     * Domino semantics:
     * <ul>
     *   <li>If the document has NO author items → editable by everyone</li>
     *   <li>If the document has ONE OR MORE author items → the user must appear in at
     *       least one of them to edit</li>
     * </ul>
     *
     * @param userName the user name to check
     * @return true if the user may edit this document
     */
    public boolean isEditableBy(String userName) {
        boolean hasAuthorField = false;
        for (CouchbaseItem item : items.values()) {
            if (item.isAuthors()) {
                hasAuthorField = true;
                for (Object val : item.getValues()) {
                    if (val != null && val.toString().equals(userName)) {
                        return true;
                    }
                }
            }
        }
        // No author fields → public document (anyone can edit)
        return !hasAuthorField;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
        dirty = true;
    }

    // ---- internal ----

    void loadFromJson(JsonObject doc) {
        this.unid = doc.getString("unid");
        this.form = doc.getString("form");
        this.parentUNID = doc.getString("parentUNID");

        // Load folders
        this.folders.clear();
        var folderArray = doc.getArray("folders");
        if (folderArray != null) {
            for (Object f : folderArray.toList()) {
                if (f != null) this.folders.add(f.toString());
            }
        }

        String createdStr = doc.getString("created");
        this.created = createdStr != null ? Instant.parse(createdStr) : Instant.now();

        String modStr = doc.getString("lastModified");
        this.lastModified = modStr != null ? Instant.parse(modStr) : this.created;

        JsonObject itemsObj = doc.getObject("items");
        if (itemsObj != null) {
            for (String name : itemsObj.getNames()) {
                JsonObject itemObj = itemsObj.getObject(name);
                if (itemObj == null) continue;
                int type = itemObj.getInt("type");
                List<Object> rawValues = itemObj.getArray("values") != null
                        ? itemObj.getArray("values").toList()
                        : List.of();
                this.items.put(name, new CouchbaseItem(name, type, rawValues));
            }
        }
    }

    JsonObject toJson() {
        JsonObject json = JsonObject.create();
        json.put("_type", "domcouch.document");
        json.put("unid", unid);

        // Derive top-level form from the Form item if not explicitly set
        String effectiveForm = form;
        if ((effectiveForm == null || effectiveForm.isEmpty()) && items.containsKey("Form")) {
            effectiveForm = items.get("Form").getValueString();
        }
        json.put("form", effectiveForm != null ? effectiveForm : "");

        JsonObject itemsJson = JsonObject.create();
        for (Map.Entry<String, CouchbaseItem> entry : items.entrySet()) {
            CouchbaseItem item = entry.getValue();
            JsonObject itemObj = JsonObject.create();
            itemObj.put("type", item.getType());
            itemObj.put("values", item.getValues());
            itemsJson.put(entry.getKey(), itemObj);
        }
        json.put("items", itemsJson);

        if (parentUNID != null && !parentUNID.isEmpty()) {
            json.put("parentUNID", parentUNID);
        }
        if (!folders.isEmpty()) {
            json.put("folders", folders);
        }

        json.put("created", created != null ? created.toString() : Instant.now().toString());
        json.put("lastModified", Instant.now().toString());
        return json;
    }

    private static String generateUNID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private static int inferType(Vector<?> v) {
        if (v.isEmpty()) return Item.TEXT;
        return inferType(v.get(0));
    }

    private static int inferType(Object val) {
        if (val == null) return Item.TEXT;
        if (val instanceof Number) return Item.NUMBERS;
        if (val instanceof Date || val instanceof DateTime || val instanceof Instant) return Item.DATETIMES;
        return Item.TEXT;
    }
}
