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
 * Multi-value items are returned as {@link java.util.Vector} lists;
 * single-value items return the first value directly.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   DocumentFormulaContext ctx = new DocumentFormulaContext(document);
 *   Object result = translator.evaluate("FirstName + \" \" + LastName", ctx);
 * }</pre>
 */
public class DocumentFormulaContext implements FormulaContext {

    private final Document document;

    /**
     * @param document the document to resolve fields from
     */
    public DocumentFormulaContext(Document document) {
        this.document = document;
    }

    @Override
    public Object resolve(String name) {
        Item item = document.getFirstItem(name);
        if (item == null) return "";
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

    /** @return the underlying document */
    public Document getDocument() {
        return document;
    }
}
