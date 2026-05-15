package com.domcouch.formula;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Evaluator")
class EvaluatorTest {

    private Evaluator evaluator;
    private Map<String, Object> vars;

    @BeforeEach
    void setUp() {
        evaluator = new Evaluator();
        vars = new HashMap<>();
    }

    private FormulaContext ctx() {
        return vars::get;
    }

    private Object eval(String formula) {
        List<Token> tokens = Lexer.tokenize(formula);
        List<Expr> stmts = new Parser(tokens).parse();
        Object result = "";
        for (Expr stmt : stmts) {
            result = evaluator.eval(stmt, ctx());
        }
        return result;
    }

    // ---- Constants ----

    @Test @DisplayName("string constant")
    void stringConstant() { assertEquals("Hello", eval("\"Hello\"")); }

    @Test @DisplayName("numeric constant")
    void numericConstant() { assertEquals(42.0, eval("42")); }

    @Test @DisplayName("negative number")
    void negativeNumber() { assertEquals(-123.0, eval("-123")); }

    @Test @DisplayName("decimal number")
    void decimalNumber() { assertEquals(3.14, eval("3.14")); }

    // ---- Variables ----

    @Test @DisplayName("variable resolution")
    void variableResolution() {
        vars.put("FIRSTNAME", "Alice");
        assertEquals("Alice", eval("FirstName"));
    }

    @Test @DisplayName("missing variable returns empty string")
    void missingVariable() {
        assertEquals("", eval("DoesNotExist"));
    }

    // ---- Arithmetic ----

    @Test @DisplayName("addition")
    void addition() { assertEquals(5.0, eval("2 + 3")); }

    @Test @DisplayName("subtraction")
    void subtraction() { assertEquals(1.0, eval("3 - 2")); }

    @Test @DisplayName("multiplication")
    void multiplication() { assertEquals(6.0, eval("2 * 3")); }

    @Test @DisplayName("division")
    void division() { assertEquals(2.0, eval("6 / 3")); }

    @Test @DisplayName("precedence: mult before add")
    void multBeforeAdd() { assertEquals(7.0, eval("1 + 2 * 3")); }

    @Test @DisplayName("parentheses")
    void parentheses() { assertEquals(9.0, eval("(1 + 2) * 3")); }

    // ---- String concatenation ----

    @Test @DisplayName("string concatenation")
    void stringConcat() { assertEquals("Hello World", eval("\"Hello\" + \" World\"")); }

    @Test @DisplayName("number plus string is concat")
    void numberPlusString() { assertEquals("42 items", eval("42 + \" items\"")); }

    // ---- Comparison ----

    @Test @DisplayName("equals true")
    void equalsTrue() { assertEquals(1.0, eval("5 = 5")); }

    @Test @DisplayName("equals false")
    void equalsFalse() { assertEquals(0.0, eval("5 = 3")); }

    @Test @DisplayName("not equals")
    void notEquals() { assertEquals(1.0, eval("5 != 3")); }

    @Test @DisplayName("greater than")
    void greaterThan() { assertEquals(1.0, eval("5 > 3")); }

    @Test @DisplayName("less than")
    void lessThan() { assertEquals(0.0, eval("5 < 3")); }

    @Test @DisplayName("string comparison")
    void stringComparison() { assertEquals(1.0, eval("\"abc\" = \"abc\"")); }

    // ---- Logical operators ----

    @Test @DisplayName("logical AND")
    void logicalAnd() { assertEquals(0.0, eval("1 & 0")); }

    @Test @DisplayName("logical OR")
    void logicalOr() { assertEquals(1.0, eval("1 | 0")); }

    @Test @DisplayName("logical NOT")
    void logicalNot() { assertEquals(0.0, eval("! 1")); }

    @Test @DisplayName("truthy: non-zero number")
    void truthyNumber() { assertEquals(1.0, eval("42 & 1")); }

    @Test @DisplayName("falsy: empty string")
    void falsyEmptyString() { assertEquals(0.0, eval("\"\" & 1")); }

    // ---- Unary operators ----

