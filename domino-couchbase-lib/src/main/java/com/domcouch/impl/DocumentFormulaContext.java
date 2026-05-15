package com.domcouch.impl;

import com.domcouch.api.Document;
import com.domcouch.api.Item;
import com.domcouch.formula.FormulaContext;

/**
 * A {@link FormulaContext} backed by a Domino-style {@link Document}.
 * <p>
 * Resolves variable names by looking up items on the document via
 * {@link Document#getFirstItem(String)}. Supports {@code setField} for
 * {@code FIELD} assignments and {@code deleteField} for {@code @DeleteField}.
 * <p>
 * Database-level properties ({@code @DbName}, {@code @DbTitle},
 * {@code @ReplicaID}, {@code @ServerName}) are optional — if not set,
 * they throw {@link com.domcouch.formula.ContextNotSupportedException}
 * and the evaluator returns sensible defaults ("").
 * <p>
 * Multi-value items are returned as {@link java.util.Vector} lists;
 * single-value items return the first value directly.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   // Document-only context (read/write fields, UNID — no database info)
 *   DocumentFormulaContext ctx = new DocumentFormulaContext(document);
 *
 *   // Full context with database metadata
 *   DocumentFormulaContext ctx = new DocumentFormulaContext(document)
 *       .withDatabaseName("contacts.nsf")
 *       .withServerName("CN=Server/O=Org")
 *       .withDatabaseTitle("Personnel Records")
 *       .withReplicaID("85255B6E004A6D12");
 * }</pre>
 */
public class DocumentFormulaContext implements FormulaContext {

    private final Document document;
    private String databaseName;
    private String serverName;
    private String databaseTitle;
    private String replicaID;

    /**
     * @param document the document to resolve fields from
     */
    public DocumentFormulaContext(Document document) {
        this.document = document;
    }

    @Override
    public Object resolve(String name) {
        Item item = document.getFirstItem(name);
        if (item == null) return null;          // field does not exist
        var values = item.getValues();
        if (values == null || values.isEmpty()) return "";
        if (values.size() > 1) return values;
        return values.get(0);
    }

    @Override
    public void setField(String name, Object value) {
        document.replaceItemValue(name, value);
    }

    @Override
    public void deleteField(String name) {
        document.replaceItemValue(name, "");
    }

    @Override
    public java.util.List<String> getFieldNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        var items = document.getItems();
        for (com.domcouch.api.Item item : items) {
            names.add(item.getName());
        }
        return names;
    }

    @Override
    public String getDocumentUNID() {
        return document.getUniversalID();
    }

    /** @return the underlying document */
    public Document getDocument() {
        return document;
    }

    // ---- Builder methods for database-level context ----

    public DocumentFormulaContext withDatabaseName(String name) {
        this.databaseName = name; return this;
    }

    public DocumentFormulaContext withServerName(String name) {
        this.serverName = name; return this;
    }

    public DocumentFormulaContext withDatabaseTitle(String title) {
        this.databaseTitle = title; return this;
    }

    public DocumentFormulaContext withReplicaID(String replicaID) {
        this.replicaID = replicaID; return this;
    }

    // ---- Database-level context overrides ----

    @Override
    public String getDatabaseName() {
        if (databaseName != null) return databaseName;
        throw new com.domcouch.formula.ContextNotSupportedException("getDatabaseName");
    }

    @Override
    public String getServerName() {
        if (serverName != null) return serverName;
        throw new com.domcouch.formula.ContextNotSupportedException("getServerName");
    }

    @Override
    public String getDatabaseTitle() {
        if (databaseTitle != null) return databaseTitle;
        throw new com.domcouch.formula.ContextNotSupportedException("getDatabaseTitle");
    }

    @Override
    public String getReplicaID() {
        if (replicaID != null) return replicaID;
        throw new com.domcouch.formula.ContextNotSupportedException("getReplicaID");
    }

    // ---- Document metadata ----

    @Override
    public java.util.List<String> getFolderNames() {
        return document.getFolderNames();
    }

    @Override
    public boolean isDocumentValid() {
        return true; // Couchbase documents are always valid
    }

    @Override
    public int getAttachmentCount() {
        return 0; // attachments not yet supported
    }

    // ---- Document lifecycle ----

    @Override
    public void addToFolder(String folderName) {
        document.putInFolder(folderName);
    }

    @Override
    public void hardDelete() {
        try { document.remove(); } catch (com.domcouch.api.NotesException e) {
            throw new RuntimeException(e);
        }
    }

    // ---- Document locking (not supported by Couchbase backend) ----
    // lockDocument, unlockDocument, getDocumentLockStatus, isDocumentLockingEnabled
    // use default → ContextNotSupportedException

    // ---- Session / environment (not applicable in Couchbase) ----
    // getDomain, getEnvironmentValue use default → ContextNotSupportedException

    // ---- Document size (not exposed by Document API) ----
    // getDocumentSize uses default → ContextNotSupportedException

    // ---- Soft deletion (not supported by Couchbase) ----
    // markForDeletion, unmarkForDeletion use default → ContextNotSupportedException

    // ---- Cross-database lookups (optional database reference) ----

    private com.domcouch.api.Database database;

    public DocumentFormulaContext withDatabase(com.domcouch.api.Database db) {
        this.database = db;
        return this;
    }

    @Override
    public java.util.List<Object> dbLookup(String server, String database, String view, Object key, int column) {
        if (this.database == null)
            throw new com.domcouch.formula.ContextNotSupportedException("dbLookup");
        try {
            var v = this.database.getView(view);
            if (v == null) return java.util.List.of();
            var entries = v.getAllEntriesByKey(key);
            var result = new java.util.ArrayList<>();
            for (var entry : entries) {
                var val = entry.getColumnValue(column - 1); // 1-based → 0-based
                if (val != null) result.add(val);
            }
            return result;
        } catch (Exception e) { return java.util.List.of(); }
    }

    @Override
    public java.util.List<Object> dbColumn(String server, String database, String view, int column) {
        if (this.database == null)
            throw new com.domcouch.formula.ContextNotSupportedException("dbColumn");
        try {
            var v = this.database.getView(view);
            if (v == null) return java.util.List.of();
            var entries = v.getAllEntries();
            var result = new java.util.ArrayList<>();
            for (var entry : entries) {
                var val = entry.getColumnValue(column - 1);
                if (val != null) result.add(val);
            }
            return result;
        } catch (Exception e) { return java.util.List.of(); }
    }
}
