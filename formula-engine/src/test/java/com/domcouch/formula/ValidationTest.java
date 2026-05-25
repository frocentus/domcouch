package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validation, Placeholders & Constants")
class ValidationTest extends BaseFormulaTest {

    @Nested @DisplayName("@Success/@Failure")
    class ValidationTests {
        @Test @DisplayName("@Success returns 1") void success() { assertEquals(1.0, eval("@Success")); }
        @Test @DisplayName("@Failure returns error message") void failure() { assertEquals("Invalid", eval("@Failure(\"Invalid\")")); }
        @Test @DisplayName("@Failure with @If (spec example)") void specExample() {
            assertEquals("Area codes have only 3 digits",
                    eval("@If(1234<1000; @Success; @Failure(\"Area codes have only 3 digits\"))")); }
    }

    @Nested @DisplayName("@IsNull @IsValid @IfError")
    class NullValidTests {
        @Test @DisplayName("@IsNull empty") void nullEmpty() { assertEquals(1.0, eval("@IsNull(\"\")")); }
        @Test @DisplayName("@IsNull not null") void notNull() { assertEquals(0.0, eval("@IsNull(\"hello\")")); }
        @Test @DisplayName("@IsValid") void isValid() { assertEquals(1.0, eval("@IsValid")); }
        @Test @DisplayName("@IfError no error") void ifErrorOk() { assertEquals(3.0, eval("@IfError(1 + 2; 42)")); }
        @Test @DisplayName("@IfError catches parse error") void parseError() { assertEquals(42.0, eval("@IfError(@Eval(\"@Foo(\"); 42)")); }
    }

    @Nested @DisplayName("Quick-win placeholders")
    class PlaceholderTests {
        @Test @DisplayName("@ClientType") void clientType() { assertEquals("Notes", eval("@ClientType")); }
        @Test @DisplayName("@DbExists") void dbExists() { assertEquals(1.0, eval("@DbExists(\"\"; \"\")")); }
        @Test @DisplayName("@GetCurrentTimeZone") void tz() { assertNotNull(eval("@GetCurrentTimeZone")); }
        @Test @DisplayName("@LanguagePreference") void lang() { assertEquals("EN", eval("@LanguagePreference")); }
        @Test @DisplayName("@Locale") void locale() { assertNotNull(eval("@Locale")); }
        @Test @DisplayName("@Keywords") void keywords() { assertEquals(List.of(), eval("@Keywords")); }
        @Test @DisplayName("@ThisName/@ThisValue") void thisX() { assertEquals("", eval("@ThisName")); assertEquals("", eval("@ThisValue")); }
        @Test @DisplayName("@UrlQueryString") void url() { assertEquals("", eval("@UrlQueryString")); }
        @Test @DisplayName("@V3UserName") void v3() { assertEquals("Alice", eval("@V3UserName")); }
        @Test @DisplayName("@V4UserAccess") void v4() { assertEquals(5.0, eval("@V4UserAccess")); }
        @Test @DisplayName("@Unavailable") void unavail() { assertEquals(1.0, eval("@Unavailable(\"NoSuchField\")")); }
        @Test @DisplayName("@Environment") void env() { assertEquals("", eval("@Environment(\"FOO\")")); }
    }

    @Nested @DisplayName("@NewLine @No @Yes @Nothing @Random")
    class ConstantTests {
        @Test @DisplayName("@NewLine") void newline() { assertEquals("\n", eval("@NewLine")); }
        @Test @DisplayName("@Yes") void yes() { assertEquals(1.0, eval("@Yes")); }
        @Test @DisplayName("@No") void no() { assertEquals(0.0, eval("@No")); }
        @Test @DisplayName("@Nothing") void nothing() { assertEquals("", eval("@Nothing")); }
        @Test @DisplayName("@Random") void random() { assertTrue(((Double)eval("@Random")) >= 0 && ((Double)eval("@Random")) <= 1); }
    }
}
