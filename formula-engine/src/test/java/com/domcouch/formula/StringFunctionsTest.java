package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("String Functions")
class StringFunctionsTest extends BaseFormulaTest {

    @Nested @DisplayName("@Contains")
    class ContainsTests {
        @Test void found()      { assertEquals(1.0, eval("@Contains(\"Hello World\"; \"World\")")); }
        @Test void notFound()   { assertEquals(0.0, eval("@Contains(\"Hello World\"; \"xyz\")")); }
        @Test void caseSensitive() { assertEquals(0.0, eval("@Contains(\"Hello\"; \"hello\")")); }
        @Test void emptySubstr()   { assertEquals(1.0, eval("@Contains(\"Hello\"; \"\")")); }
        @Test void bothEmpty()     { assertEquals(1.0, eval("@Contains(\"\"; \"\")")); }
        @Test @DisplayName("list in string") void listInString() {
            assertEquals(1.0, eval("@Contains(\"Hi There\"; \"Th\" : \"xy\")")); }
        @Test @DisplayName("string in list") void stringInList() {
            assertEquals(1.0, eval("@Contains(\"Tom\" : \"Dick\" : \"Harry\"; \"Harry\")")); }
        @Test @DisplayName("list in list") void listInList() {
            assertEquals(1.0, eval("@Contains(\"Tom\" : \"Dick\" : \"Harry\"; \"Harry\" : \"Tom\")")); }
        @Test @DisplayName("no match between lists") void listNoMatch() {
            assertEquals(0.0, eval("@Contains(\"Tom\" : \"Dick\"; \"Jane\" : \"Mary\")")); }
    }

    @Nested @DisplayName("@Begins")
    class BeginsTests {
        @Test void matches()     { assertEquals(1.0, eval("@Begins(\"Hi There\"; \"Hi\")")); }
        @Test void noMatch()     { assertEquals(0.0, eval("@Begins(\"Hi There\"; \"World\")")); }
        @Test void caseSensitive() { assertEquals(0.0, eval("@Begins(\"Hi There\"; \"hi\")")); }
        @Test @DisplayName("list in second param") void listInSecond() {
            assertEquals(1.0, eval("@Begins(\"Luigi Smith\"; \"Luigi\" : \"Florence\" : \"Henri\")")); }
        @Test @DisplayName("list no match") void listInSecondNoMatch() {
            assertEquals(0.0, eval("@Begins(\"Mario Smith\"; \"Luigi\" : \"Florence\" : \"Henri\")")); }
    }

    @Nested @DisplayName("@Ends")
    class EndsTests {
        @Test void matches()     { assertEquals(1.0, eval("@Ends(\"Hi There\"; \"re\")")); }
        @Test void noMatch()     { assertEquals(0.0, eval("@Ends(\"Hi There\"; \"The\")")); }
        @Test void caseSensitive() { assertEquals(0.0, eval("@Ends(\"Hello\"; \"LO\")")); }
        @Test @DisplayName("list in second param") void listInSecond() {
            assertEquals(1.0, eval("@Ends(\"Alice Owens\"; \"Owens\" : \"Irons\" : \"Baker\")")); }
        @Test @DisplayName("list no match") void listInSecondNoMatch() {
            assertEquals(0.0, eval("@Ends(\"Alice Smith\"; \"Owens\" : \"Irons\" : \"Baker\")")); }
    }

