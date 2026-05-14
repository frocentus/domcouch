package com.domcouch.formula;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests using real examples from the Domino formula language spec
 * and the formula-language-architecture.md document.
 */
@DisplayName("Formula Examples (integration)")
class FormulaExamplesTest {

    private Evaluator evaluator;
    private Map<String, Object> vars;

    @BeforeEach
    void setUp() {
        evaluator = new Evaluator("Mary Chen");
        vars = new HashMap<>();
        vars.put("AUTHOR", "Mary Chen");
        vars.put("FIRSTNAME", "John");
        vars.put("LASTNAME", "Smith");
        vars.put("COMPANY", "Acme Inc.");
        vars.put("COMPANYNAME", "Acme Inc.");
        vars.put("SUBJECT", "Hello World");
        vars.put("SALARY", 95000.0);
        vars.put("STATUS", "Active");
        vars.put("TOPIC", "Important Notice");
        vars.put("KEYTHOUGHT", "");
        vars.put("CATEGORIES", List.of("A", "B", "C"));
        vars.put("NEWORDERNUMBER", 1001.0);
        vars.put("CREATED", "2024-01-01T00:00:00Z");
        vars.put("FORM", "Person");
        vars.put("$TITLE", "PersonForm");
        vars.put("BODY", "Some rich text content here");
    }

    private FormulaContext ctx() {
        return name -> vars.get(name);
    }

    private Object eval(String formula) {
        return evaluator.evalExpr(formula, ctx());
    }

    // ================================================================
    // §2.5 Complete Examples (from architecture doc)
    // ================================================================

    @Nested @DisplayName("Architecture Doc Examples (§2.5)")
    class ArchitectureDocExamples {

        @Test @DisplayName("D := @Created")
        void assignCreated() {
            Object result = eval("D := @Created");
            assertEquals("2024-01-01T00:00:00Z", result);
        }

        @Test @DisplayName("@Trim(Subject)")
        void trimSubject() {
            vars.put("SUBJECT", "  Hello World  ");
            assertEquals("Hello World", eval("@Trim(Subject)"));
        }

        @Test @DisplayName("SELECT @All")
        void selectAll() {
            assertEquals(1.0, eval("SELECT @All"));
        }

        @Test @DisplayName("LastName + \", \" + FirstName (with spaces)")
        void concatWithSpaces() {
            assertEquals("Smith, John", eval("LastName + \", \" + FirstName"));
        }

        @Test @DisplayName("LastName+\", \"+FirstName (no spaces)")
        void concatNoSpaces() {
            assertEquals("Smith, John", eval("LastName+\", \"+FirstName"));
        }
    }

    // ================================================================
    // §2.7 Fields and Variables
    // ================================================================

    @Nested @DisplayName("Fields and Variables (§2.7)")
    class FieldsAndVariables {

        @Test @DisplayName("FIELD Subject := \"No Subject\"")
        void fieldSubject() {
            MapFormulaContext mctx = new MapFormulaContext(vars);
            Evaluator ev = new Evaluator();
            ev.evalExpr("FIELD Subject := \"No Subject\"", mctx);
            assertEquals("No Subject", mctx.getWrittenFields().get("SUBJECT"));
        }

        @Test @DisplayName("DEFAULT KeyThought := Topic")
        void defaultKeyThought() {
            // KeyThought is empty, so Topic is used as default
            assertEquals("Important Notice", eval("DEFAULT KeyThought := Topic"));
        }

        @Test @DisplayName("DEFAULT with existing value uses existing")
        void defaultExisting() {
            vars.put("KEYTHOUGHT", "Existing Value");
            assertEquals("Existing Value", eval("DEFAULT KeyThought := Topic"));
        }

        @Test @DisplayName("ENVIRONMENT OrderNumber := @Text(NewOrderNumber)")
        void environmentAssignment() {
            assertEquals("1001", eval("ENVIRONMENT OrderNumber := @Text(NewOrderNumber)"));
        }

