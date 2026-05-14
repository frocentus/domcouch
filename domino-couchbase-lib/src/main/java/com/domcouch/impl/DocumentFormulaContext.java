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
}
