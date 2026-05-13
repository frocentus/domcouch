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

    // ================================================================
    // @UpperCase / @LowerCase (list support)
    // ================================================================

    @Nested @DisplayName("@UpperCase / @LowerCase")
    class CaseTests {
        @Test @DisplayName("@UpperCase single string")
        void upperSingle() { assertEquals("ROBERT T. SMITH", eval("@UpperCase(\"Robert T. Smith\")")); }
        @Test @DisplayName("@UpperCase list")
        void upperList() {
            assertEquals(List.of("ROBERT", "SMITH"), eval("@UpperCase(\"Robert\" : \"Smith\")"));
        }
        @Test @DisplayName("@UpperCase field")
        void upperField() { vars.put("STATE", "ma"); assertEquals("MA", eval("@UpperCase(State)")); }
        @Test @DisplayName("@LowerCase single string")
        void lowerSingle() { assertEquals("juan mendoza", eval("@LowerCase(\"Juan Mendoza\")")); }
        @Test @DisplayName("@LowerCase list")
        void lowerList() {
            assertEquals(List.of("juan", "mendoza"), eval("@LowerCase(\"Juan\" : \"Mendoza\")"));
        }
        @Test @DisplayName("@LowerCase field")
        void lowerField() {
            vars.put("FURNITURE", "ARM CHAIR");
            assertEquals("arm chair", eval("@LowerCase(Furniture)"));
        }
    }

    // ================================================================
    // @Length (list support)
    // ================================================================

    @Nested @DisplayName("@Length")
    class LengthTests {
        @Test @DisplayName("single string")
        void singleString() {
            assertEquals(45.0, eval("@Length(\"The boy crossed the wide, but gentle, stream.\")"));
        }
        @Test @DisplayName("empty string returns 0")
        void emptyString() { assertEquals(0.0, eval("@Length(\"\")")); }
        @Test @DisplayName("list returns number list")
        void listReturnsNumberList() {
            assertEquals(List.of(0.0, 5.0, 3.0), eval("@Length(\"\" : \"abcde\" : \"xyz\")"));
        }
    }

    // ================================================================
    // @Left (substring overload + list support)
    // ================================================================

    @Nested @DisplayName("@Left")
    class LeftTests {
        @Test @DisplayName("numeric: leftmost N characters")
        void numericArg() { assertEquals("Len", eval("@Left(\"Lennard Wallace\"; 3)")); }
        @Test @DisplayName("numeric: negative returns whole string")
        void numericNegative() { assertEquals("hello", eval("@Left(\"hello\"; -1)")); }
        @Test @DisplayName("substring: returns characters left of substring")
        void substringArg() { assertEquals("Lennard Wal", eval("@Left(\"Lennard Wallace\"; \"la\")")); }
        @Test @DisplayName("substring not found returns empty")
        void substringNotFound() { assertEquals("", eval("@Left(\"hello\"; \"xyz\")")); }
        @Test @DisplayName("list support")
        void listSupport() {
            assertEquals(List.of("L", "W"), eval("@Left(\"Lennard\" : \"Wallace\"; 1)"));
        }
    }

    // ================================================================
    // @Right (substring overload + list support)
    // ================================================================

    @Nested @DisplayName("@Right")
    class RightTests {
        @Test @DisplayName("numeric: rightmost N characters")
        void numericArg() { assertEquals("ace", eval("@Right(\"Lennard Wallace\"; 3)")); }
        @Test @DisplayName("substring: returns characters right of substring")
        void substringArg() { assertEquals("Wallace", eval("@Right(\"Lennard Wallace\"; \" \")")); }
        @Test @DisplayName("substring not found returns empty")
        void substringNotFound() { assertEquals("", eval("@Right(\"hello\"; \"xyz\")")); }
        @Test @DisplayName("negative returns whole string")
        void negativeArg() { assertEquals("hello", eval("@Right(\"hello\"; -1)")); }
        @Test @DisplayName("list support")
        void listSupport() {
            assertEquals(List.of("ard", "ace"), eval("@Right(\"Lennard\" : \"Wallace\"; 3)"));
        }
    }

    // ================================================================
    // @Repeat (third arg + list support)
    // ================================================================

    @Nested @DisplayName("@Repeat")
    class RepeatTests {
        @Test @DisplayName("basic repeat")
        void basic() { assertEquals("HelloHelloHello", eval("@Repeat(\"Hello\"; 3)")); }
        @Test @DisplayName("with max chars truncation")
        void maxChars() { assertEquals("ByeBy", eval("@Repeat(\"Bye\"; 2; 5)")); }
        @Test @DisplayName("list support")
        void listSupport() {
            assertEquals(List.of("HelloHelloHello", "ByeByeBye"), eval("@Repeat(\"Hello\" : \"Bye\"; 3)"));
        }
    }

    // ================================================================
    // @Matches (pattern matching)
    // ================================================================

    @Nested @DisplayName("@Matches")
    class MatchesTests {
        @Test @DisplayName("? matches any single char")
        void questionMark() { assertEquals(1.0, eval("@Matches(\"abc\"; \"a?c\")")); }
        @Test @DisplayName("? does not match wrong length")
        void questionMarkFail() { assertEquals(0.0, eval("@Matches(\"A big test\"; \"a?test\")")); }
        @Test @DisplayName("multiple ? wildcards")
        void multipleQuestions() { assertEquals(1.0, eval("@Matches(\"A big test\"; \"a?????test\")")); }
        @Test @DisplayName("* matches any string")
        void starWildcard() { assertEquals(1.0, eval("@Matches(\"Vermont\"; \"*mont*\")")); }
        @Test @DisplayName("{ABC} character class")
        void charClass() { assertEquals(1.0, eval("@Matches(\"AB\"; \"{A-C}{A-C}\")")); }
        @Test @DisplayName("case-insensitive simple chars")
        void caseInsensitive() { assertEquals(1.0, eval("@Matches(\"abc\"; \"ABC\")")); }
        @Test @DisplayName("list: any match returns true")
        void listMatch() {
            assertEquals(1.0, eval("@Matches(\"one\" : \"two\" : \"three\"; \"three\" : \"four\": \"five\")"));
        }
        @Test @DisplayName("list: no match returns false")
        void listNoMatch() {
            assertEquals(0.0, eval("@Matches(\"one\" : \"two\" : \"three\"; \"four\" : \"five\" : \"six\")"));
        }
    }

    // ================================================================
    // @Text (format strings)
    // ================================================================

    @Nested @DisplayName("@Text")
    class TextTests {
        @Test @DisplayName("simple conversion")
        void simple() { assertEquals("123.45", eval("@Text(123.45)")); }
        @Test @DisplayName("currency format")
        void currency() { assertEquals("$800.00", eval("@Text(800; \"C,2\")")); }
        @Test @DisplayName("scientific format")
        void scientific() { assertEquals("8.00E+02", eval("@Text(800; \"S\")")); }
        @Test @DisplayName("list with format")
        void listFormat() {
            Object result = eval("@Text(800 : (-600); \"S\")");
            assertEquals(List.of("8.00E+02", "-6.00E+02"), result);
        }
    }
}