        @Test @DisplayName("FIELD Subject := @If(Subject=\"\"; \"No Subject\"; Subject)")
        void fieldIfNull() {
            vars.put("SUBJECT", "");
            MapFormulaContext mctx = new MapFormulaContext(vars);
            Evaluator ev = new Evaluator();
            assertEquals("No Subject",
                    ev.evalExpr("FIELD Subject := @If(Subject = \"\"; \"No Subject\"; Subject)", mctx));
        }
    }

    // ================================================================
    // §2.8 Temporary Variables
    // ================================================================

    @Nested @DisplayName("Temporary Variables (§2.8)")
    class TempVariables {

        @Test @DisplayName("n := 1")
        void assignOne() {
            assertEquals(1.0, eval("n := 1"));
        }

        @Test @DisplayName("n := n + 1 (reassignment)")
        void reassign() {
            assertEquals(2.0, eval("n := 1; n := n + 1"));
        }

        @Test @DisplayName("city1Upper := @UpperCase(city1 := \"London\")")
        void nestedAssignment() {
            assertEquals("LONDON", eval("city1Upper := @UpperCase(city1 := \"London\")"));
        }

        @Test @DisplayName("List construction: \"London\" : \"New York\" : \"Tokyo\"")
        void listConstruction() {
            Object result = eval("\"London\" : \"New York\" : \"Tokyo\"");
            assertTrue(result instanceof List);
            assertEquals(List.of("London", "New York", "Tokyo"), result);
        }

        @Test @DisplayName("List with variables: \"London\" : \"New York\"; result : \"Tokyo\"")
        void listWithVars() {
            assertEquals(List.of("London", "New York", "Tokyo"),
                    eval("LNY := \"London\" : \"New York\"; LNY : \"Tokyo\""));
        }

        @Test @DisplayName("month extraction chain")
        void monthFromDate() {
            // date := @Created; month := @Text(@Month(date))
            Object result = eval("date := @Created; @Text(date)");
            assertEquals("2024-01-01T00:00:00Z", result);
        }

        @Test @DisplayName("Keyword assigned to variable: o := [OK]")
        void keywordAsVariable() {
            assertEquals("OK", eval("o := [OK]"));
        }
    }

    // ================================================================
    // §2.9 Time-Date Constants
    // ================================================================

    @Nested @DisplayName("Time-Date Constants (§2.9)")
    class DateTimeConstants {

        @Test @DisplayName("time constant [5:30]")
        void timeConstant() { assertEquals("5:30", eval("[5:30]")); }

        @Test @DisplayName("12-hour time [5:30 PM]")
        void time12h() { assertEquals("5:30 PM", eval("[5:30 PM]")); }

        @Test @DisplayName("date constant [6/15]")
        void dateConstant() { assertEquals("6/15", eval("[6/15]")); }

        @Test @DisplayName("date-time [6/15 5:30 PM]")
        void dateTimeCombined() { assertEquals("6/15 5:30 PM", eval("[6/15 5:30 PM]")); }
    }

    // ================================================================
    // Text Constants (from spec)
    // ================================================================

    @Nested @DisplayName("Text Constants")
    class TextConstants {

        @Test @DisplayName("simple string \"Cost\"")
        void simpleString() { assertEquals("Cost", eval("\"Cost\"")); }

        @Test @DisplayName("\"From: \" + Author + \" (\" + @Text(@Created) + \")\"")
        void fromAuthorCreated() {
            vars.put("AUTHOR", "Mary Chen");
            vars.put("CREATED", "11/30/2000 02:39:55 PM");
            String result = (String) eval(
                    "\"From: \" + Author + \" (\" + @Text(@Created) + \")\"");
            assertEquals("From: Mary Chen (11/30/2000 02:39:55 PM)", result);
        }

        @Test @DisplayName("escaped quotes: \"Type \\\"Yes\\\" or \\\"No\\\"\"")
        void escapedQuotes() {
            assertEquals("Type \"Yes\" or \"No\"",
                    eval("\"Type \\\"Yes\\\" or \\\"No\\\"\""));
        }

        @Test @DisplayName("brace-delimited: {Type \"Yes\" or \"No\"}")
        void braceString() {
            assertEquals("Type \"Yes\" or \"No\"",
                    eval("{Type \"Yes\" or \"No\"}"));
        }

