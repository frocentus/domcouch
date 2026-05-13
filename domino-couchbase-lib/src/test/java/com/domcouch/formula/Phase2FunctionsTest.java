package com.domcouch.formula;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Phase 2 @Functions.
 */
@DisplayName("Phase 2 @Functions")
class Phase2FunctionsTest {

    private Evaluator evaluator;
    private Map<String, Object> vars;

    @BeforeEach
    void setUp() {
        evaluator = new Evaluator("Alice");
        vars = new HashMap<>();
        vars.put("CREATED", "2024-01-15T09:30:00Z");
        vars.put("COMPANY", "Acme Inc.");
        vars.put("SUBJECT", "Hello World");
        vars.put("BODY", "The quick brown fox jumps over the lazy dog");
        vars.put("CATEGORIES", List.of("A", "B", "C", "D", "E"));
    }

    private FormulaContext ctx() { return vars::get; }
    private Object eval(String formula) { return evaluator.evalExpr(formula, ctx()); }

    // ================================================================
    // @Contains
    // ================================================================

    @Nested @DisplayName("@Contains")
    class ContainsTests {
        @Test void found()      { assertEquals(1.0, eval("@Contains(\"Hello World\"; \"World\")")); }
        @Test void notFound()   { assertEquals(0.0, eval("@Contains(\"Hello World\"; \"xyz\")")); }
        @Test void caseSensitive() { assertEquals(0.0, eval("@Contains(\"Hello\"; \"hello\")")); }
        @Test void emptySubstr()   { assertEquals(1.0, eval("@Contains(\"Hello\"; \"\")")); }
        @Test void bothEmpty()     { assertEquals(1.0, eval("@Contains(\"\"; \"\")")); }
        @Test @DisplayName("list in string")
        void listInString() {
            assertEquals(1.0, eval("@Contains(\"Hi There\"; \"Th\" : \"xy\")"));
        }
        @Test @DisplayName("string in list")
        void stringInList() {
            assertEquals(1.0, eval("@Contains(\"Tom\" : \"Dick\" : \"Harry\"; \"Harry\")"));
        }
        @Test @DisplayName("list in list — any element of first contains any element of second")
        void listInList() {
            assertEquals(1.0, eval("@Contains(\"Tom\" : \"Dick\" : \"Harry\"; \"Harry\" : \"Tom\")"));
        }
        @Test @DisplayName("no match between lists")
        void listNoMatch() {
            assertEquals(0.0, eval("@Contains(\"Tom\" : \"Dick\"; \"Jane\" : \"Mary\")"));
        }
    }

    // ================================================================
    // @Begins
    // ================================================================

    @Nested @DisplayName("@Begins")
    class BeginsTests {
        @Test void matches()     { assertEquals(1.0, eval("@Begins(\"Hi There\"; \"Hi\")")); }
        @Test void noMatch()     { assertEquals(0.0, eval("@Begins(\"Hi There\"; \"World\")")); }
        @Test void caseSensitive() { assertEquals(0.0, eval("@Begins(\"Hi There\"; \"hi\")")); }
        @Test @DisplayName("list in second param — any match returns true")
        void listInSecond() {
            assertEquals(1.0, eval("@Begins(\"Luigi Smith\"; \"Luigi\" : \"Florence\" : \"Henri\")"));
        }
        @Test @DisplayName("list in second param — no match")
        void listInSecondNoMatch() {
            assertEquals(0.0, eval("@Begins(\"Mario Smith\"; \"Luigi\" : \"Florence\" : \"Henri\")"));
        }
    }

    // ================================================================
    // @Ends
    // ================================================================

    @Nested @DisplayName("@Ends")
    class EndsTests {
        @Test void matches()     { assertEquals(1.0, eval("@Ends(\"Hi There\"; \"re\")")); }
        @Test void noMatch()     { assertEquals(0.0, eval("@Ends(\"Hi There\"; \"The\")")); }
        @Test void caseSensitive() { assertEquals(0.0, eval("@Ends(\"Hello\"; \"LO\")")); }
        @Test @DisplayName("list in second param — any match returns true")
        void listInSecond() {
            assertEquals(1.0, eval("@Ends(\"Alice Owens\"; \"Owens\" : \"Irons\" : \"Baker\")"));
        }
        @Test @DisplayName("list in second param — no match")
        void listInSecondNoMatch() {
            assertEquals(0.0, eval("@Ends(\"Alice Smith\"; \"Owens\" : \"Irons\" : \"Baker\")"));
        }
    }

