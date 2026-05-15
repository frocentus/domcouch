package com.domcouch.formula;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FormulaTranslator#toN1ql(String)} — AST-based N1QL translation.
 * Uses valid Domino formula syntax: strings are {@code "..."} not {@code '...'},
 * and {@code LIKE} is not a view selection operator.
 */
@DisplayName("FormulaTranslator N1QL translation")
class FormulaTranslatorTest {

    private final FormulaTranslator translator = new FormulaTranslator("Alice");

    @Test @DisplayName("field comparison")
    void fieldComparison() {
        assertEquals("doc.items.FORM.`values`[0] = 'Person'",
                translator.toN1ql("Form = \"Person\""));
    }

    @Test @DisplayName("case-insensitive field")
    void caseInsensitive() {
        assertEquals("doc.items.FORM.`values`[0] = 'Person'",
                translator.toN1ql("form = \"Person\""));
    }

    @Test @DisplayName("AND operator")
    void andOp() {
        assertEquals("(doc.items.FORM.`values`[0] = 'Person' AND doc.items.STATUS.`values`[0] = 'Active')",
                translator.toN1ql("Form = \"Person\" & Status = \"Active\""));
    }

    @Test @DisplayName("AND without spaces")
    void andNoSpaces() {
        assertEquals("(doc.items.FORM.`values`[0] = 'Person' AND doc.items.STATUS.`values`[0] = 'Active')",
                translator.toN1ql("Form = \"Person\"&Status = \"Active\""));
    }

    @Test @DisplayName("& inside string literal preserved")
    void ampInString() {
        assertEquals("doc.items.FORM.`values`[0] = 'A & B'",
                translator.toN1ql("Form = \"A & B\""));
    }

    @Test @DisplayName("OR operator")
    void orOp() {
        assertEquals("(doc.items.FORM.`values`[0] = 'Person' OR doc.items.STATUS.`values`[0] = 'Active')",
                translator.toN1ql("Form = \"Person\" | Status = \"Active\""));
    }

    @Test @DisplayName("NOT operator")
    void notOp() {
        assertEquals("(doc.items.FORM.`values`[0] = 'Person' AND NOT (doc.items.STATUS.`values`[0] = 'Closed'))",
                translator.toN1ql("Form = \"Person\" & !Status = \"Closed\""));
    }

    @Test @DisplayName("!= preserved")
    void neqPreserved() {
        assertEquals("doc.items.FORM.`values`[0] != 'Person'",
                translator.toN1ql("Form != \"Person\""));
    }

    @Test @DisplayName(">< mapped to !=")
    void altNotEqual() {
        assertEquals("doc.items.FORM.`values`[0] != 'Person'",
                translator.toN1ql("Form >< \"Person\""));
    }

    @Test @DisplayName("! inside string literal preserved")
    void notInString() {
        assertEquals("doc.items.FORM.`values`[0] = 'Hi!'",
                translator.toN1ql("Form = \"Hi!\""));
    }

    @Test @DisplayName("@IsResponseDoc")
    void isResponseDoc() {
        assertEquals("(doc.items.FORM.`values`[0] = 'Memo' AND doc.parentUNID IS NOT MISSING)",
                translator.toN1ql("Form = \"Memo\" & @IsResponseDoc"));
    }

    @Test @DisplayName("@Contains")
    void atContains() {
        assertEquals("CONTAINS(doc.items.SUBJECT.`values`[0], 'Meeting')",
                translator.toN1ql("@Contains(Subject; \"Meeting\")"));
    }

    @Test @DisplayName("@LowerCase")
    void atLowerCase() {
        assertEquals("LOWER(doc.items.LASTNAME.`values`[0]) = 'smith'",
                translator.toN1ql("@LowerCase(LastName) = \"smith\""));
    }

    @Test @DisplayName("@UserName")
    void atUserName() {
        assertEquals("doc.items.AUTHOR.`values`[0] = 'Alice'",
                translator.toN1ql("Author = @UserName"));
    }

    @Test @DisplayName("@Today")
    void atToday() {
        assertEquals("doc.items.REPORTDATE.`values`[0] = NOW_STR()",
                translator.toN1ql("ReportDate = @Today"));
    }

    @Test @DisplayName("@If")
    void atIf() {
        assertEquals("CASE WHEN doc.items.PRICE.`values`[0] > 100 THEN 'Expensive' ELSE 'Cheap' END",
                translator.toN1ql("@If(Price > 100; \"Expensive\"; \"Cheap\")"));
    }

    @Test @DisplayName("@Created / @Modified")
    void createdModified() {
        assertEquals("(doc.created > '2024-01-01' AND doc.lastModified > '2024-06-01')",
                translator.toN1ql("@Created > \"2024-01-01\" & @Modified > \"2024-06-01\""));
    }

    @Test @DisplayName("@IsAvailable with string literal")
    void atIsAvailableString() {
        assertEquals("doc.items.SUBJECT.`values`[0] IS NOT MISSING",
                translator.toN1ql("@IsAvailable(\"Subject\")"));
    }

    @Test @DisplayName("SELECT stripped")
    void selectStripped() {
        assertEquals("doc.items.FORM.`values`[0] = 'Person'",
                translator.toN1ql("SELECT Form = \"Person\""));
    }

    @Test @DisplayName("arithmetic in @If is parenthesized")
    void arithmeticParenthesized() {
        assertEquals("doc.items.TOTAL.`values`[0] = ((doc.items.PRICE.`values`[0] + doc.items.TAX.`values`[0]) * doc.items.QTY.`values`[0])",
                translator.toN1ql("Total = (Price + Tax) * Qty"));
    }

    @Test @DisplayName("null returns null")
    void nullInput() {
        assertNull(translator.toN1ql(null));
    }

    @Test @DisplayName("already-translated passes through")
    void alreadyTranslated() {
        assertEquals("doc.items.Form.`values`[0] = 'Person'",
                translator.toN1ql("doc.items.Form.`values`[0] = 'Person'"));
    }

    // ---- Quick-win @Function translations ----

    @Test @DisplayName("@Month")
    void atMonth() {
        assertEquals("DATE_PART_STR(doc.items.DATE.`values`[0], 'month') = 1",
                translator.toN1ql("@Month(Date) = 1"));
    }

    @Test @DisplayName("@Year")
    void atYear() {
        assertEquals("DATE_PART_STR(doc.created, 'year') = 2024",
                translator.toN1ql("@Year(@Created) = 2024"));
    }

    @Test @DisplayName("@Abs")
    void atAbs() {
        assertEquals("ABS(doc.items.AMOUNT.`values`[0]) > 100",
                translator.toN1ql("@Abs(Amount) > 100"));
    }

    @Test @DisplayName("@Tomorrow")
    void atTomorrow() {
        assertEquals("DATE_ADD_STR(NOW_STR(), 1, 'day')",
                translator.toN1ql("@Tomorrow"));
    }

    @Test @DisplayName("@ReplaceSubstring")
    void atReplaceSubstring() {
        assertEquals("REPLACE(doc.items.NAME.`values`[0], 'old', 'new')",
                translator.toN1ql("@ReplaceSubstring(Name; \"old\"; \"new\")"));
    }
}