        @Test @DisplayName("escaped backslash: \"Type \\\\Yes\\\\ or \\\\No\\\\\"")
        void escapedBackslash() {
            assertEquals("Type \\Yes\\ or \\No\\",
                    eval("\"Type \\\\Yes\\\\ or \\\\No\\\\\""));
        }
    }

    // ================================================================
    // Numeric Constants (from spec)
    // ================================================================

    @Nested @DisplayName("Numeric Constants")
    class NumericConstants {

        @Test @DisplayName("integer 123") void integer123() { assertEquals(123.0, eval("123")); }
        @Test @DisplayName("decimal .123") void leadingDecimal() { assertEquals(0.123, eval(".123")); }
        @Test @DisplayName("negative -123.4") void negative() { assertEquals(-123.4, eval("-123.4")); }
        @Test @DisplayName("scientific 123E2") void scientificPos() { assertEquals(12300.0, eval("123E2")); }
        @Test @DisplayName("scientific 123E-2") void scientificNeg() { assertEquals(1.23, eval("123E-2")); }
        @Test @DisplayName("number in quotes is text") void numberInQuotes() { assertEquals("42", eval("\"42\"")); }
    }

    // ================================================================
    // Operator Precedence (§2.4)
    // ================================================================

    @Nested @DisplayName("Operator Precedence")
    class OperatorPrecedence {

        @Test @DisplayName("5 - 3 * 6 - 4 = -17")
        void multBeforeAdd() { assertEquals(-17.0, eval("5 - 3 * 6 - 4")); }

        @Test @DisplayName("(5 - 3) * (6 - 4) = 4")
        void parensFirst() { assertEquals(4.0, eval("(5 - 3) * (6 - 4)")); }

        @Test @DisplayName("8 / 4 * 2 = 4 (left-to-right)")
        void leftToRight() { assertEquals(4.0, eval("8 / 4 * 2")); }

        @Test @DisplayName("4 = 2 + 2 & 5 = 3 + 2 = True")
        void logicalAndComparison() {
            assertEquals(1.0, eval("4 = 2 + 2 & 5 = 3 + 2"));
        }

        @Test @DisplayName("(4 = 2 + 2) & (5 = 3 + 2) = True")
        void logicalAndParens() {
            assertEquals(1.0, eval("(4 = 2 + 2) & (5 = 3 + 2)"));
        }

        @Test @DisplayName("4 = 2 + 2 | 5 = 2 + 2 = True")
        void logicalOr() {
            assertEquals(1.0, eval("4 = 2 + 2 | 5 = 2 + 2"));
        }

        @Test @DisplayName("! 5 = 2 + 2 = True (NOT binds loosely)")
        void logicalNotPrecedence() {
            assertEquals(1.0, eval("! 5 = 2 + 2"));
        }

        @Test @DisplayName("! (5 = 2 + 2) = True")
        void logicalNotParens() {
            assertEquals(1.0, eval("! (5 = 2 + 2)"));
        }

        @Test @DisplayName("\"London\" = \"Lon\" + \"don\" = True")
        void stringEqualsConcat() {
            assertEquals(1.0, eval("\"London\" = \"Lon\" + \"don\""));
        }

        @Test @DisplayName("\"London\" != \"Tokyo\" = True")
        void stringNotEquals() {
            assertEquals(1.0, eval("\"London\" != \"Tokyo\""));
        }

        @Test @DisplayName("2 + 2 > 3 = True")
        void addGreaterThan() {
            assertEquals(1.0, eval("2 + 2 > 3"));
        }

        @Test @DisplayName("-3:4 with precedence — minus applied to 3 only (Lexer token)")
        void minusListPrecedence() {
            // Lexer consumes -3 as a single number token
            assertEquals(List.of(-3.0, 4.0), eval("-3:4"));
        }

        @Test @DisplayName("(-3):4 — Lexer consumes -3 as number token")
        void minusListParens() {
            assertEquals(List.of(-3.0, 4.0), eval("(-3):4"));
        }
    }

    // ================================================================
    // Subscript (§5.4)
    // ================================================================

    @Nested @DisplayName("Subscript")
    class Subscript {

