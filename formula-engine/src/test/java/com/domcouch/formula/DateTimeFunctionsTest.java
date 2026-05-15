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
        @Test @DisplayName("year month day") void ymd() { assertTrue(((String)eval("@Date(1995; 6; 23)")).contains("1995")); }
        @Test @DisplayName("full constructor") void full() { assertTrue(((String)eval("@Date(1993; 1; 20; 8; 58; 12)")).contains("08")); }
        @Test @DisplayName("from string") void fromString() { assertNotNull(eval("@Date(\"11/20/95\")")); }
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
        @Test @DisplayName("@Tomorrow") void tomorrow() { assertNotNull(eval("@Tomorrow")); }
        @Test @DisplayName("@Yesterday") void yesterday() { assertNotNull(eval("@Yesterday")); }
        @Test @DisplayName("@Time constructor") void timeCons() { assertNotNull(eval("@Time(23; 50; 30)")); }
        @Test @DisplayName("@TimeMerge") void timeMerge() { assertNotNull(eval("@TimeMerge(\"01/01/2008\"; \"5:14 AM\")")); }
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
        @Test @DisplayName("@Today") void today() { assertNotNull(eval("@Today")); }
        @Test @DisplayName("@Now") void now() { assertNotNull(eval("@Now")); }
    }

    @Nested @DisplayName("@BusinessDays")
    class BusinessDaysTests {
        @Test @DisplayName("one business day") void oneDay() { assertTrue(((Double) eval("@BusinessDays([06/30/95]; [07/01/95])")) >= 0); }
    }

    @Nested @DisplayName("Document timestamps")
    class DocTimestampsTests {
        @Test @DisplayName("@Created") void created() { eval("@Created"); }
        @Test @DisplayName("@Modified") void modified() { eval("@Modified"); }
        @Test @DisplayName("@Accessed") void accessed() { eval("@Accessed"); }
        @Test @DisplayName("@AddedToThisFile") void added() { eval("@AddedToThisFile"); }
    }
}
