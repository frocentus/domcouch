package com.domcouch.formula;

import com.domcouch.formula.translate.FormulaTranslator;
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
        assertEquals("doc.items.FORM[0].`values`[0] = 'Person'",
                translator.toN1ql("Form = \"Person\""));
    }

    @Test @DisplayName("case-insensitive field")
    void caseInsensitive() {
        assertEquals("doc.items.FORM[0].`values`[0] = 'Person'",
                translator.toN1ql("form = \"Person\""));
    }

    @Test @DisplayName("AND operator")
    void andOp() {
        assertEquals("(doc.items.FORM[0].`values`[0] = 'Person' AND doc.items.STATUS[0].`values`[0] = 'Active')",
                translator.toN1ql("Form = \"Person\" & Status = \"Active\""));
    }

    @Test @DisplayName("AND without spaces")
    void andNoSpaces() {
        assertEquals("(doc.items.FORM[0].`values`[0] = 'Person' AND doc.items.STATUS[0].`values`[0] = 'Active')",
                translator.toN1ql("Form = \"Person\"&Status = \"Active\""));
    }

    @Test @DisplayName("& inside string literal preserved")
    void ampInString() {
        assertEquals("doc.items.FORM[0].`values`[0] = 'A & B'",
                translator.toN1ql("Form = \"A & B\""));
    }

    @Test @DisplayName("OR operator")
    void orOp() {
        assertEquals("(doc.items.FORM[0].`values`[0] = 'Person' OR doc.items.STATUS[0].`values`[0] = 'Active')",
                translator.toN1ql("Form = \"Person\" | Status = \"Active\""));
    }

    @Test @DisplayName("NOT operator")
    void notOp() {
        assertEquals("(doc.items.FORM[0].`values`[0] = 'Person' AND NOT (doc.items.STATUS[0].`values`[0] = 'Closed'))",
                translator.toN1ql("Form = \"Person\" & !Status = \"Closed\""));
    }

    @Test @DisplayName("!= preserved")
    void neqPreserved() {
        assertEquals("doc.items.FORM[0].`values`[0] != 'Person'",
                translator.toN1ql("Form != \"Person\""));
    }

    @Test @DisplayName(">< mapped to !=")
    void altNotEqual() {
        assertEquals("doc.items.FORM[0].`values`[0] != 'Person'",
                translator.toN1ql("Form >< \"Person\""));
    }

    @Test @DisplayName("! inside string literal preserved")
    void notInString() {
        assertEquals("doc.items.FORM[0].`values`[0] = 'Hi!'",
                translator.toN1ql("Form = \"Hi!\""));
    }

    @Test @DisplayName("@IsResponseDoc")
    void isResponseDoc() {
        assertEquals("(doc.items.FORM[0].`values`[0] = 'Memo' AND doc.parentUNID IS NOT MISSING)",
                translator.toN1ql("Form = \"Memo\" & @IsResponseDoc"));
    }

    @Test @DisplayName("@Contains")
    void atContains() {
        assertEquals("CONTAINS(doc.items.SUBJECT[0].`values`[0], 'Meeting')",
                translator.toN1ql("@Contains(Subject; \"Meeting\")"));
    }

    @Test @DisplayName("@LowerCase")
    void atLowerCase() {
        assertEquals("LOWER(doc.items.LASTNAME[0].`values`[0]) = 'smith'",
                translator.toN1ql("@LowerCase(LastName) = \"smith\""));
    }

    @Test @DisplayName("@UserName")
    void atUserName() {
        assertEquals("doc.items.AUTHOR[0].`values`[0] = 'Alice'",
                translator.toN1ql("Author = @UserName"));
    }

    @Test @DisplayName("@Today")
    void atToday() {
        assertEquals("doc.items.REPORTDATE[0].`values`[0] = NOW_STR()",
                translator.toN1ql("ReportDate = @Today"));
    }

    @Test @DisplayName("@If")
    void atIf() {
        assertEquals("CASE WHEN doc.items.PRICE[0].`values`[0] > 100 THEN 'Expensive' ELSE 'Cheap' END",
                translator.toN1ql("@If(Price > 100; \"Expensive\"; \"Cheap\")"));
    }

    @Test @DisplayName("@Created / @Modified")
    void createdModified() {
        assertEquals("(doc.created > '2024-01-01' AND doc.lastModified > '2024-06-01')",
                translator.toN1ql("@Created > \"2024-01-01\" & @Modified > \"2024-06-01\""));
    }

    @Test @DisplayName("@IsAvailable with string literal")
    void atIsAvailableString() {
        assertEquals("doc.items.SUBJECT[0].`values`[0] IS NOT MISSING",
                translator.toN1ql("@IsAvailable(\"Subject\")"));
    }

    @Test @DisplayName("SELECT stripped")
    void selectStripped() {
        assertEquals("doc.items.FORM[0].`values`[0] = 'Person'",
                translator.toN1ql("SELECT Form = \"Person\""));
    }

    @Test @DisplayName("arithmetic in @If is parenthesized")
    void arithmeticParenthesized() {
        assertEquals("doc.items.TOTAL[0].`values`[0] = ((doc.items.PRICE[0].`values`[0] + doc.items.TAX[0].`values`[0]) * doc.items.QTY[0].`values`[0])",
                translator.toN1ql("Total = (Price + Tax) * Qty"));
    }

    @Test @DisplayName("null returns null")
    void nullInput() {
        assertNull(translator.toN1ql(null));
    }

    @Test @DisplayName("already-translated passes through")
    void alreadyTranslated() {
        assertEquals("doc.items.Form[0].`values`[0] = 'Person'",
                translator.toN1ql("doc.items.Form[0].`values`[0] = 'Person'"));
    }

    // ---- Quick-win @Function translations ----

    @Test @DisplayName("@Month")
    void atMonth() {
        assertEquals("DATE_PART_STR(doc.items.DATE[0].`values`[0], 'month') = 1",
                translator.toN1ql("@Month(Date) = 1"));
    }

    @Test @DisplayName("@Year")
    void atYear() {
        assertEquals("DATE_PART_STR(doc.created, 'year') = 2024",
                translator.toN1ql("@Year(@Created) = 2024"));
    }

    @Test @DisplayName("@Abs")
    void atAbs() {
        assertEquals("ABS(doc.items.AMOUNT[0].`values`[0]) > 100",
                translator.toN1ql("@Abs(Amount) > 100"));
    }

    @Test @DisplayName("@Tomorrow")
    void atTomorrow() {
        assertEquals("DATE_ADD_STR(NOW_STR(), 1, 'day')",
                translator.toN1ql("@Tomorrow"));
    }

    @Test @DisplayName("@ReplaceSubstring")
    void atReplaceSubstring() {
        assertEquals("REPLACE(doc.items.NAME[0].`values`[0], 'old', 'new')",
                translator.toN1ql("@ReplaceSubstring(Name; \"old\"; \"new\")"));
    }

    // ---- New translations for custom views ----

    @Test @DisplayName("@IsNewDoc")
    void atIsNewDoc() {
        assertEquals("doc.unid IS MISSING", translator.toN1ql("@IsNewDoc"));
    }

    @Test @DisplayName("@IsUnavailable")
    void atIsUnavailable() {
        assertEquals("doc.items.STATUS[0].`values`[0] IS MISSING",
                translator.toN1ql("@IsUnavailable(Status)"));
    }

    @Test @DisplayName("@Like")
    void atLike() {
        assertEquals("doc.items.NAME[0].`values`[0] LIKE '%Smith%'",
                translator.toN1ql("@Like(Name; \"%Smith%\")"));
    }

    @Test @DisplayName("@Text")
    void atText() {
        assertEquals("TO_STRING(doc.items.AMOUNT[0].`values`[0]) = '100'",
                translator.toN1ql("@Text(Amount) = \"100\""));
    }

    @Test @DisplayName("@TextToNumber")
    void atTextToNumber() {
        assertEquals("TO_NUMBER(doc.items.PRICE[0].`values`[0]) > 50",
                translator.toN1ql("@TextToNumber(Price) > 50"));
    }

    @Test @DisplayName("@Date constructor")
    void atDateConstructor() {
        assertEquals("DATE_STR(2024 || '-' || 6 || '-' || 15) = doc.created",
                translator.toN1ql("@Date(2024; 6; 15) = @Created"));
    }

    @Test @DisplayName("@Adjust")
    void atAdjust() {
        assertEquals("DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(doc.created, 1, 'year'), 0, 'month'), 0, 'day'), 0, 'hour'), 0, 'minute'), 0, 'second')",
                translator.toN1ql("@Adjust(@Created; 1; 0; 0; 0; 0; 0)"));
    }

    @Test @DisplayName("@Word")
    void atWord() {
        assertEquals("SPLIT(doc.items.NAME[0].`values`[0], ' ')[2 - 1]",
                translator.toN1ql("@Word(Name; \" \"; 2)"));
    }

    @Test @DisplayName("@IsNull")
    void atIsNull() {
        assertEquals("doc.items.NAME[0].`values`[0] IS MISSING",
                translator.toN1ql("@IsNull(Name)"));
    }

    @Test @DisplayName("@Explode")
    void atExplode() {
        assertEquals("SPLIT(doc.items.TAGS[0].`values`[0], ',')",
                translator.toN1ql("@Explode(Tags; \",\")"));
    }

    @Test @DisplayName("@Implode")
    void atImplode() {
        assertEquals("ARRAY_JOIN(doc.items.TAGS[0].`values`, '-')",
                translator.toN1ql("@Implode(Tags; \"-\")"));
    }

    @Test @DisplayName("@Count")
    void atCount() {
        assertEquals("ARRAY_LENGTH(doc.items.TAGS[0].`values`)",
                translator.toN1ql("@Count(Tags)"));
    }
}