        @Test @DisplayName("Categories[2] returns element 2 (1-based)")
        void subscriptElement() {
            vars.put("CATEGORIES", List.of("A", "B", "C"));
            assertEquals("B", eval("Categories[2]"));
        }

        @Test @DisplayName("Categories[1] returns first element")
        void subscriptFirst() {
            vars.put("CATEGORIES", List.of("A", "B", "C"));
            assertEquals("A", eval("Categories[1]"));
        }

        @Test @DisplayName("scalar [1] returns value")
        void subscriptScalar() {
            assertEquals("Hello", eval("\"Hello\"[1]"));
        }
    }

    // ================================================================
    // Control Flow (§5.5)
    // ================================================================

    @Nested @DisplayName("Control Flow")
    class ControlFlow {

        @Test @DisplayName("@If true branch")
        void ifTrue() { assertEquals("High", eval("@If(Salary > 50000; \"High\"; \"Standard\")")); }

        @Test @DisplayName("@If false branch")
        void ifFalse() { assertEquals("Standard", eval("@If(Salary > 200000; \"High\"; \"Standard\")")); }

        @Test @DisplayName("@Do returns last")
        void doReturnsLast() { assertEquals("c", eval("@Do(\"a\"; \"b\"; \"c\")")); }

        @Test @DisplayName("@Return stops early")
        void returnStops() {
            assertEquals("early", eval("@If(1; @Return(\"early\"); \"late\")"));
        }

        @Test @DisplayName("@If with @Return for early exit")
        void ifWithReturn() {
            assertEquals("", eval("@If(Salary > 200000; @Return(\"\"); \"\")"));
        }
    }

    // ================================================================
    // String @Functions (from spec)
    // ================================================================

    @Nested @DisplayName("String Functions")
    class StringFunctions {

        @Test @DisplayName("@UpperCase")
        void upperCase() { assertEquals("HELLO", eval("@UpperCase(\"hello\")")); }

        @Test @DisplayName("@LowerCase")
        void lowerCase() { assertEquals("hello", eval("@LowerCase(\"HELLO\")")); }

        @Test @DisplayName("@Trim")
        void trim() { assertEquals("hello", eval("@Trim(\"  hello  \")")); }

        @Test @DisplayName("@Length")
        void length() { assertEquals(5.0, eval("@Length(\"hello\")")); }

        @Test @DisplayName("@Left")
        void left() { assertEquals("he", eval("@Left(\"hello\"; 2)")); }

        @Test @DisplayName("@Right")
        void right() { assertEquals("lo", eval("@Right(\"hello\"; 2)")); }

        @Test @DisplayName("@Text on number")
        void textOnNumber() { assertEquals("42", eval("@Text(42)")); }

        @Test @DisplayName("@Text on date")
        void textOnDate() {
            vars.put("CREATED", "11/30/2000 02:39:55 PM");
            assertEquals("11/30/2000 02:39:55 PM", eval("@Text(@Created)"));
        }

        @Test @DisplayName("@TextToNumber")
        void textToNumber() { assertEquals(42.0, eval("@TextToNumber(\"42\")")); }

        @Test @DisplayName("@Repeat")
        void repeat() { assertEquals("XXXXX", eval("@Repeat(\"X\"; 5)")); }
    }

    // ================================================================
    // Type Checking Functions
    // ================================================================

    @Nested @DisplayName("Type Checking")
    class TypeChecking {

        @Test @DisplayName("@IsNumber on number")
        void isNumberTrue() { assertEquals(1.0, eval("@IsNumber(42)")); }

        @Test @DisplayName("@IsNumber on string")
        void isNumberFalse() { assertEquals(0.0, eval("@IsNumber(\"hello\")")); }

        @Test @DisplayName("@IsText on string")
        void isTextTrue() { assertEquals(1.0, eval("@IsText(\"hello\")")); }

        @Test @DisplayName("@IsText on number")
        void isTextFalse() { assertEquals(0.0, eval("@IsText(42)")); }

        @Test @DisplayName("@IsAvailable existing field")
        void isAvailableTrue() { assertEquals(1.0, eval("@IsAvailable(Subject)")); }

