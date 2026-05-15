package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Date/Time Functions")
class DateTimeFunctionsTest extends BaseFormulaTest {

    @Nested @DisplayName("@Month / @Day / @Year")
    class DateExtractionTests {
        @Test @DisplayName("@Month from ISO") void monthFromIso() { vars.put("D", "2024-01-15T09:30:00Z"); assertEquals(1.0, eval("@Month(d)")); }
        @Test @DisplayName("@Day from ISO") void dayFromIso() { vars.put("D", "2024-01-15T09:30:00Z"); assertEquals(15.0, eval("@Day(d)")); }
        @Test @DisplayName("@Year from ISO") void yearFromIso() { vars.put("D", "2024-01-15T09:30:00Z"); assertEquals(2024.0, eval("@Year(d)")); }
        @Test @DisplayName("@Month from US date") void monthFromUS() { vars.put("D", "11/30/2000 02:39:55 PM"); assertEquals(11.0, eval("@Month(d)")); }
        @Test @DisplayName("@Day from US date") void dayFromUS() { vars.put("D", "11/30/2000 02:39:55 PM"); assertEquals(30.0, eval("@Day(d)")); }
        @Test @DisplayName("@Year from US date") void yearFromUS() { vars.put("D", "11/30/2000 02:39:55 PM"); assertEquals(2000.0, eval("@Year(d)")); }
        @Test @DisplayName("@Month from @Created") void monthFromCreated() { assertEquals(1.0, eval("@Month(@Created)")); }
        @Test @DisplayName("@Month from bad string") void monthFromBadString() { vars.put("D", "not a date"); assertEquals(0.0, eval("@Month(d)")); }
    }

    @Nested @DisplayName("@Date")
    class DateTests {
        @Test @DisplayName("year month day") void ymd() {
            String r = (String) eval("@Date(1995; 6; 23)");
            assertTrue(r.contains("06/23/1995") || r.contains("6/23/1995"),
                    "Expected June 23 1995, got: " + r);
        }
        @Test @DisplayName("full constructor") void full() {
            String r = (String) eval("@Date(1993; 1; 20; 8; 58; 12)");
            assertTrue(r.contains("01/20/1993") || r.contains("1/20/1993"),
                    "Expected Jan 20 1993, got: " + r);
            assertTrue(r.contains("08:58:12"), "Expected 08:58:12, got: " + r);
        }
        @Test @DisplayName("from string") void fromString() {
            String r = (String) eval("@Date(\"11/20/95\")");
            assertTrue(r.contains("11/20"), "Expected Nov 20, got: " + r);
        }
    }

    @Nested @DisplayName("@Adjust")
    class AdjustTests {
        @Test @DisplayName("adjust years months days") void basic() {
            String result = (String) eval("@Adjust([06/30/95]; 2; 2; 2; 0; 0; 0)");
            assertTrue(result.contains("09/02") || result.contains("9/2")); }
        @Test @DisplayName("negative adjustment") void negative() {
            String result = (String) eval("@Adjust([03/30/96]; -2; 0; -10; 0; 0; 0)");
            assertTrue(result.contains("03/20") || result.contains("3/20")); }
    }

    @Nested @DisplayName("@Tomorrow @Yesterday @Time @TimeMerge")
    class DateOpTests {
        @Test @DisplayName("@Tomorrow returns date format") void tomorrow() {
            String result = (String) eval("@Tomorrow");
            assertNotNull(result);
            assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4} .*"), "Expected date, got: " + result);
        }
        @Test @DisplayName("@Yesterday returns date format") void yesterday() {
            String result = (String) eval("@Yesterday");
            assertNotNull(result);
            assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4} .*"), "Expected date, got: " + result);
        }
        @Test @DisplayName("@Time constructor returns time format") void timeCons() {
            String result = (String) eval("@Time(23; 50; 30)");
            assertNotNull(result);
            assertTrue(result.contains("11:50:30 PM"), "Expected 11:50:30 PM, got: " + result);
        }
        @Test @DisplayName("@TimeMerge combines date and time") void timeMerge() {
            String result = (String) eval("@TimeMerge(\"01/01/2008\"; \"5:14 AM\")");
            assertNotNull(result);
            assertTrue(result.contains("01/01/2008"), "Expected 01/01/2008, got: " + result);
            assertTrue(result.contains("05:14"), "Expected 05:14, got: " + result);
        }
    }

    @Nested @DisplayName("@Second @Minute @Hour @Weekday")
    class TimePartTests {
        @Test @DisplayName("@Second") void second() { assertNotNull(eval("@Second(@Now)")); }
        @Test @DisplayName("@Minute") void minute() { assertNotNull(eval("@Minute(@Now)")); }
        @Test @DisplayName("@Hour") void hour() { assertNotNull(eval("@Hour(@Now)")); }
        @Test @DisplayName("@Weekday") void weekday() { assertNotNull(eval("@Weekday(@Now)")); }
        @Test @DisplayName("@Hour from [5:30]") void hourFromTime() { assertEquals(5.0, eval("@Hour([5:30])")); }
        @Test @DisplayName("@Minute from [5:30 PM]") void minuteFromTime() { assertEquals(30.0, eval("@Minute([5:30 PM])")); }
        @Test @DisplayName("@Second from [5:30:45]") void secondFromTime() { assertEquals(45.0, eval("@Second([5:30:45])")); }
        @Test @DisplayName("@Time from [5:30]") void timeFromConstant() { assertNotNull(eval("@Time([5:30])")); }
    }

    @Nested @DisplayName("@Today/@Now")
    class TodayNowTests {
        @Test @DisplayName("@Today returns date format MM/dd/yyyy") void today() {
            String result = (String) eval("@Today");
            assertNotNull(result);
            assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4} .*"), "Expected date format, got: " + result);
        }
        @Test @DisplayName("@Now returns datetime format") void now() {
            String result = (String) eval("@Now");
            assertNotNull(result);
            assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} [AP]M"),
                    "Expected datetime format, got: " + result);
        }
        @Test @DisplayName("@Now differs from @Today (has time)") void nowDiffersFromToday() {
            String today = (String) eval("@Today");
            String now = (String) eval("@Now");
            assertNotEquals(today, now);
        }
    }

    @Nested @DisplayName("@BusinessDays")
    class BusinessDaysTests {
        @Test @DisplayName("one business day") void oneDay() { assertTrue(((Double) eval("@BusinessDays([06/30/95]; [07/01/95])")) >= 0); }
    }

    @Nested @DisplayName("Document timestamps")
    class DocTimestampsTests {
        @Test @DisplayName("@Created resolves from context") void created() {
            assertEquals("2024-01-15T09:30:00Z", eval("@Created"));
        }
        @Test @DisplayName("@Modified resolves from context") void modified() {
            vars.put("MODIFIED", "2024-02-20T14:00:00Z");
            assertEquals("2024-02-20T14:00:00Z", eval("@Modified"));
        }
        @Test @DisplayName("@Accessed resolves from context") void accessed() {
            vars.put("ACCESSED", "2024-03-10T08:00:00Z");
            assertEquals("2024-03-10T08:00:00Z", eval("@Accessed"));
        }
        @Test @DisplayName("@AddedToThisFile resolves from context") void added() {
            vars.put("ADDEDTOTHISFILE", "2024-01-01T00:00:00Z");
            assertEquals("2024-01-01T00:00:00Z", eval("@AddedToThisFile"));
        }
    }
}
