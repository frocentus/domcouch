package com.domcouch.api;

import java.util.List;

/**
 * Represents a Domino form definition — a schema for documents.
 * Forms define field names, types, computation rules, and validation.
 * Mirrors the Domino Form design element.
 *
 * <p>Usage:
 * <pre>
 *   Form form = db.getForm("Person");
 *   doc.computeWithForm(form, false, false);
 * </pre>
 */
public interface Form {

    /** @return the form name (matches Document Form item) */
    String getName();

    /** @return all field definitions in this form */
    List<FieldDefinition> getFields();

    /** @return a specific field definition, or null */
    FieldDefinition getField(String name);

    /**
     * A single field definition within a Form.
     */
    interface FieldDefinition {

        /** @return the field name */
        String getName();

        /** @return the field type (Item.TEXT, Item.NUMBERS, etc.) */
        int getType();

        /** @return true if this field is computed (re-evaluated on every save) */
        boolean isComputed();

        /** @return true if computed once at document creation time */
        boolean isComputedWhenComposed();

        /** @return true if computed for display only (not stored) */
        boolean isComputedForDisplay();

        /** @return the formula for computed fields, or null */
        String getFormula();

        /** @return the default value formula, or null */
        String getDefaultFormula();

        /** @return the validation formula, or null */
        String getValidationFormula();

        /** @return the validation error message, or null */
        String getValidationMessage();

        /** @return true if the field allows multiple values */
        boolean isMultiValue();

        /** @return true if this is a RichText field */
        boolean isRichText();

        /** @return the number format pattern (e.g., "#,##0.00"), or null */
        String getNumberFormat();

        /** @return the date format pattern, or null */
        String getDateFormat();
    }
}
