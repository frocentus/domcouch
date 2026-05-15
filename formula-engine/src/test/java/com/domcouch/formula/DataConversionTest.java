package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Data Conversion")
class DataConversionTest extends BaseFormulaTest {

    @Nested @DisplayName("@Text")
    class TextTests {
        @Test @DisplayName("simple conversion") void simple() { assertEquals("123.45", eval("@Text(123.45)")); }
        @Test @DisplayName("currency format") void currency() { assertEquals("$800.00", eval("@Text(800; \"C,2\")")); }
        @Test @DisplayName("scientific format") void scientific() { assertEquals("8.00E+02", eval("@Text(800; \"S\")")); }
        @Test @DisplayName("list with format") void listFormat() { assertEquals(List.of("8.00E+02", "-6.00E+02"), eval("@Text(800 : (-600); \"S\")")); }
        @Test @DisplayName("date format D0 (MM/dd/yyyy)") void dateD0() {
            String result = (String) eval("@Text(@Created; \"D0\")");
            assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4}"), "Expected MM/dd/yyyy, got: " + result);
        }
        @Test @DisplayName("date format D1 (MM/dd)") void dateD1() {
            String result = (String) eval("@Text(@Created; \"D1\")");
            assertTrue(result.matches("\\d{2}/\\d{2}"), "Expected MM/dd, got: " + result);
        }
        @Test @DisplayName("date format T0 (HH:mm:ss)") void timeT0() {
            String result = (String) eval("@Text(@Created; \"T0\")");
            assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2}"), "Expected HH:mm:ss, got: " + result);
        }
        @Test @DisplayName("date format S0 (MM/dd/yyyy)") void dateS0() {
            String result = (String) eval("@Text(@Created; \"S0\")");
            assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4}"), "Expected MM/dd/yyyy, got: " + result);
        }
    }

    @Nested @DisplayName("@TextToNumber")
    class TextToNumberTests {
        @Test @DisplayName("simple integer") void simple() { assertEquals(123.0, eval("@TextToNumber(\"123\")")); }
        @Test @DisplayName("leading numeric 12ABC") void leadingNumeric() { assertEquals(12.0, eval("@TextToNumber(\"12ABC\")")); }
        @Test @DisplayName("non-numeric start ABC12") void nonNumericStart() { assertEquals(0.0, eval("@TextToNumber(\"ABC12\")")); }
        @Test @DisplayName("list") void listSupport() { assertEquals(List.of(123.0, 456.0), eval("@TextToNumber(\"123\" : \"456\")")); }
        @Test @DisplayName("negative") void negative() { assertEquals(-42.0, eval("@TextToNumber(\"-42\")")); }
    }

    @Nested @DisplayName("@IsNumber")
    class IsNumberTests {
        @Test @DisplayName("actual number") void number() { assertEquals(1.0, eval("@IsNumber(123)")); }
        @Test @DisplayName("string not number") void stringNotNumber() { assertEquals(0.0, eval("@IsNumber(\"123\")")); }
        @Test @DisplayName("date not number") void dateNotNumber() { assertEquals(0.0, eval("@IsNumber(@Created)")); }
        @Test @DisplayName("number list") void numberList() { assertEquals(1.0, eval("@IsNumber(-345 : 2.78 : 997 : .7)")); }
        @Test @DisplayName("mixed list") void mixedList() { assertEquals(0.0, eval("@IsNumber(1 : \"two\")")); }
    }

    @Nested @DisplayName("@IsTime @TextToTime @ToNumber @ToTime")
    class DataConversionTests {
        @Test @DisplayName("@IsTime date string") void isTimeTrue() { assertEquals(1.0, eval("@IsTime(\"12/31/2025\")")); }
        @Test @DisplayName("@IsTime number") void isTimeNum() { assertEquals(0.0, eval("@IsTime(123)")); }
        @Test @DisplayName("@ToNumber strings") void toNumber() { assertEquals(List.of(20.0,40.0), eval("@ToNumber(\"20\" : \"40\")")); }
        @Test @DisplayName("@ToNumber non-numeric") void toNumberNan() { assertEquals(0.0, eval("@ToNumber(\"abc\")")); }
        @Test @DisplayName("@TextToTime") void textToTime() { assertNotNull(eval("@TextToTime(\"12/31/2025\")")); }
        @Test @DisplayName("@ToTime") void toTime() { assertNotNull(eval("@ToTime(\"2/29/08\")")); }
    }
}