    // ================================================================
    // @ReplaceSubstring
    // ================================================================

    @Nested @DisplayName("@ReplaceSubstring")
    class ReplaceSubstringTests {
        @Test @DisplayName("single replacement")
        void simple()      { assertEquals("I hate apples", eval("@ReplaceSubstring(\"I like apples\"; \"like\"; \"hate\")")); }
        @Test @DisplayName("no match returns original")
        void noMatch()     { assertEquals("Hello World", eval("@ReplaceSubstring(\"Hello World\"; \"xyz\"; \"Universe\")")); }
        @Test @DisplayName("multiple from/to pairs")
        void multiplePairs() {
            assertEquals("I hate peaches", eval("@ReplaceSubstring(\"I like apples\"; \"like\" : \"apples\"; \"hate\" : \"peaches\")"));
        }
        @Test @DisplayName("sequential replacement: first→second, then second→third")
        void sequentialReplacement() {
            assertEquals("third", eval("@ReplaceSubstring(\"first\"; \"first\" : \"second\"; \"second\" : \"third\")"));
        }
        @Test @DisplayName("extra fromList items use last toList value")
        void extraFromItems() {
            // "a-b-c" with from ["a", "b", "c"] and to ["x"] → all replaced with "x"
            assertEquals("x-x-x", eval("@ReplaceSubstring(\"a-b-c\"; \"a\" : \"b\" : \"c\"; \"x\")"));
        }
        @Test @DisplayName("source is a list — each element processed")
        void sourceList() {
            Object result = eval("@ReplaceSubstring(\"a\" : \"b\" : \"c\"; \"a\" : \"b\"; \"x\" : \"y\")");
            assertEquals(List.of("x", "y", "c"), result);
        }
        @Test @DisplayName("removal by replacing with empty string")
        void remove()      { assertEquals("Hello ", eval("@ReplaceSubstring(\"Hello World\"; \"World\"; \"\")")); }
    }

    // ================================================================
    // @Month, @Day, @Year
    // ================================================================

    @Nested @DisplayName("@Month / @Day / @Year")
    class DateExtractionTests {

        @Test @DisplayName("@Month from ISO date")
        void monthFromIso() {
            vars.put("D", "2024-01-15T09:30:00Z");
            assertEquals(1.0, eval("@Month(d)"));
        }

        @Test @DisplayName("@Day from ISO date")
        void dayFromIso() {
            vars.put("D", "2024-01-15T09:30:00Z");
            assertEquals(15.0, eval("@Day(d)"));
        }

        @Test @DisplayName("@Year from ISO date")
        void yearFromIso() {
            vars.put("D", "2024-01-15T09:30:00Z");
            assertEquals(2024.0, eval("@Year(d)"));
        }

        @Test @DisplayName("@Month from US date format")
        void monthFromUS() {
            vars.put("D", "11/30/2000 02:39:55 PM");
            assertEquals(11.0, eval("@Month(d)"));
        }

        @Test @DisplayName("@Day from US date format")
        void dayFromUS() {
            vars.put("D", "11/30/2000 02:39:55 PM");
            assertEquals(30.0, eval("@Day(d)"));
        }

        @Test @DisplayName("@Year from US date format")
        void yearFromUS() {
            vars.put("D", "11/30/2000 02:39:55 PM");
            assertEquals(2000.0, eval("@Year(d)"));
        }

        @Test @DisplayName("@Month from @Created")
        void monthFromCreated() {
            assertEquals(1.0, eval("@Month(@Created)"));
        }

        @Test @DisplayName("@Month from string with no date returns 0")
        void monthFromBadString() {
            vars.put("D", "not a date");
            assertEquals(0.0, eval("@Month(d)"));
        }
    }

    // ================================================================
    // @While
    // ================================================================

    @Nested @DisplayName("@While")
    class WhileTests {