    @Nested @DisplayName("@ReplaceSubstring")
    class ReplaceSubstringTests {
        @Test @DisplayName("single replacement") void simple() {
            assertEquals("I hate apples", eval("@ReplaceSubstring(\"I like apples\"; \"like\"; \"hate\")")); }
        @Test @DisplayName("no match") void noMatch() {
            assertEquals("Hello World", eval("@ReplaceSubstring(\"Hello World\"; \"xyz\"; \"Universe\")")); }
        @Test @DisplayName("multiple pairs") void multiplePairs() {
            assertEquals("I hate peaches", eval("@ReplaceSubstring(\"I like apples\"; \"like\" : \"apples\"; \"hate\" : \"peaches\")")); }
        @Test @DisplayName("sequential replacement") void sequentialReplacement() {
            assertEquals("third", eval("@ReplaceSubstring(\"first\"; \"first\" : \"second\"; \"second\" : \"third\")")); }
        @Test @DisplayName("extra fromList items use last toList value") void extraFromItems() {
            assertEquals("x-x-x", eval("@ReplaceSubstring(\"a-b-c\"; \"a\" : \"b\" : \"c\"; \"x\")")); }
        @Test @DisplayName("source list") void sourceList() {
            assertEquals(List.of("x", "y", "c"), eval("@ReplaceSubstring(\"a\" : \"b\" : \"c\"; \"a\" : \"b\"; \"x\" : \"y\")")); }
        @Test @DisplayName("removal") void remove() {
            assertEquals("Hello ", eval("@ReplaceSubstring(\"Hello World\"; \"World\"; \"\")")); }
    }

    @Nested @DisplayName("@Word")
    class WordTests {
        @Test @DisplayName("space-separated") void spaceSeparated() {
            assertEquals("Collins,", eval("@Word(\"Larson, Collins, and Jensen\"; \" \"; 2)")); }
        @Test @DisplayName("comma separator") void commaSeparator() {
            assertEquals("M.", eval("@Word(\"Larson,James,M.\"; \",\"; 3)")); }
        @Test @DisplayName("negative index") void negativeIndex() {
            assertEquals("Larson", eval("@Word(\"James M. Larson\"; \" \"; -1)")); }
        @Test @DisplayName("zero index") void zeroIndex() { assertEquals("Hello", eval("@Word(\"Hello World\"; \" \"; 0)")); }
        @Test @DisplayName("out of bounds") void outOfBounds() { assertEquals("", eval("@Word(\"a b c\"; \" \"; 10)")); }
        @Test @DisplayName("negative out of bounds") void negativeOutOfBounds() { assertEquals("", eval("@Word(\"a b c\"; \" \"; -10)")); }
        @Test @DisplayName("list source") void listSource() {
            assertEquals(List.of("b", "y"), eval("@Word(\"a b c\" : \"x y z\"; \" \"; 2)")); }
    }

    @Nested @DisplayName("@Trim")
    class TrimTests {
        @Test @DisplayName("trailing spaces") void trailingSpaces() {
            assertEquals("ROBERT SMITH", eval("@Trim(@UpperCase(\"Robert Smith    \"))")); }
        @Test @DisplayName("redundant spaces") void redundantSpaces() {
            assertEquals("ROBERT SMITH", eval("@UpperCase(@Trim(\"        Robert       Smith\"))")); }
        @Test @DisplayName("all spaces") void allSpaces() { assertEquals("", eval("@Trim(\"     \")")); }
        @Test @DisplayName("list trim") void listTrim() {
            assertEquals(List.of("Seattle", "Toronto", "Chile"), eval("@Trim(\"Seattle\" : \"   \" : \"  Toronto  \" : \"\" : \"Chile\")")); }
        @Test @DisplayName("list all empty") void listAllEmpty() { assertEquals("", eval("@Trim(\"   \" : \"\" : \"  \")")); }
    }

    @Nested @DisplayName("@UpperCase / @LowerCase")
    class CaseTests {
        @Test @DisplayName("@UpperCase single") void upperSingle() { assertEquals("ROBERT T. SMITH", eval("@UpperCase(\"Robert T. Smith\")")); }
        @Test @DisplayName("@UpperCase list") void upperList() { assertEquals(List.of("ROBERT", "SMITH"), eval("@UpperCase(\"Robert\" : \"Smith\")")); }
        @Test @DisplayName("@UpperCase field") void upperField() { vars.put("STATE", "ma"); assertEquals("MA", eval("@UpperCase(State)")); }
        @Test @DisplayName("@LowerCase single") void lowerSingle() { assertEquals("juan mendoza", eval("@LowerCase(\"Juan Mendoza\")")); }
        @Test @DisplayName("@LowerCase list") void lowerList() { assertEquals(List.of("juan", "mendoza"), eval("@LowerCase(\"Juan\" : \"Mendoza\")")); }
        @Test @DisplayName("@LowerCase field") void lowerField() { vars.put("FURNITURE", "ARM CHAIR"); assertEquals("arm chair", eval("@LowerCase(Furniture)")); }
    }