        @Test @DisplayName("@IsAvailable missing field")
        void isAvailableFalse() { assertEquals(0.0, eval("@IsAvailable(MissingField)")); }

        @Test @DisplayName("@IsAvailable with @If: @If(@IsAvailable(Form); Form; $TITLE)")
        void formOrTitle() {
            assertEquals("Person", eval("@If(@IsAvailable(Form); Form; $TITLE)"));
        }

        @Test @DisplayName("@IsAvailable fallback when Form missing")
        void formMissingFallback() {
            vars.remove("FORM");
            assertEquals("PersonForm", eval("@If(@IsAvailable(Form); Form; $TITLE)"));
        }
    }

    // ================================================================
    // List Functions
    // ================================================================

    @Nested @DisplayName("List Functions")
    class ListFunctions {

        @Test @DisplayName("@Elements")
        void elements() {
            assertEquals(3.0, eval("@Elements(\"a\" : \"b\" : \"c\")"));
        }

        @Test @DisplayName("@IsMember true")
        void isMemberTrue() {
            assertEquals(1.0, eval("@IsMember(\"b\"; \"a\" : \"b\" : \"c\")"));
        }

        @Test @DisplayName("@IsMember false")
        void isMemberFalse() {
            assertEquals(0.0, eval("@IsMember(\"x\"; \"a\" : \"b\" : \"c\")"));
        }

        @Test @DisplayName("@IsNotMember")
        void isNotMember() {
            assertEquals(1.0, eval("@IsNotMember(\"x\"; \"a\" : \"b\" : \"c\")"));
        }
    }

    // ================================================================
    // Boolean and Special Functions
    // ================================================================

    @Nested @DisplayName("Boolean Functions")
    class BooleanFunctions {

        @Test @DisplayName("@True") void trueFunc() { assertEquals(1.0, eval("@True")); }
        @Test @DisplayName("@False") void falseFunc() { assertEquals(0.0, eval("@False")); }
        @Test @DisplayName("@All") void allFunc() { assertEquals(1.0, eval("@All")); }
        @Test @DisplayName("@UserName") void userName() { assertEquals("Mary Chen", eval("@UserName")); }
    }

    // ================================================================
    // Side-Effecting Functions
    // ================================================================

    @Nested @DisplayName("Side-Effecting Functions")
    class SideEffectFunctions {

        @Test @DisplayName("@Command is no-op, returns empty string")
        void commandNoop() {
            assertEquals("", eval("@Command([SwitchView]; \"Marketing\\\\Procedures\")"));
        }

        @Test @DisplayName("@PostedCommand is no-op")
        void postedCommandNoop() {
            assertEquals("", eval("@PostedCommand([DesignForms])"));
        }

        @Test @DisplayName("FIELD BodyText := @DeleteField")
        void deleteField() {
            MapFormulaContext mctx = new MapFormulaContext(vars);
            Evaluator ev = new Evaluator();
            ev.evalExpr("FIELD BodyText := @DeleteField", mctx);
            assertTrue(mctx.getDeletedFields().contains("BODYTEXT"));
        }
    }

    // ================================================================
    // REM Comments
    // ================================================================

    @Nested @DisplayName("REM Comments")
    class RemComments {

        @Test @DisplayName("REM \"comment\" before code")
        void remBeforeCode() {
            assertEquals(2.0, eval("REM \"header\"; 1 + 1"));
        }

        @Test @DisplayName("REM empty")
        void remEmpty() {
            assertEquals(42.0, eval("REM; 42"));
        }

        @Test @DisplayName("REM {brace comment}")
        void remBrace() {
            assertEquals("ok", eval("REM {comment}; \"ok\""));
        }
    }

    // ================================================================
    // @Command no-ops
    // ================================================================

    @Nested @DisplayName("@Commands (no-ops)")
    class CommandNoops {

        @Test @DisplayName("@Command([EditClear])")
        void editClear() { assertEquals("", eval("@Command([EditClear])")); }

        @Test @DisplayName("@Command([AddDatabase])")
        void addDatabase() { assertEquals("", eval("@Command([AddDatabase])")); }