    @Test @DisplayName("unary minus")
    void unaryMinus() {
        vars.put("X", 5.0);
        assertEquals(-5.0, eval("-x"));
    }

    // ---- Assignment ----

    @Test @DisplayName("assignment stores and returns value, accessible within same eval")
    void assignment() {
        MapFormulaContext ctx = new MapFormulaContext();
        Evaluator ev = new Evaluator();
        Object result = ev.evalExpr("n := 1; n + 1", ctx);
        assertEquals(2.0, result);
    }

    // ---- FIELD assignment ----

    @Test @DisplayName("FIELD stores via setField")
    void fieldAssignment() {
        MapFormulaContext ctx = new MapFormulaContext();
        Evaluator ev = new Evaluator();
        Object result = ev.evalExpr("FIELD Subject := \"Hello\"", ctx);
        assertEquals("Hello", ctx.getWrittenFields().get("SUBJECT"));
    }

    // ---- @Functions (Phase 1) ----

    @Nested @DisplayName("@Functions")
    class FunctionTests {

        @Test @DisplayName("@Trim")
        void trim() { assertEquals("hello", eval("@Trim(\"  hello  \")")); }

        @Test @DisplayName("@UpperCase")
        void upperCase() { assertEquals("HELLO", eval("@UpperCase(\"hello\")")); }

        @Test @DisplayName("@LowerCase")
        void lowerCase() { assertEquals("hello", eval("@LowerCase(\"HELLO\")")); }

        @Test @DisplayName("@Length")
        void length() { assertEquals(5.0, eval("@Length(\"hello\")")); }

        @Test @DisplayName("@Left")
        void left() { assertEquals("he", eval("@Left(\"hello\"; 2)")); }

        @Test @DisplayName("@Right")
        void right() { assertEquals("lo", eval("@Right(\"hello\"; 2)")); }

        @Test @DisplayName("@If true branch")
        void ifTrue() { assertEquals("yes", eval("@If(1; \"yes\"; \"no\")")); }

        @Test @DisplayName("@If false branch")
        void ifFalse() { assertEquals("no", eval("@If(0; \"yes\"; \"no\")")); }

        @Test @DisplayName("@Do returns last value")
        void doReturnsLast() { assertEquals("c", eval("@Do(\"a\"; \"b\"; \"c\")")); }

        @Test @DisplayName("@Text converts number to string")
        void textNumber() { assertEquals("42", eval("@Text(42)")); }

        @Test @DisplayName("@TextToNumber")
        void textToNumber() { assertEquals(42.0, eval("@TextToNumber(\"42\")")); }

        @Test @DisplayName("@IsNumber true")
        void isNumberTrue() { assertEquals(1.0, eval("@IsNumber(42)")); }

        @Test @DisplayName("@IsNumber false")
        void isNumberFalse() { assertEquals(0.0, eval("@IsNumber(\"hello\")")); }

        @Test @DisplayName("@IsText")
        void isText() { assertEquals(1.0, eval("@IsText(\"hello\")")); }

        @Test @DisplayName("@IsAvailable true")
        void isAvailableTrue() {
            vars.put("SUBJECT", "Hello");
            assertEquals(1.0, eval("@IsAvailable(Subject)"));
        }

        @Test @DisplayName("@IsAvailable false")
        void isAvailableFalse() { assertEquals(0.0, eval("@IsAvailable(Missing)")); }

        @Test @DisplayName("@IsAvailable with string literal")
        void isAvailableStringLiteral() {
            vars.put("SUBJECT", "Hello");
            assertEquals(1.0, eval("@IsAvailable(\"SUBJECT\")"));
        }

        @Test @DisplayName("@Elements of list")
        void elements() {
            assertEquals(3.0, eval("@Elements(\"a\" : \"b\" : \"c\")"));
        }

        @Test @DisplayName("@IsMember true")
        void isMemberTrue() { assertEquals(1.0, eval("@IsMember(\"b\"; \"a\" : \"b\" : \"c\")")); }

        @Test @DisplayName("@IsMember false")
        void isMemberFalse() { assertEquals(0.0, eval("@IsMember(\"x\"; \"a\" : \"b\" : \"c\")")); }