    @Nested @DisplayName("@Length")
    class LengthTests {
        @Test @DisplayName("single string") void singleString() {
            assertEquals(45.0, eval("@Length(\"The boy crossed the wide, but gentle, stream.\")")); }
        @Test @DisplayName("empty string") void emptyString() { assertEquals(0.0, eval("@Length(\"\")")); }
        @Test @DisplayName("list") void listReturnsNumberList() { assertEquals(List.of(0.0, 5.0, 3.0), eval("@Length(\"\" : \"abcde\" : \"xyz\")")); }
    }

    @Nested @DisplayName("@Left")
    class LeftTests {
        @Test @DisplayName("numeric N chars") void numericArg() { assertEquals("Len", eval("@Left(\"Lennard Wallace\"; 3)")); }
        @Test @DisplayName("negative returns whole") void numericNegative() { assertEquals("hello", eval("@Left(\"hello\"; -1)")); }
        @Test @DisplayName("substring") void substringArg() { assertEquals("Lennard Wal", eval("@Left(\"Lennard Wallace\"; \"la\")")); }
        @Test @DisplayName("substring not found") void substringNotFound() { assertEquals("", eval("@Left(\"hello\"; \"xyz\")")); }
        @Test @DisplayName("list") void listSupport() { assertEquals(List.of("L", "W"), eval("@Left(\"Lennard\" : \"Wallace\"; 1)")); }
    }

    @Nested @DisplayName("@Right")
    class RightTests {
        @Test @DisplayName("numeric N chars") void numericArg() { assertEquals("ace", eval("@Right(\"Lennard Wallace\"; 3)")); }
        @Test @DisplayName("substring") void substringArg() { assertEquals("Wallace", eval("@Right(\"Lennard Wallace\"; \" \")")); }
        @Test @DisplayName("substring not found") void substringNotFound() { assertEquals("", eval("@Right(\"hello\"; \"xyz\")")); }
        @Test @DisplayName("negative returns whole") void negativeArg() { assertEquals("hello", eval("@Right(\"hello\"; -1)")); }
        @Test @DisplayName("list") void listSupport() { assertEquals(List.of("ard", "ace"), eval("@Right(\"Lennard\" : \"Wallace\"; 3)")); }
    }

    @Nested @DisplayName("@Repeat")
    class RepeatTests {
        @Test @DisplayName("basic") void basic() { assertEquals("HelloHelloHello", eval("@Repeat(\"Hello\"; 3)")); }
        @Test @DisplayName("with truncation") void maxChars() { assertEquals("ByeBy", eval("@Repeat(\"Bye\"; 2; 5)")); }
        @Test @DisplayName("list") void listSupport() { assertEquals(List.of("HelloHelloHello", "ByeByeBye"), eval("@Repeat(\"Hello\" : \"Bye\"; 3)")); }
    }

