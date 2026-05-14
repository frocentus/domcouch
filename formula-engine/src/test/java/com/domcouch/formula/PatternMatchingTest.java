package com.domcouch.formula;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pattern Matching")
class PatternMatchingTest extends BaseFormulaTest {

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
        @Test @DisplayName("| OR") void orFirst() { assertEquals(1.0, eval("@Matches(\"Central\"; \"Central | Midwest\")")); }
        @Test @DisplayName("| OR second") void orSecond() { assertEquals(1.0, eval("@Matches(\"Midwest\"; \"Central | Midwest\")")); }
        @Test @DisplayName("| OR no match") void orNoMatch() { assertEquals(0.0, eval("@Matches(\"East\"; \"Central | Midwest\")")); }
        @Test @DisplayName("& AND") void andBoth() { assertEquals(1.0, eval("@Matches(\"abc123\"; \"abc & 123\")")); }
        @Test @DisplayName("& AND only first") void andFirstOnly() { assertEquals(0.0, eval("@Matches(\"abc\"; \"abc & 123\")")); }
        @Test @DisplayName("!A matches any except A") void notLiteral() { assertEquals(1.0, eval("@Matches(\"B\"; \"!A\")")); }
        @Test @DisplayName("!A rejects A") void notLiteralFail() { assertEquals(0.0, eval("@Matches(\"A\"; \"!A\")")); }
        @Test @DisplayName("!{A-F} outside range") void notSet() { assertEquals(1.0, eval("@Matches(\"Z\"; \"!{A-F}\")")); }
    }

    @Nested @DisplayName("@Like")
    class LikeTests {
        @Test @DisplayName("underscore wildcard") void underscore() { assertEquals(1.0, eval("@Like(\"big\"; \"b_g\")")); }
        @Test @DisplayName("case insensitive") void caseIns() { assertEquals(1.0, eval("@Like(\"A big test\"; \"A BIG TEST\")")); }
        @Test @DisplayName("percent wildcard") void percent() { assertEquals(1.0, eval("@Like(\"A big test\"; \"%test\")")); }
        @Test @DisplayName("no match") void noMatch() { assertEquals(0.0, eval("@Like(\"A big test\"; \"xyz\")")); }
        @Test @DisplayName("escape char: | makes % literal") void escapePercent() {
            // Without escape, % is wildcard (both match). With |, % is literal.
            // @Like("50% off"; "50|% off"; "|") → 1 (literal % matches literal %)
            assertEquals(1.0, eval("@Like(\"50% off\"; \"50|% off\"; \"|\")"));
        }
        @Test @DisplayName("escape char: | makes _ literal") void escapeUnderscore() {
            // Without escape, _ is any char (matches aXb). With |, _ is literal.
            // @Like("aXb"; "a|_b"; "|") → 0 (literal _ doesn't match X)
            assertEquals(0.0, eval("@Like(\"aXb\"; \"a|_b\"; \"|\")"));
        }
    }
}
