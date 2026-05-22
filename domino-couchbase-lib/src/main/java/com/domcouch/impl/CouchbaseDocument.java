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
 * Couchbase-backed implementation of {@link Document}.
 *
 * <p>Documents are stored as JSON in Couchbase with the following schema:
 * <pre>
 * {
 *   "_type": "domcouch.document",
 *   "unid": "32-char hex",
 *   "form": "Person",
 *   "items": { "FIELD": [{ "type": 0, "values": ["val"] }] },
 *   "folders": ["Inbox"],
 *   "parentUNID": "...",
 *   "_attachments": [...],
 *   "created": "ISO-8601",
 *   "lastModified": "ISO-8601"
 * }
 * </pre>
 *
 * <p>Items are stored as JSON arrays per name, supporting Domino multi-instance
 * items (multiple items with the same name). Item loading is lazy — the full
 * {@link CouchbaseItem} objects are only deserialized on first access.
 *
 * <p>Thread-safe: {@link ConcurrentHashMap} for the items map.
 */
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
    private final Map<String, List<CouchbaseItem>> items;
    private volatile JsonObject rawDoc;
    private volatile boolean itemsLoaded;
    private String unid;
    private String form;
    private boolean dirty;
    private Instant created;
    private Instant lastModified;
    private boolean isNew;
    private String parentUNID;
    private final List<String> folders;
    private final List<CouchbaseEmbeddedObject> attachments;

    /** Construct a brand-new document with a generated UNID. */
    public CouchbaseDocument(CouchbaseDatabase database) {
        this.database = database;
        this.items = new ConcurrentHashMap<>();
        this.folders = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.unid = generateUNID();
        this.isNew = true;
        this.dirty = true;
        this.created = Instant.now();
        this.lastModified = this.created;
    }

    /** Load an existing document from a Couchbase JsonObject (lazy items). */
    public CouchbaseDocument(CouchbaseDatabase database, JsonObject doc) {
        this.database = database;
        this.items = new ConcurrentHashMap<>();
        this.folders = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.isNew = false;
        this.dirty = false;
        this.rawDoc = doc;
        this.itemsLoaded = false;
        loadMetadata(doc); // loads everything except items
    }

    private synchronized void ensureItemsLoaded() {
        if (!itemsLoaded && rawDoc != null) {
            loadItems(rawDoc);
            rawDoc = null;
            itemsLoaded = true;
        }
    }

    // ---- public API ----

    /**
     * Returns the first item with the given name, or {@code null}.
     * Item names are case-insensitive (stored uppercased).
     * Triggers lazy item loading if items haven't been accessed yet.
     *
     * @param name item name (case-insensitive)
     * @return the first matching item, or null
     */
    @Override
    public Item getFirstItem(String name) {
        ensureItemsLoaded();
        var list = items.get(name.toUpperCase());
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     * Returns all items in this document. Multi-instance items (multiple
     * items with the same name) each appear as separate entries.
     * Triggers lazy item loading on first access.
     *
     * @return vector of all items
     */
    @Override
    public Vector<Item> getItems() {
        ensureItemsLoaded();
        var all = new Vector<Item>();
        for (var list : items.values()) all.addAll(list);
        return all;
    }

    /**
     * Checks whether this document has at least one item with the given name.
     *
     * @param name item name (case-insensitive)
     * @return true if an item with this name exists
     */
    @Override
    public boolean hasItem(String name) {
        ensureItemsLoaded();
        var list = items.get(name.toUpperCase());
        return list != null && !list.isEmpty();
    }

    /**
     * Replaces ALL items with the given name with a single new item.
     * Accepts a single value (String, Number, etc.) or a {@code Vector}
     * for multi-value items. Item type is inferred from the value class.
     *
     * @param name  item name (case-insensitive, stored uppercased)
     * @param value single value or Vector of values
     * @return the created item
     */
    @Override
    public Item replaceItemValue(String name, Object value) {
        // If the caller passes an existing CouchbaseItem, use it directly
        if (value instanceof CouchbaseItem existing && name.toUpperCase().equals(existing.getName())) {
            existing.setParent(this);
            items.put(existing.getName(), List.of(existing));
            dirty = true;
            return existing;
        }
        String normalizedName = name.toUpperCase();
        CouchbaseItem item;
        if (value instanceof Vector<?> v) {
            item = new CouchbaseItem(normalizedName, inferType(v), new ArrayList<>(v));
        } else {
            item = new CouchbaseItem(normalizedName, inferType(value), value);
        }
        item.setParent(this);
        items.put(normalizedName, List.of(item));
        dirty = true;
        return item;
    }

    /** Removes all items with the given name. */
    @Override
    public void removeItem(String name) {
        items.remove(name.toUpperCase());
        dirty = true;
    }

    /**
     * Persists this document to Couchbase via KV upsert.
     * Enforces Author-field access control: if the document has Author items,
     * the current session user must be listed in at least one of them.
     *
     * @return true on success
     * @throws NotesException if author check fails (4010) or save fails (4000)
     */
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

    /**
     * Permanently deletes this document from Couchbase.
     * Enforces the same Author-field check as {@link #save()}.
     *
     * @return true on success
     * @throws NotesException if author check fails (4010) or remove fails (4001)
     */
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
            isNew = false;
            return true;
        } catch (Exception e) {
            throw new NotesException(4001, "Failed to remove document " + unid, e);
        }
    }

    /** @return 32-character uppercase hex universal ID */
    @Override
    public String getUniversalID() {
        return unid;
    }

    /** @return creation timestamp, or null for unsaved documents */
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
        ensureItemsLoaded();
        Document copy = targetDb.createDocument();
        copy.replaceItemValue("Form", form);
        for (var entry : items.entrySet()) {
            for (var item : entry.getValue()) {
                copy.replaceItemValue(entry.getKey(), item.getValues());
            }
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
    public void computeWithForm(boolean computeAll, boolean validateOnly) throws NotesException {
        ensureItemsLoaded();
        Item formItem = getFirstItem("Form");
        if (formItem == null) {
            throw new NotesException(4000, "Document has no Form item — cannot resolve form definition");
        }
        String formName = formItem.getValueString();
        Form form = database.getForm(formName);
        if (form == null) {
            throw new NotesException(4000, "Form not found: " + formName);
        }
        computeWithForm(form, computeAll, validateOnly);
    }

    @Override
    public void computeWithForm(Form form, boolean computeAll, boolean validateOnly) throws NotesException {
        ensureItemsLoaded();
        var ft = new com.domcouch.formula.translate.FormulaTranslator();
        DocumentFormulaContext ctx = new DocumentFormulaContext(this)
                .withDatabase(database);
        for (Form.FieldDefinition fd : form.getFields()) {
            // Skip display-only fields
            if (fd.isComputedForDisplay()) continue;
            // Skip computed-when-composed for existing docs (not new)
            if (fd.isComputedWhenComposed() && !isNew) continue;

            String formula = fd.isComputed() ? fd.getFormula() : null;
            // Also check default formula if field is empty and no computed formula
            if (formula == null && fd.getDefaultFormula() != null) {
                Item existing = getFirstItem(fd.getName());
                if (existing == null || existing.getValueString().isEmpty()) {
                    formula = fd.getDefaultFormula();
                }
            }
            if (formula == null) continue;

            // Validation formula
            if (fd.getValidationFormula() != null) {
                try {
                    Object result = ft.evaluate(fd.getValidationFormula(), ctx);
                    if (result instanceof Number n && n.doubleValue() == 0.0) {
                        throw new NotesException(4000,
                                fd.getValidationMessage() != null ? fd.getValidationMessage()
                                        : "Validation failed for field " + fd.getName());
                    }
                } catch (NotesException e) { throw e; } catch (Exception ignored) {}
            }
            if (validateOnly) continue;

            // Compute and set the value
            try {
                Object result = ft.evaluate(formula, ctx);
                if (result != null && !result.equals(com.domcouch.formula.Evaluator.ERROR_VALUE)) {
                    // Unwrap single-element lists (common for @DbLookup)
                    if (result instanceof java.util.List<?> list && list.size() == 1) {
                        result = list.get(0);
                    }
                    replaceItemValue(fd.getName(), result);
                }
            } catch (Exception e) {
                // Formula evaluation failed — leave field as-is
            }
        }
    }

    @Override
    public String getParentDocumentUNID() {
        return parentUNID != null ? parentUNID : "";
    }

    @Override
    public DocumentCollection getResponses() throws NotesException {
        return database.findByParentUNID(unid);
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

    // ---- Attachments ----

    @Override
    public EmbeddedObject embedObject(String name, byte[] bytes, String mimeType) {
        return embedObject(null, name, bytes, mimeType);
    }

    @Override
    public EmbeddedObject embedObject(String itemName, String name, byte[] bytes, String mimeType) {
        var eo = new CouchbaseEmbeddedObject(name, mimeType != null ? mimeType : "application/octet-stream",
                bytes != null ? bytes.length : 0, bytes, itemName);
        attachments.add(eo);
        // Document-level attachments: map to $FILE items for Domino API compatibility
        if (itemName == null) {
            var fileList = items.get("$FILE");
            if (fileList != null && !fileList.isEmpty()) {
                var vals = new java.util.ArrayList<>(fileList.get(0).getValues());
                vals.add(name);
                replaceItemValue("$FILE", new Vector<>(vals));
            } else {
                replaceItemValue("$FILE", name);
            }
        }
        dirty = true;
        return eo;
    }

    @Override
    public List<EmbeddedObject> getEmbeddedObjects() {
        return new ArrayList<>(attachments);
    }

    @Override
    public EmbeddedObject getAttachment(String name) {
        return attachments.stream()
                .filter(a -> name.equalsIgnoreCase(a.getName()))
                .findFirst().orElse(null);
    }

    @Override
    public void recycle() {
        items.clear();
        folders.clear();
        attachments.clear();
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
        ensureItemsLoaded();
        boolean hasReaderField = false;
        for (var itemList : items.values()) {
            for (CouchbaseItem item : itemList) {
                if (item.isReaders()) {
                    hasReaderField = true;
                    for (Object val : item.getValues()) {
                        if (val != null && val.toString().equals(userName)) {
                            return true;
                        }
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
        ensureItemsLoaded();
        boolean hasAuthorField = false;
        for (var itemList : items.values()) {
            for (CouchbaseItem item : itemList) {
                if (item.isAuthors()) {
                    hasAuthorField = true;
                    for (Object val : item.getValues()) {
                        if (val != null && val.toString().equals(userName)) {
                            return true;
                        }
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

    void loadMetadata(JsonObject doc) {
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

        // Load attachments from _attachments JSON array
        this.attachments.clear();
        var attArray = doc.getArray("_attachments");
        if (attArray != null) {
            for (Object a : attArray.toList()) {
                if (a instanceof JsonObject att) {
                    this.attachments.add(new CouchbaseEmbeddedObject(
                            att.getString("name"), att.getString("type"),
                            att.getLong("size"), null,
                            att.getString("item")));
                }
            }
        }
        // Fallback: if no _attachments, check $FILE items for document-level attachments
        if (this.attachments.isEmpty()) {
            var fileItem = doc.getObject("items");
            if (fileItem != null) {
                var fileObj = fileItem.getObject("$FILE");
                if (fileObj != null) {
                    var fileValues = fileObj.getArray("values");
                    if (fileValues != null) {
                        for (Object f : fileValues.toList()) {
                            if (f != null) {
                                this.attachments.add(new CouchbaseEmbeddedObject(
                                        f.toString(), null, 0, null, null));
                            }
                        }
                    }
                }
            }
        }

        String createdStr = doc.getString("created");
        this.created = createdStr != null ? Instant.parse(createdStr) : Instant.now();

        String modStr = doc.getString("lastModified");
        this.lastModified = modStr != null ? Instant.parse(modStr) : this.created;
    }

    private void loadItems(JsonObject doc) {
        JsonObject itemsObj = doc.getObject("items");
        if (itemsObj != null) {
            for (String name : itemsObj.getNames()) {
                var itemList = new ArrayList<CouchbaseItem>();
                Object val = itemsObj.get(name);
                if (val instanceof com.couchbase.client.java.json.JsonArray arr) {
                    for (int i = 0; i < arr.size(); i++) {
                        var io = arr.getObject(i);
                        if (io != null) itemList.add(parseItem(name, io));
                    }
                } else if (val instanceof JsonObject io) {
                    itemList.add(parseItem(name, io));
                }
                if (!itemList.isEmpty()) this.items.put(name, itemList);
            }
        }
    }

    private CouchbaseItem parseItem(String name, JsonObject io) {
        int type = io.getInt("type");
        // Rich text: reconstruct from stored segments
        if (type == Item.RICHTEXT) {
            var rtSegments = io.getArray("rtSegments");
            if (rtSegments != null) {
                var item = new CouchbaseRichTextItem(name, rtSegments);
                item.setParent(this);
                return item;
            }
        }
        List<Object> rawValues = io.getArray("values") != null
                ? io.getArray("values").toList() : List.of();
        var item = new CouchbaseItem(name, type, rawValues);
        item.setParent(this);
        return item;
    }

    JsonObject toJson() {
        ensureItemsLoaded();
        JsonObject json = JsonObject.create();
        json.put("_type", "domcouch.document");
        json.put("unid", unid);

        // Derive top-level form from the Form item if not explicitly set
        String effectiveForm = form;
        if ((effectiveForm == null || effectiveForm.isEmpty()) && items.containsKey("FORM")) {
            var formList = items.get("FORM");
            if (formList != null && !formList.isEmpty()) effectiveForm = formList.get(0).getValueString();
        }
        json.put("form", effectiveForm != null ? effectiveForm : "");

        JsonObject itemsJson = JsonObject.create();
        for (var entry : items.entrySet()) {
            var itemList = entry.getValue();
            // Store as JSON array: always wrap in array for multi-instance support
            var itemArray = com.couchbase.client.java.json.JsonArray.create();
            for (var item : itemList) {
                JsonObject itemObj = JsonObject.create();
                itemObj.put("type", item.getType());
                itemObj.put("values", item.getValues());
                // Store rich text segments if present
                if (item instanceof CouchbaseRichTextItem rt) {
                    itemObj.put("rtSegments", rt.buildSegmentArray());
                }
                itemArray.add(itemObj);
            }
            itemsJson.put(entry.getKey(), itemArray);
        }
        json.put("items", itemsJson);

        if (parentUNID != null && !parentUNID.isEmpty()) {
            json.put("parentUNID", parentUNID);
        }
        if (!folders.isEmpty()) {
            json.put("folders", folders);
        }
        if (!attachments.isEmpty()) {
            var attArray = com.couchbase.client.java.json.JsonArray.create();
            for (var att : attachments) {
                var attJson = com.couchbase.client.java.json.JsonObject.create()
                        .put("name", att.getName())
                        .put("type", att.getType())
                        .put("size", att.getFileSize());
                if (att.getItemName() != null) attJson.put("item", att.getItemName());
                attArray.add(attJson);
            }
            json.put("_attachments", attArray);
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
