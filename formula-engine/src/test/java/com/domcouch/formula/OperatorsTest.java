package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("List Operators")
class OperatorsTest extends BaseFormulaTest {

    @Nested @DisplayName("Pair-wise list operations")
    class PairwiseTests {
        @Test @DisplayName("concat A:B:C + 1:2:3") void concat3() { assertEquals(List.of("A1","B2","C3"), eval("\"A\":\"B\":\"C\" + \"1\":\"2\":\"3\"")); }
        @Test @DisplayName("concat A:B:C + 1:2 (repeat)") void concat2() { assertEquals(List.of("A1","B2","C2"), eval("\"A\":\"B\":\"C\" + \"1\":\"2\"")); }
        @Test @DisplayName("concat A:B:C + 1 (scalar)") void concatScalar() { assertEquals(List.of("A1","B1","C1"), eval("\"A\":\"B\":\"C\" + \"1\"")); }
        @Test @DisplayName("add 1:2:3 + 10:20:30") void add3() { assertEquals(List.of(11.0,22.0,33.0), eval("1:2:3 + 10:20:30")); }
        @Test @DisplayName("add 1:2:3 + 10:20 (repeat)") void add2() { assertEquals(List.of(11.0,22.0,23.0), eval("1:2:3 + 10:20")); }
        @Test @DisplayName("add 1:2:3 + 10 (scalar)") void addScalar() { assertEquals(List.of(11.0,12.0,13.0), eval("1:2:3 + 10")); }
        @Test @DisplayName("eq A:B:C = B:C:A (none)") void eq0() { assertEquals(0.0, eval("\"A\":\"B\":\"C\" = \"B\":\"C\":\"A\"")); }
        @Test @DisplayName("eq 2:3:3 = 2:3") void eq1() { assertEquals(1.0, eval("2:3:3 = 2:3")); }
        @Test @DisplayName("eq 2:3:3 = 3:1") void eq0b() { assertEquals(0.0, eval("2:3:3 = 3:1")); }
        @Test @DisplayName("A=B and A!=B both true") void bothTrue() { assertEquals(1.0, eval("1:2 = 1:3")); assertEquals(1.0, eval("1:2 != 1:3")); }
    }

    @Nested @DisplayName("Permuted list operations")
    class PermutedTests {
        @Test @DisplayName("concat A:B:C *+ 1:2:3") void concat3() {
            assertEquals(List.of("A1","A2","A3","B1","B2","B3","C1","C2","C3"), eval("\"A\":\"B\":\"C\" *+ \"1\":\"2\":\"3\"")); }
        @Test @DisplayName("add 1:2:3 *+ 10:20:30") void add3() {
            assertEquals(List.of(11.0,21.0,31.0,12.0,22.0,32.0,13.0,23.0,33.0), eval("1:2:3 *+ 10:20:30")); }
        @Test @DisplayName("eq A:B:C *= B:C:A") void eq1() { assertEquals(1.0, eval("\"A\":\"B\":\"C\" *= \"B\":\"C\":\"A\"")); }
        @Test @DisplayName("eq 1:2:3 *= 4:5") void eq0() { assertEquals(0.0, eval("1:2:3 *= 4:5")); }
    }
}