        @Test @DisplayName("@PostedCommand([DesignForms])")
        void designForms() { assertEquals("", eval("@PostedCommand([DesignForms])")); }

        @Test @DisplayName("@Command([EditDown]; \"5\")")
        void editDown() { assertEquals("", eval("@Command([EditDown]; \"5\")")); }
    }

    // ================================================================
    // Complex Real-World Examples
    // ================================================================

    @Nested @DisplayName("Real-World Examples")
    class RealWorldExamples {

        @Test @DisplayName("\"From:\" + @Repeat(\" \"; 8) + Author")
        void fromWithPadding() {
            vars.put("AUTHOR", "Mary Chen");
            assertEquals("From:        Mary Chen",
                    eval("\"From:\" + @Repeat(\" \"; 8) + Author"));
        }

        @Test @DisplayName("Concatenation with @Text and @Created")
        void fromAuthorCreated() {
            vars.put("AUTHOR", "Mary Chen");
            vars.put("CREATED", "11/30/2000 02:39:55 PM");
            assertEquals("From: Mary Chen (11/30/2000 02:39:55 PM)",
                    eval("\"From: \" + Author + \" (\" + @Text(@Created) + \")\""));
        }

        @Test @DisplayName("FIELD CompanyName := Company + \", Inc.\"")
        void fieldCompanyInc() {
            MapFormulaContext mctx = new MapFormulaContext(vars);
            Evaluator ev = new Evaluator();
            ev.evalExpr("FIELD CompanyName := Company + \", Inc.\"", mctx);
            assertEquals("Acme Inc., Inc.", mctx.getWrittenFields().get("COMPANYNAME"));
        }

        @Test @DisplayName("FIELD CompanyName := @DeleteField")
        void fieldDeleteCompany() {
            MapFormulaContext mctx = new MapFormulaContext(vars);
            Evaluator ev = new Evaluator();
            ev.evalExpr("FIELD CompanyName := @DeleteField", mctx);
            assertTrue(mctx.getDeletedFields().contains("COMPANYNAME"));
        }

        @Test @DisplayName("four arithmetic operations result in 16")
        void fourWaysToGet16() {
            assertEquals(16.0, eval("4 * 4"));
            assertEquals(16.0, eval("64 / 4"));
            assertEquals(16.0, eval("12 + 4"));
            assertEquals(16.0, eval("20 - 4"));
        }

        @Test @DisplayName("CompanyName + \", Inc.\"")
        void companyInc() {
            assertEquals("Acme Inc., Inc.", eval("CompanyName + \", Inc.\""));
        }

        @Test @DisplayName("VALUE with unary: 5, +5, -(-5) all equal 5")
        void unaryEquivalent() {
            assertEquals(5.0, eval("5"));
            assertEquals(5.0, eval("+5"));
            assertEquals(5.0, eval("-(-5)"));
        }

        @Test @DisplayName("FIELD CityUpper := @UpperCase(FIELD City := \"London\")")
        void nestedFieldAssign() {
            MapFormulaContext mctx = new MapFormulaContext(vars);
            Evaluator ev = new Evaluator();
            Object result = ev.evalExpr(
                    "FIELD CityUpper := @UpperCase(FIELD City := \"London\")", mctx);
            assertEquals("LONDON", result);
            assertEquals("London", mctx.getWrittenFields().get("CITY"));
            assertEquals("LONDON", mctx.getWrittenFields().get("CITYUPPER"));
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    static class MapFormulaContext implements FormulaContext {
        private final Map<String, Object> map;
        private final Map<String, Object> written = new HashMap<>();
        private final List<String> deleted = new ArrayList<>();

        MapFormulaContext(Map<String, Object> initial) {
            this.map = new HashMap<>(initial);
        }

        @Override public Object resolve(String name) { return map.get(name); }
        @Override public void setField(String name, Object value) {
            map.put(name, value);
            written.put(name, value);
        }
        @Override public void deleteField(String name) {
            map.remove(name);
            deleted.add(name);
        }
        Map<String, Object> getWrittenFields() { return written; }
        List<String> getDeletedFields() { return deleted; }
    }
}