    @Nested @DisplayName("@Matches")
    class MatchesTests {
        @Test @DisplayName("? matches any single char") void questionMark() { assertEquals(1.0, eval("@Matches(\"abc\"; \"a?c\")")); }
        @Test @DisplayName("? does not match wrong length") void questionMarkFail() { assertEquals(0.0, eval("@Matches(\"A big test\"; \"a?test\")")); }
        @Test @DisplayName("multiple ?") void multipleQuestions() { assertEquals(1.0, eval("@Matches(\"A big test\"; \"a?????test\")")); }
        @Test @DisplayName("* matches any string") void starWildcard() { assertEquals(1.0, eval("@Matches(\"Vermont\"; \"*mont*\")")); }
        @Test @DisplayName("{ABC} character class") void charClass() { assertEquals(1.0, eval("@Matches(\"AB\"; \"{A-C}{A-C}\")")); }
        @Test @DisplayName("case-insensitive") void caseInsensitive() { assertEquals(1.0, eval("@Matches(\"abc\"; \"ABC\")")); }
        @Test @DisplayName("list any match") void listMatch() { assertEquals(1.0, eval("@Matches(\"one\" : \"two\" : \"three\"; \"three\" : \"four\": \"five\")")); }
        @Test @DisplayName("list no match") void listNoMatch() { assertEquals(0.0, eval("@Matches(\"one\" : \"two\" : \"three\"; \"four\" : \"five\" : \"six\")")); }
        @Test @DisplayName("+? matches any string") void plusAny() { assertEquals(1.0, eval("@Matches(\"anything\"; \"+?\")")); }
        @Test @DisplayName("+? matches empty") void plusAnyEmpty() { assertEquals(1.0, eval("@Matches(\"\"; \"+?\")")); }
        @Test @DisplayName("+A zero-or-more") void plusLiteral() { assertEquals(1.0, eval("@Matches(\"AAA\"; \"+A\")")); }
        @Test @DisplayName("+A empty") void plusLiteralEmpty() { assertEquals(1.0, eval("@Matches(\"\"; \"+A\")")); }
        @Test @DisplayName("+{A-F} set") void plusSet() { assertEquals(1.0, eval("@Matches(\"ABC\"; \"+{A-F}\")")); }
        @Test @DisplayName("| OR first") void orFirst() { assertEquals(1.0, eval("@Matches(\"Central\"; \"Central | Midwest\")")); }
        @Test @DisplayName("| OR second") void orSecond() { assertEquals(1.0, eval("@Matches(\"Midwest\"; \"Central | Midwest\")")); }
        @Test @DisplayName("| OR no match") void orNoMatch() { assertEquals(0.0, eval("@Matches(\"East\"; \"Central | Midwest\")")); }
        @Test @DisplayName("| with wildcards") void orWithWildcards() { assertEquals(1.0, eval("@Matches(\"prefixABC\"; \"A* | *BC\")")); }
        @Test @DisplayName("& AND both match") void andBoth() { assertEquals(1.0, eval("@Matches(\"abc123\"; \"abc & 123\")")); }
        @Test @DisplayName("& AND first only") void andFirstOnly() { assertEquals(0.0, eval("@Matches(\"abc\"; \"abc & 123\")")); }
        @Test @DisplayName("& AND neither") void andNeither() { assertEquals(0.0, eval("@Matches(\"xyz\"; \"abc & 123\")")); }
        @Test @DisplayName("!A matches any except A") void notLiteral() { assertEquals(1.0, eval("@Matches(\"B\"; \"!A\")")); }
        @Test @DisplayName("!A rejects A") void notLiteralFail() { assertEquals(0.0, eval("@Matches(\"A\"; \"!A\")")); }
        @Test @DisplayName("!{A-F} outside range") void notSet() { assertEquals(1.0, eval("@Matches(\"Z\"; \"!{A-F}\")")); }
        @Test @DisplayName("!? matches nothing") void notAny() { assertEquals(0.0, eval("@Matches(\"A\"; \"!?\")")); }
    }

    @Nested @DisplayName("@Explode")
    class ExplodeTests {
        @Test @DisplayName("comma-separated") void commaSeparated() { assertEquals(List.of("a", "b", "c"), eval("@Explode(\"a,b,c\")")); }
        @Test @DisplayName("default separators") void defaultSeparators() { assertEquals(List.of("Weekly", "Status", "Report"), eval("@Explode(\"Weekly Status Report\")")); }
        @Test @DisplayName("custom separator") void customSep() { assertEquals(List.of("Please send resume ", " references"), eval("@Explode(\"Please send resume & references\"; \"&\")")); }
        @Test @DisplayName("single element") void singleElement() { assertEquals("hello", eval("@Explode(\"hello\")")); }
    }