        @Test @DisplayName("simple counter loop")
        void simpleCounter() {
            // n := 1; @While(n < 5; n := n + 1; 0)  → n should end at 5
            // Actually @While returns "" — we test the side effect
            // Simpler: evaluate the increment pattern
            Object result = eval("n := 1; @While(n <= 3; n := n + 1; 0); n");
            assertEquals(4.0, result);
        }

        @Test @DisplayName("@While with @Do body")
        void whileWithDo() {
            // Accumulate in a temp variable
            Object result = eval(
                "sum := 0; i := 1; " +
                "@While(i <= 5; @Do(sum := sum + i; i := i + 1); 0); " +
                "sum");
            assertEquals(15.0, result); // 1+2+3+4+5 = 15
        }

        @Test @DisplayName("@While with empty condition returns empty")
        void whileEmpty() {
            Object result = eval("n := 0; @While(n > 10; n := n + 1; 0); n");
            assertEquals(0.0, result);
        }
    }

    // ================================================================
    // @Word
    // ================================================================

    @Nested @DisplayName("@Word")
    class WordTests {
        @Test @DisplayName("space-separated, positive index")
        void spaceSeparated()  {
            assertEquals("Collins,", eval("@Word(\"Larson, Collins, and Jensen\"; \" \"; 2)"));
        }
        @Test @DisplayName("comma separator")
        void commaSeparator()  {
            assertEquals("M.", eval("@Word(\"Larson,James,M.\"; \",\"; 3)"));
        }
        @Test @DisplayName("negative index: -1 = last word")
        void negativeIndex()  {
            assertEquals("Larson", eval("@Word(\"James M. Larson\"; \" \"; -1)"));
        }
        @Test @DisplayName("zero index = first word")
        void zeroIndex()  {
            assertEquals("Hello", eval("@Word(\"Hello World\"; \" \"; 0)"));
        }
        @Test @DisplayName("out of bounds returns empty")
        void outOfBounds() { assertEquals("", eval("@Word(\"a b c\"; \" \"; 10)")); }
        @Test @DisplayName("negative out of bounds")
        void negativeOutOfBounds() { assertEquals("", eval("@Word(\"a b c\"; \" \"; -10)")); }
        @Test @DisplayName("list source returns list of words")
        void listSource() {
            Object result = eval("@Word(\"a b c\" : \"x y z\"; \" \"; 2)");
            assertEquals(List.of("b", "y"), result);
        }
    }

    // ================================================================
    // @Set / @SetField
    // ================================================================

    @Nested @DisplayName("@Set and @SetField")
    class SetTests {

        @Test @DisplayName("@Set assigns temp variable and returns value")
        void setTempVar() {
            Object result = eval("@Set(\"x\"; \"hello\"); x");
            assertEquals("hello", result);
        }

        @Test @DisplayName("@SetField writes to document")
        void setFieldWrites() {
            CachedEvaluationTest.MapFormulaContext mctx =
                    new CachedEvaluationTest.MapFormulaContext(new HashMap<>());
            evaluator.evalExpr("@SetField(\"Subject\"; \"Updated\")", mctx);
            assertEquals("Updated", mctx.get("SUBJECT"));
        }
    }

    // ================================================================
    // @Trim (updated to match spec)
    // ================================================================

    @Nested @DisplayName("@Trim")
    class TrimTests {
        @Test @DisplayName("trailing spaces removed")
        void trailingSpaces() {
            assertEquals("ROBERT SMITH", eval("@Trim(@UpperCase(\"Robert Smith    \"))"));
        }
        @Test @DisplayName("redundant internal spaces collapsed")
        void redundantSpaces() {
            assertEquals("ROBERT SMITH", eval("@UpperCase(@Trim(\"        Robert       Smith\"))"));
        }
        @Test @DisplayName("all spaces returns empty")
        void allSpaces() { assertEquals("", eval("@Trim(\"     \")")); }
        @Test @DisplayName("list: empty elements removed")
        void listTrim() {
            Object result = eval("@Trim(\"Seattle\" : \"   \" : \"  Toronto  \" : \"\" : \"Chile\")");
            assertEquals(List.of("Seattle", "Toronto", "Chile"), result);
        }
        @Test @DisplayName("list: all empty returns empty string")
        void listAllEmpty() { assertEquals("", eval("@Trim(\"   \" : \"\" : \"  \")")); }
    }
}
