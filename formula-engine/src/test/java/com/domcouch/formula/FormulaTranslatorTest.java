package com.domcouch.formula;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FormulaTranslator#toN1ql(String)} — verifies the
 * regex-based N1QL translation against real-world formula patterns.
 */
@DisplayName("FormulaTranslator N1QL translation")
class FormulaTranslatorTest {

    private final FormulaTranslator translator = new FormulaTranslator("Alice");

    // ---- Basic field translation ----

    @Test @DisplayName("field comparison with PascalCase")
    void fieldComparison() {
        assertEquals("doc.items.Form.`values`[0] = 'Person'",
                translator.toN1ql("Form = 'Person'"));
    }

    @Test @DisplayName("field comparison case-insensitive (lowercase)")
    void fieldComparisonCaseInsensitive() {
        assertEquals("doc.items.form.`values`[0] = 'Person'",
                translator.toN1ql("form = 'Person'"));
    }

    @Test @DisplayName("field comparison with LIKE")
    void fieldLike() {
        assertEquals("doc.items.LastName.`values`[0] LIKE '%Smith%'",
                translator.toN1ql("LastName LIKE '%Smith%'"));
    }

    // ---- IS MISSING / IS NOT MISSING ----

    @Test @DisplayName("IS MISSING")
    void isMissing() {
        assertEquals("doc.items.Status IS MISSING",
                translator.toN1ql("Status IS MISSING"));
    }

    @Test @DisplayName("IS NOT MISSING")
    void isNotMissing() {
        assertEquals("doc.items.Status IS NOT MISSING",
                translator.toN1ql("Status IS NOT MISSING"));
    }

    // ---- & operator (with and without spaces) ----

    @Test @DisplayName("AND with spaces")
    void andWithSpaces() {
        assertEquals("doc.items.Form.`values`[0] = 'Person' AND doc.items.Status.`values`[0] = 'Active'",
                translator.toN1ql("Form = 'Person' & Status = 'Active'"));
    }

    @Test @DisplayName("AND without spaces")
    void andWithoutSpaces() {
        assertEquals("doc.items.Form.`values`[0] = 'Person' AND doc.items.Status.`values`[0] = 'Active'",
                translator.toN1ql("Form = 'Person'&Status = 'Active'"));
    }

    @Test @DisplayName("& inside string literal is preserved")
    void andInsideString() {
        assertEquals("doc.items.Form.`values`[0] = 'A & B'",
                translator.toN1ql("Form = 'A & B'"));
    }

    // ---- | operator ----

    @Test @DisplayName("OR with spaces")
    void orWithSpaces() {
        assertEquals("doc.items.Form.`values`[0] = 'Person' OR doc.items.Status.`values`[0] = 'Active'",
                translator.toN1ql("Form = 'Person' | Status = 'Active'"));
    }

    @Test @DisplayName("OR without spaces")
    void orWithoutSpaces() {
        assertEquals("doc.items.Form.`values`[0] = 'Person' OR doc.items.Status.`values`[0] = 'Active'",
                translator.toN1ql("Form = 'Person'|Status = 'Active'"));
    }

    // ---- ! (NOT) operator ----

    @Test @DisplayName("NOT operator")
    void notOperator() {
        assertEquals("doc.items.Form.`values`[0] = 'Person' AND NOT doc.items.Status.`values`[0] = 'Closed'",
                translator.toN1ql("Form = 'Person' & !Status = 'Closed'"));
    }

    @Test @DisplayName("!= is preserved (not rewritten to NOT)")
    void notEqualsPreserved() {
        assertEquals("doc.items.Form.`values`[0] != 'Person'",
                translator.toN1ql("Form != 'Person'"));
    }

    @Test @DisplayName("! inside string literal is preserved")
    void notInsideString() {
        assertEquals("doc.items.Form.`values`[0] = 'Hi!'",
                translator.toN1ql("Form = 'Hi!'"));
    }

    // ---- @Function translation ----

    @Test @DisplayName("@IsResponseDoc")
    void isResponseDoc() {
        assertEquals("doc.items.Form.`values`[0] = 'Memo' AND doc.parentUNID IS NOT MISSING",
                translator.toN1ql("Form = 'Memo' & @IsResponseDoc"));
    }

    @Test @DisplayName("@Contains")
    void atContains() {
        assertEquals("CONTAINS(doc.items.Subject.`values`[0], 'Meeting')",
                translator.toN1ql("@Contains(Subject; 'Meeting')"));
    }

    @Test @DisplayName("@LowerCase")
    void atLowerCase() {
        assertEquals("LOWER(doc.items.LastName.`values`[0]) = 'smith'",
                translator.toN1ql("@LowerCase(LastName) = 'smith'"));
    }

    @Test @DisplayName("@UserName")
    void atUserName() {
        assertEquals("doc.items.Author.`values`[0] = 'Alice'",
                translator.toN1ql("Author = @UserName"));
    }

    @Test @DisplayName("@Today")
    void atToday() {
        assertEquals("doc.items.ReportDate.`values`[0] = NOW_STR()",
                translator.toN1ql("ReportDate = @Today"));
    }

    @Test @DisplayName("@If")
    void atIf() {
        assertEquals("CASE WHEN doc.items.Price.`values`[0] > 100 THEN 'Expensive' ELSE 'Cheap' END",
                translator.toN1ql("@If(Price > 100; 'Expensive'; 'Cheap')"));
    }

    @Test @DisplayName("@Created / @Modified")
    void createdModified() {
        assertEquals("doc.created > '2024-01-01' AND doc.lastModified > '2024-06-01'",
                translator.toN1ql("@Created > '2024-01-01' & @Modified > '2024-06-01'"));
    }

    @Test @DisplayName("SELECT is stripped")
    void selectStripped() {
        assertEquals("doc.items.Form.`values`[0] = 'Person'",
                translator.toN1ql("SELECT Form = 'Person'"));
    }

    // ---- Edge cases ----

    @Test @DisplayName("null input returns null")
    void nullInput() {
        assertNull(translator.toN1ql(null));
    }

    @Test @DisplayName("already-translated input passes through")
    void alreadyTranslated() {
        assertEquals("doc.items.Form.`values`[0] = 'Person'",
                translator.toN1ql("doc.items.Form.`values`[0] = 'Person'"));
    }
}
