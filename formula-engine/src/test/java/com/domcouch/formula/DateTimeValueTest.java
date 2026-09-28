package com.domcouch.formula;

import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Time-date values and regional formats")
class DateTimeValueTest extends BaseFormulaTest {

    @Nested @DisplayName("US formats (default)")
    class UsTests {
        @Test @DisplayName("date-only values have no time")
        void dateOnly() {
            assertEquals("03/15/2024", String.valueOf(eval("@Date(2024; 3; 15)")));
            assertEquals("03/15/2024", eval("@Text(@Date(2024; 3; 15))"));
            assertFalse(((DateTimeValue) eval("@Today")).hasTime());
        }

        @Test @DisplayName("date and time keep the historic format")
        void dateTime() {
            assertEquals("03/15/2024 10:20:00 AM", String.valueOf(eval("@Date(2024; 3; 15; 10; 20; 0)")));
            assertEquals("11:50:30 PM", String.valueOf(eval("@Time(23; 50; 30)")));
        }

        @Test @DisplayName("comparison is chronological, not by text")
        void chronological() {
            assertEquals(1.0, eval("@Date(2023; 12; 1) < @Date(2024; 3; 15)"));
            vars.put("DATUM", "12/01/2023");
            assertEquals(1.0, eval("Datum < @Date(2024; 3; 15)"), "text item compared with a time-date");
            assertEquals(1.0, eval("@Date(2024; 3; 15) = @Date(2024; 3; 15)"));
            assertEquals(1.0, eval("@Today > @Yesterday & @Today < @Tomorrow"));
        }

        @Test @DisplayName("date - date = seconds, date ± number adds seconds")
        void arithmetic() {
            assertEquals(86400.0, eval("@Date(2024; 3; 16) - @Date(2024; 3; 15)"));
            assertEquals("03/15/2024 01:00:00 AM", String.valueOf(eval("@Date(2024; 3; 15) + 3600")));
            assertEquals("03/14/2024 11:00:00 PM", String.valueOf(eval("@Date(2024; 3; 15) - 3600")));
        }

        @Test @DisplayName("@Adjust keeps a date-only value date only")
        void adjust() {
            assertEquals("03/16/2024", String.valueOf(eval("@Adjust(@Date(2024; 3; 15); 0; 0; 1; 0; 0; 0)")));
        }

        @Test @DisplayName("ISO-8601 storage reads back as the same kind of value")
        void iso() {
            DateTimeValue date = DateTimeValue.ofDate(LocalDate.of(2024, 3, 15));
            DateTimeValue dateTime = DateTimeValue.of(LocalDateTime.of(2024, 3, 15, 10, 20));
            DateTimeValue time = DateTimeValue.ofTime(java.time.LocalTime.of(10, 20));
            assertEquals("2024-03-15", date.toIso());
            assertEquals("10:20:00", time.toIso());
            for (DateTimeValue v : java.util.List.of(date, dateTime, time)) {
                assertEquals(v, DateTimeValue.from(v.toIso()), v.toIso());
            }
        }

        @Test @DisplayName("hosts pass java.time values")
        void hostValues() {
            vars.put("DATUM", DateTimeValue.ofDate(LocalDate.of(2024, 3, 15)));
            vars.put("ZEIT", DateTimeValue.of(LocalDateTime.of(2024, 3, 15, 10, 20)));
            assertEquals(15.0, eval("@Day(Datum)"));
            assertEquals(1.0, eval("@IsTime(Datum)"));
            assertEquals(0.0, eval("@IsText(Datum)"));
            assertEquals(37200.0, eval("Zeit - Datum"));
        }
    }

    @Nested @DisplayName("de-AT formats")
    class GermanTests {
        @BeforeEach void german() { DateFormats.setDefault(DateFormats.of(Locale.forLanguageTag("de-AT"))); }
        @AfterEach void us() { DateFormats.setDefault(DateFormats.US); }

        @Test @DisplayName("patterns of the locale, four-digit year")
        void patterns() {
            DateFormats f = DateFormats.getDefault();
            assertEquals("dd.MM.yyyy", f.datePattern());
            assertEquals("HH:mm:ss", f.timePattern());
        }

        @Test @DisplayName("@Text and concatenation show the regional format")
        void text() {
            assertEquals("15.03.2024", eval("@Text(@Date(2024; 3; 15))"));
            assertEquals("15.03.2024 10:20:00", eval("@Text(@Date(2024; 3; 15; 10; 20; 0))"));
            assertEquals("23:50:30", eval("@Text(@Time(23; 50; 30))"));
            assertEquals("am: 15.03.2024", eval("\"am: \" + @Text(@Date(2024; 3; 15))"));
        }

        @Test @DisplayName("@Text format codes")
        void codes() {
            assertEquals("15.03.2024", eval("@Text(@Date(2024; 3; 15); \"D0\")"));
            assertEquals("15.03", eval("@Text(@Date(2024; 3; 15); \"D2\")"));
            assertEquals("03.2024", eval("@Text(@Date(2024; 3; 15); \"D3\")"));
            assertEquals("10:20", eval("@Text(@Date(2024; 3; 15; 10; 20; 0); \"T1\")"));
        }

        @Test @DisplayName("text in the regional, US and ISO format is parsed")
        void parse() {
            assertEquals(15.0, eval("@Day(\"15.03.2024\")"));
            assertEquals(10.0, eval("@Hour(@TextToTime(\"15.03.2024 10:20:00\"))"));
            assertEquals(3.0, eval("@Month(\"03/15/2024 10:20:00 AM\")"), "stored US text stays readable");
            assertEquals(2024.0, eval("@Year(\"2024-03-15T10:20:00Z\")"));
            assertEquals(1.0, eval("@IsTime(\"15.03.2024\")"));
            assertEquals(0.0, eval("@IsTime(\"kein Datum\")"));
        }

        @Test @DisplayName("comparison with regional text")
        void compare() {
            vars.put("DATUM", "01.12.2023");
            assertEquals(1.0, eval("Datum < @Date(2024; 3; 15)"));
        }
    }
}