        @Test @DisplayName("@All")
        void all() { assertEquals(1.0, eval("@All")); }

        @Test @DisplayName("@True")
        void trueFunc() { assertEquals(1.0, eval("@True")); }

        @Test @DisplayName("@False")
        void falseFunc() { assertEquals(0.0, eval("@False")); }

        @Test @DisplayName("@Created")
        void created() {
            MapFormulaContext ctx = new MapFormulaContext();
            Evaluator ev = new Evaluator();
            assertEquals("2024-01-01T00:00:00Z", ev.evalExpr("@Created", ctx));
        }

        @Test @DisplayName("@Now returns DateTime")
        void now() { assertTrue(eval("@Now") instanceof String); }

        @Test @DisplayName("@UserName")
        void userName() {
            Evaluator ev = new Evaluator("Alice");
            Object result = ev.evalExpr("@UserName", vars::get);
            assertEquals("Alice", result);
        }
    }

    // ---- @Return ----

    @Test @DisplayName("@Return stops evaluation")
    void returnStops() {
        MapFormulaContext ctx = new MapFormulaContext();
        Evaluator ev = new Evaluator();
        Object result = ev.evalExpr("@If(1; @Return(\"early\"); \"late\")", ctx);
        assertEquals("early", result);
    }

    // ---- Edge cases ----

    @Test @DisplayName("null becomes empty string")
    void nullBecomesEmpty() {
        vars.put("X", null);
        assertEquals("", eval("x"));
    }

    @Test @DisplayName("string plus number is concatenation, not arithmetic")
    void stringPlusNumber() {
        vars.put("X", "5");
        assertEquals("55", eval("x + 5"));
    }

    @Test @DisplayName("coerce string to number for multiplication")
    void coerceStringToNumber() {
        vars.put("X", "5");
        assertEquals(25.0, eval("x * 5"));
    }

    @Test @DisplayName("subtraction without spaces: Salary-1000")
    void subtractionNoSpaces() {
        vars.put("SALARY", 5000.0);
        assertEquals(4000.0, eval("Salary-1000"));
    }

    @Test @DisplayName("addition without spaces: a+5")
    void additionNoSpaces() {
        vars.put("A", 10.0);
        assertEquals(15.0, eval("a+5"));
    }

    @Test @DisplayName("String vs Double comparison falls back to string compare")
    void stringVsDoubleCompare() {
        // "10" compared to 5.0 — both are Comparable but different types
        // Falls back to toString comparison: "10" vs "5.0" → -1 (1 < 5 char-wise)
        vars.put("S", "10");
        assertEquals(0.0, eval("@If(s = 5; 1; 0)")); // "10" != "5.0"
    }

    @Test @DisplayName("DEFAULT assignment applies when value is empty list")
    void defaultOnEmptyList() {
        MapFormulaContext mctx = new MapFormulaContext(
                java.util.Map.of("MYLIST", List.of()));
        assertEquals("fallback", evaluator.evalExpr("DEFAULT MyList := \"fallback\"; MyList", mctx));
    }

    @Test @DisplayName("DEFAULT assignment keeps non-empty list")
    void defaultOnNonEmptyList() {
        MapFormulaContext mctx = new MapFormulaContext(
                java.util.Map.of("MYLIST", List.of("a", "b")));
        assertEquals(List.of("a", "b"), evaluator.evalExpr("DEFAULT MyList := \"fallback\"; MyList", mctx));
    }

    // ---- Helpers ----

    /** A context backed by a map that also tracks setField calls. */
    static class MapFormulaContext implements FormulaContext {
        private final Map<String, Object> map = new HashMap<>();
        private final Map<String, Object> written = new HashMap<>();

        MapFormulaContext() { this.map.put("CREATED", "2024-01-01T00:00:00Z"); }
        MapFormulaContext(Map<String, Object> initial) { this(); map.putAll(initial); }

        @Override public Object resolve(String name) { return map.get(name); }
        @Override public void setField(String name, Object value) {
            map.put(name, value);
            written.put(name, value);
        }
        Map<String, Object> getWrittenFields() { return written; }
    }
}