    @Nested @DisplayName("@LeftBack @RightBack")
    class BackSubstringTests {
        @Test @DisplayName("@LeftBack char count") void leftBackChars() { assertEquals("Lennard Wall", eval("@LeftBack(\"Lennard Wallace\"; 3)")); }
        @Test @DisplayName("@LeftBack separator excludes separator") void leftBackSep() { assertEquals("Lennard", eval("@LeftBack(\"Lennard Wallace\"; \" \")")); }
        @Test @DisplayName("@LeftBack separator not found returns whole") void leftBackSepNotFound() { assertEquals("Lennard Wallace", eval("@LeftBack(\"Lennard Wallace\"; \"x\")")); }
        @Test @DisplayName("@LeftBack list") void leftBackList() { assertEquals(java.util.List.of("Lenn", "Wall"), eval("@LeftBack(\"Lennard\" : \"Wallace\"; 3)")); }
        @Test @DisplayName("@RightBack char count") void rightBackChars() { assertEquals("ace", eval("@RightBack(\"Lennard Wallace\"; 3)")); }
        @Test @DisplayName("@RightBack separator uses lastIndexOf") void rightBackSep() {
            assertEquals("c", eval("@RightBack(\"a.b.c\"; \".\")"));
        }
        @Test @DisplayName("@RightBack separator single occurrence") void rightBackSepSingle() { assertEquals("Wallace", eval("@RightBack(\"Lennard Wallace\"; \" \")")); }
        @Test @DisplayName("@RightBack separator not found returns whole") void rightBackSepNotFound() { assertEquals("Lennard Wallace", eval("@RightBack(\"Lennard Wallace\"; \"x\")")); }
    }

    @Nested @DisplayName("@Middle / @MiddleBack")
    class MiddleTests {
        // @Middle spec examples (scan from left)
        @Test @DisplayName("@Middle offset+numberchars") void midOffN() {
            assertEquals("h C", eval("@Middle(\"North Carolina\"; 4; 3)"));
        }
        @Test @DisplayName("@Middle offset+neg numberchars") void midOffNegN() {
            assertEquals("ort", eval("@Middle(\"North Carolina\"; 4; -3)"));
        }
        @Test @DisplayName("@Middle substring+numberchars") void midSubN() {
            assertEquals("Car", eval("@Middle(\"North Carolina\"; \" \"; 3)"));
        }
        @Test @DisplayName("@Middle substring+neg numberchars") void midSubNegN() {
            assertEquals("or", eval("@Middle(\"North Carolina\"; \"th\"; -2)"));
        }
        @Test @DisplayName("@Middle offset+endstring") void midOffSub() {
            assertEquals(" is the ", eval("@Middle(\"This is the text\"; 4; \"text\")"));
        }
        @Test @DisplayName("@Middle substring+endstring") void midSubSub() {
            assertEquals(" the ", eval("@Middle(\"This is the text\"; \" is\"; \"text\")"));
        }

        // @MiddleBack spec examples (scan from right)
        @Test @DisplayName("@MiddleBack offset+numberchars") void midBackOffN() {
            // @MiddleBack(Author; " "; 3) on "Timothy Altman" → "Alt"
            assertEquals("Alt", eval("@MiddleBack(\"Timothy Altman\"; \" \"; 3)"));
        }
        @Test @DisplayName("@MiddleBack substring not found") void midBackSubNotFound() {
            assertEquals("", eval("@MiddleBack(\"Smith\"; \" \"; 3)"));
        }
    }

    @Nested @DisplayName("@ProperCase")
    class ProperCaseTests {
        @Test @DisplayName("spec example") void spec() { assertEquals("Every Child Loves Toys", eval("@ProperCase(\"every CHILD LOves toys\")")); }
        @Test @DisplayName("with digits") void digits() { assertEquals("3-Digit Code", eval("@ProperCase(\"3-digit code\")")); }
        @Test @DisplayName("list") void list() { assertEquals(List.of("Robert","Smith"), eval("@ProperCase(\"ROBERT\" : \"SMITH\")")); }
    }

    @Nested @DisplayName("@Ascii/@Char")
    class AsciiCharTests {
        @Test @DisplayName("@Ascii basic") void ascii() { assertEquals("A", eval("@Ascii(\"A\")")); }
        @Test @DisplayName("@Char basic") void charFn() { assertEquals("A", eval("@Char(65)")); }
    }
}
