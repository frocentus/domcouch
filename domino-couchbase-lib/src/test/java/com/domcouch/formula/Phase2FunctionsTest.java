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

    // ================================================================
    // @TextToNumber (leading numeric extraction + list support)
    // ================================================================

    @Nested @DisplayName("@TextToNumber")
    class TextToNumberTests {
        @Test @DisplayName("simple integer")
        void simple() { assertEquals(123.0, eval("@TextToNumber(\"123\")")); }
        @Test @DisplayName("leading numeric: 12ABC → 12")
        void leadingNumeric() { assertEquals(12.0, eval("@TextToNumber(\"12ABC\")")); }
        @Test @DisplayName("non-numeric start: ABC12 → 0")
        void nonNumericStart() { assertEquals(0.0, eval("@TextToNumber(\"ABC12\")")); }
        @Test @DisplayName("list support")
        void listSupport() {
            assertEquals(List.of(123.0, 456.0), eval("@TextToNumber(\"123\" : \"456\")"));
        }
        @Test @DisplayName("negative number")
        void negative() { assertEquals(-42.0, eval("@TextToNumber(\"-42\")")); }
    }

    // ================================================================
    // @IsNumber (spec-correct: strings are NOT numbers)
    // ================================================================

    @Nested @DisplayName("@IsNumber")
    class IsNumberTests {
        @Test @DisplayName("actual number returns true")
        void number() { assertEquals(1.0, eval("@IsNumber(123)")); }
        @Test @DisplayName("string returns false")
        void stringNotNumber() { assertEquals(0.0, eval("@IsNumber(\"123\")")); }
        @Test @DisplayName("date returns false")
        void dateNotNumber() { assertEquals(0.0, eval("@IsNumber(@Created)")); }
        @Test @DisplayName("number list returns true")
        void numberList() { assertEquals(1.0, eval("@IsNumber(-345 : 2.78 : 997 : .7)")); }
        @Test @DisplayName("mixed list returns false")
        void mixedList() { assertEquals(0.0, eval("@IsNumber(1 : \"two\")")); }
    }

    // ================================================================
    // @IsMember (both-lists = subset check)
    // ================================================================

    @Nested @DisplayName("@IsMember")
    class IsMemberTests {
        @Test @DisplayName("single value in list")
        void singleInList() {
            assertEquals(1.0, eval("@IsMember(\"computer\"; \"printer\" : \"computer\" : \"monitor\")"));
        }
        @Test @DisplayName("both lists: ALL of first must be in second")
        void bothListsSubset() {
            assertEquals(0.0, eval("@IsMember(\"computer\" : \"Notes\"; \"Notes\" : \"printer\" : \"monitor\")"));
        }
        @Test @DisplayName("single value is subset")
        void singleSubset() {
            assertEquals(1.0, eval("@IsMember(\"Fred\"; \"Barney\" : \"Wilma\" : \"Fred\")"));
        }
        @Test @DisplayName("field reference")
        void fieldRef() {
            vars.put("DEPARTMENT", List.of("R&D", "Sales"));
            assertEquals(1.0, eval("@IsMember(\"R&D\"; Department)"));
        }
    }

    // ================================================================
    // @Replace (list-level element replacement)
    // ================================================================

    @Nested @DisplayName("@Replace")
    class ReplaceTests {
        @Test @DisplayName("element replacement in list")
        void elementReplace() {
            Object result = eval("@Replace(\"Red\" : \"Orange\" : \"Yellow\" : \"Green\"; \"Orange\" : \"Blue\"; \"Black\" : \"Brown\")");
            assertEquals(List.of("Red", "Black", "Yellow", "Green"), result);
        }
        @Test @DisplayName("no match returns original")
        void noMatch() {
            Object result = eval("@Replace(\"A\" : \"B\" : \"C\"; \"X\"; \"Y\")");
            assertEquals(List.of("A", "B", "C"), result);
        }
    }

    // ================================================================
    // @Abs
    // ================================================================

    @Nested @DisplayName("@Abs")
    class AbsTests {
        @Test @DisplayName("negative number")
        void negative() { assertEquals(2.16, eval("@Abs(-2.16)")); }
        @Test @DisplayName("positive number")
        void positive() { assertEquals(2.16, eval("@Abs(2.16)")); }
        @Test @DisplayName("list")
        void list() {
            assertEquals(List.of(2.15, 2.16), eval("@Abs(2.15 : (-2.16))"));
        }
        @Test @DisplayName("field value")
        void field() { vars.put("NET", -5.0); assertEquals(5.0, eval("@Abs(Net)")); }
    }

    // ================================================================
    // @Adjust
    // ================================================================

    @Nested @DisplayName("@Adjust")
    class AdjustTests {
        @Test @DisplayName("adjust years, months, days")
        void basic() {
            // [06/30/95] + 2y 2m 2d → 09/02/97
            String result = (String) eval("@Adjust([06/30/95]; 2; 2; 2; 0; 0; 0)");
            assertTrue(result.contains("09/02") || result.contains("9/2"));
        }
        @Test @DisplayName("negative adjustment")
        void negative() {
            String result = (String) eval("@Adjust([03/30/96]; -2; 0; -10; 0; 0; 0)");
            assertTrue(result.contains("03/20") || result.contains("3/20"));
        }
    }

    // ================================================================
    // @Explode
    // ================================================================
    @Nested @DisplayName("@Explode")
    class ExplodeTests {
        @Test @DisplayName("comma-separated")
        void commaSeparated() {
            assertEquals(List.of("a", "b", "c"), eval("@Explode(\"a,b,c\")"));
        }
        @Test @DisplayName("default separators space-comma-semicolon")
        void defaultSeparators() {
            assertEquals(List.of("Weekly", "Status", "Report"), eval("@Explode(\"Weekly Status Report\")"));
        }
        @Test @DisplayName("custom separator")
        void customSep() {
            assertEquals(List.of("Please send resume ", " references"), eval("@Explode(\"Please send resume & references\"; \"&\")"));
        }
        @Test @DisplayName("single element")
        void singleElement() {
            assertEquals("hello", eval("@Explode(\"hello\")"));
        }
    }

    // ================================================================
    // @Compare
    // ================================================================
    @Nested @DisplayName("@Compare")
    class CompareTests {
        @Test @DisplayName("equal strings")
        void equalStrings() { assertEquals(0.0, eval("@Compare(\"abc\"; \"abc\")")); }
        @Test @DisplayName("less than")
        void lessThan() { assertEquals(-1.0, eval("@Compare(\"a\"; \"b\")")); }
        @Test @DisplayName("greater than")
        void greaterThan() { assertEquals(1.0, eval("@Compare(\"b\"; \"a\")")); }
    }

    // ================================================================
    // @Count
    // ================================================================
    @Nested @DisplayName("@Count")
    class CountTests {
        @Test @DisplayName("list count")
        void listCount() { assertEquals(3.0, eval("@Count(\"a\":\"b\":\"c\")")); }
        @Test @DisplayName("scalar returns 1")
        void scalar() { assertEquals(1.0, eval("@Count(\"hello\")")); }
        @Test @DisplayName("null returns 1")
        void nullStr() { assertEquals(1.0, eval("@Count(\"\")")); }
    }

    // ================================================================
    // @Date
    // ================================================================
    @Nested @DisplayName("@Date")
    class DateTests {
        @Test @DisplayName("year month day constructor")
        void ymd() { assertTrue(((String)eval("@Date(1995; 6; 23)")).contains("1995")); }
        @Test @DisplayName("full constructor")
        void full() { assertTrue(((String)eval("@Date(1993; 1; 20; 8; 58; 12)")).contains("08")); }
        @Test @DisplayName("date from string")
        void fromString() { assertNotNull(eval("@Date(\"11/20/95\")")); }
    }

    // ================================================================
    // @DocFields / @DocLength / @DocLock / @DocumentUniqueID
    // ================================================================
    @Nested @DisplayName("@DocFields")
    class DocFieldsTests {
        @Test @DisplayName("returns list")
        void returnsList() {
            Object r = eval("@DocFields");
            assertTrue(r instanceof List);
        }
    }
    @Nested @DisplayName("@DocLength")
    class DocLengthTests {
        @Test @DisplayName("returns number")
        void returnsNumber() { assertEquals(0.0, eval("@DocLength")); }
    }
    @Nested @DisplayName("@DocLock")
    class DocLockTests {
        @Test @DisplayName("LOCKINGENABLED returns 0")
        void lockingEnabled() { assertEquals(0.0, eval("@DocLock([LOCKINGENABLED])")); }
        @Test @DisplayName("STATUS returns empty")
        void status() { assertEquals("", eval("@DocLock([STATUS])")); }
        @Test @DisplayName("LOCK returns 1")
        void lock() { assertEquals(1.0, eval("@DocLock([LOCK])")); }
    }
    @Nested @DisplayName("@DocumentUniqueID")
    class DocumentUniqueIdTests {
        @Test @DisplayName("returns string")
        void returnsUnid() {
            assertEquals("", eval("@DocumentUniqueID")); // default context returns empty
        }
    }

    // ================================================================
    // @DoWhile
    // ================================================================
    @Nested @DisplayName("@DoWhile")
    class DoWhileTests {
        @Test @DisplayName("executes body then checks condition")
        void doWhile() {
            assertEquals(1.0, eval("x := 0; @DoWhile(x := x + 1; x < 3)"));
        }
    }

    // ================================================================
    // @Error / @IsError
    // ================================================================
    @Nested @DisplayName("@Error/@IsError")
    class ErrorTests {
        @Test @DisplayName("@Error is detectable by @IsError")
        void isErrorDetects() { assertEquals(1.0, eval("@IsError(@Error)")); }
        @Test @DisplayName("non-error is not error")
        void notError() { assertEquals(0.0, eval("@IsError(42)")); }
    }

    // ================================================================
    // @Eval
    // ================================================================
    @Nested @DisplayName("@Eval")
    class EvalTests {
        @Test @DisplayName("basic meta-evaluation")
        void basic() { assertEquals(3.0, eval("@Eval(\"1 + 2\")")); }
        @Test @DisplayName("assignment in eval")
        void assignInEval() { assertEquals("rebar", eval("@Eval(\"x := \\\"re\\\"; x + \\\"bar\\\"\")")); }
    }

    // ================================================================
    // Math: @Pi, @Power, @Sqrt, @Exp, @Log, @Cos, @Sin, @Tan
    // ================================================================
    @Nested @DisplayName("Math functions")
    class MathTests {
        @Test @DisplayName("@Pi")
        void pi() { assertEquals(Math.PI, (Double) eval("@Pi"), 0.0001); }
        @Test @DisplayName("@Power")
        void power() { assertEquals(8.0, eval("@Power(2; 3)")); }
        @Test @DisplayName("@Sqrt")
        void sqrt() { assertEquals(3.0, eval("@Sqrt(9)")); }
        @Test @DisplayName("@Exp")
        void exp() { assertEquals(Math.exp(1.25), (Double) eval("@Exp(1.25)"), 0.0001); }
        @Test @DisplayName("@Log")
        void log() { assertEquals(0.0, eval("@Log(1)")); }
        @Test @DisplayName("@Cos")
        void cos() { assertEquals(1.0, (Double) eval("@Cos(2 * @Pi)"), 0.0001); }
        @Test @DisplayName("@Sin")
        void sin() { assertEquals(Math.sin(0.5), (Double) eval("@Sin(0.5)"), 0.0001); }
        @Test @DisplayName("@Tan")
        void tan() { assertEquals(Math.tan(0.5), (Double) eval("@Tan(0.5)"), 0.0001); }
        @Test @DisplayName("@Integer")
        void integer() { assertEquals(3.0, eval("@Integer(3.7)")); }
        @Test @DisplayName("@Round")
        void round() { assertEquals(4.0, eval("@Round(3.7)")); }
    }

    // ================================================================
    // @ATan / @ATan2 / @ASin / @ACos
    // ================================================================
    @Nested @DisplayName("Arc trig")
    class ArcTrigTests {
        @Test @DisplayName("@ATan")
        void atan() { assertEquals(Math.atan(1.0), (Double) eval("@ATan(1)"), 0.0001); }
        @Test @DisplayName("@ATan2")
        void atan2() { assertEquals(Math.atan2(1.0, 1.0), (Double) eval("@ATan2(1; 1)"), 0.0001); }
        @Test @DisplayName("@ASin")
        void asin() { assertEquals(Math.asin(0.5), (Double) eval("@ASin(0.5)"), 0.0001); }
        @Test @DisplayName("@ACos")
        void acos() { assertEquals(Math.acos(0.5), (Double) eval("@ACos(0.5)"), 0.0001); }
    }

    // ================================================================
    // @Ascii / @Char
    // ================================================================
    @Nested @DisplayName("@Ascii/@Char")
    class AsciiCharTests {
        @Test @DisplayName("@Ascii basic")
        void ascii() { assertEquals("A", eval("@Ascii(\"A\")")); }
        @Test @DisplayName("@Char basic")
        void charFn() { assertEquals("A", eval("@Char(65)")); }
    }

    // ================================================================
    // @CheckFormulaSyntax
    // ================================================================
    @Nested @DisplayName("@CheckFormulaSyntax")
    class CheckFormulaSyntaxTests {
        @Test @DisplayName("valid formula")
        void valid() { assertEquals("1", eval("@CheckFormulaSyntax(\"1 + 2\")")); }
        @Test @DisplayName("invalid formula")
        void invalid() {
            Object r = eval("@CheckFormulaSyntax(\"@Foo(\")");
            assertTrue(r instanceof List);
        }
    }

    // ================================================================
    // @DeleteField
    // ================================================================
    @Nested @DisplayName("@DeleteField")
    class DeleteFieldDirectTests {
        @Test @DisplayName("function exists and evaluates")
        void exists() {
            // @DeleteField returns an Expr.DeleteField node that is a side-effect marker.
            // In a simple context without setField capability, it still evaluates.
            Object r = eval("@IsError(@DeleteField(\"Field\"))");
            assertEquals(0.0, r);
        }
    }

    // ================================================================
    // @Accessed / @Modified / @AddedToThisFile
    // ================================================================
    @Nested @DisplayName("Document timestamps")
    class DocTimestampsTests {
        @Test @DisplayName("@Created resolves")
        void created() { eval("@Created"); } // runs without error
        @Test @DisplayName("@Modified resolves")
        void modified() { eval("@Modified"); }
        @Test @DisplayName("@Accessed resolves")
        void accessed() { eval("@Accessed"); }
        @Test @DisplayName("@AddedToThisFile resolves")
        void added() { eval("@AddedToThisFile"); }
    }

    // ================================================================
    // @Today / @Now
    // ================================================================
    @Nested @DisplayName("@Today/@Now")
    class TodayNowTests {
        @Test @DisplayName("@Today returns date")
        void today() { assertNotNull(eval("@Today")); }
        @Test @DisplayName("@Now returns datetime")
        void now() { assertNotNull(eval("@Now")); }
    }

    // ================================================================
    // @BusinessDays
    // ================================================================
    @Nested @DisplayName("@BusinessDays")
    class BusinessDaysTests {
        @Test @DisplayName("one business day")
        void oneDay() { assertTrue(((Double) eval("@BusinessDays([06/30/95]; [07/01/95])")) >= 0); }
    }
}
