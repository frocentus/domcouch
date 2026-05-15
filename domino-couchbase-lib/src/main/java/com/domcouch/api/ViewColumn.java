package com.domcouch.api;

/**
 * Defines a column in a view. Columns can be direct field mappings
 * or formula expressions evaluated against each document.
 */
public class ViewColumn {

    private final String name;
    private final String expression;
    private final boolean isFormula;

    private ViewColumn(String name, String expression, boolean isFormula) {
        this.name = name;
        this.expression = expression;
        this.isFormula = isFormula;
    }

    /** Create a column that maps directly to a document field. */
    public static ViewColumn field(String name, String fieldName) {
        return new ViewColumn(name, fieldName, false);
    }

    /** Create a column whose value is computed by a Domino @Formula expression. */
    public static ViewColumn formula(String name, String formulaExpression) {
        return new ViewColumn(name, formulaExpression, true);
    }

    public String getName() { return name; }
    public String getExpression() { return expression; }
    public boolean isFormula() { return isFormula; }

    @Override
    public String toString() {
        return (isFormula ? "formula:" : "field:") + name + "=" + expression;
    }
}
