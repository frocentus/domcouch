package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("List Functions")
class ListFunctionsTest extends BaseFormulaTest {

    @Nested @DisplayName("@IsMember")
    class IsMemberTests {
        @Test @DisplayName("single value in list") void singleInList() {
            assertEquals(1.0, eval("@IsMember(\"computer\"; \"printer\" : \"computer\" : \"monitor\")")); }
        @Test @DisplayName("both lists subset") void bothListsSubset() {
            assertEquals(0.0, eval("@IsMember(\"computer\" : \"Notes\"; \"Notes\" : \"printer\" : \"monitor\")")); }
        @Test @DisplayName("single value subset") void singleSubset() {
            assertEquals(1.0, eval("@IsMember(\"Fred\"; \"Barney\" : \"Wilma\" : \"Fred\")")); }
        @Test @DisplayName("field reference") void fieldRef() {
            vars.put("DEPARTMENT", List.of("R&D", "Sales"));
            assertEquals(1.0, eval("@IsMember(\"R&D\"; Department)")); }
    }

    @Nested @DisplayName("@Replace")
    class ReplaceTests {
        @Test @DisplayName("element replacement") void elementReplace() {
            assertEquals(List.of("Red", "Black", "Yellow", "Green"),
                    eval("@Replace(\"Red\" : \"Orange\" : \"Yellow\" : \"Green\"; \"Orange\" : \"Blue\"; \"Black\" : \"Brown\")")); }
        @Test @DisplayName("no match") void noMatch() {
            assertEquals(List.of("A", "B", "C"), eval("@Replace(\"A\" : \"B\" : \"C\"; \"X\"; \"Y\")")); }
    }

    @Nested @DisplayName("@Count")
    class CountTests {
        @Test @DisplayName("list count") void listCount() { assertEquals(3.0, eval("@Count(\"a\":\"b\":\"c\")")); }
        @Test @DisplayName("scalar returns 1") void scalar() { assertEquals(1.0, eval("@Count(\"hello\")")); }
        @Test @DisplayName("null returns 1") void nullStr() { assertEquals(1.0, eval("@Count(\"\")")); }
    }

    @Nested @DisplayName("@Compare")
    class CompareTests {
        @Test @DisplayName("equal") void equalStrings() { assertEquals(0.0, eval("@Compare(\"abc\"; \"abc\")")); }
        @Test @DisplayName("less than") void lessThan() { assertEquals(-1.0, eval("@Compare(\"a\"; \"b\")")); }
        @Test @DisplayName("greater than") void greaterThan() { assertEquals(1.0, eval("@Compare(\"b\"; \"a\")")); }
    }

    @Nested @DisplayName("@Subset @Unique @Member @Implode @Sort")
    class ListManipTests {
        @Test @DisplayName("@Subset first 2") void subsetFirst() {
            assertEquals(List.of("New Orleans","London"), eval("@Subset(\"New Orleans\":\"London\":\"Frankfurt\":\"Tokyo\"; 2)")); }
        @Test @DisplayName("@Subset last 3") void subsetLast() {
            assertEquals(List.of("London","Frankfurt","Tokyo"), eval("@Subset(\"New Orleans\":\"London\":\"Frankfurt\":\"Tokyo\"; -3)")); }
        @Test @DisplayName("@Unique") void unique() {
            assertEquals(List.of("red","green","blue"), eval("@Unique(\"red\":\"green\":\"blue\":\"green\":\"red\")")); }
        @Test @DisplayName("@Member position") void memberPos() {
            assertEquals(3.0, eval("@Member(\"Sales\"; \"Finance\":\"Service\":\"Sales\":\"Legal\")")); }
        @Test @DisplayName("@Member not found") void memberNotFound() { assertEquals(0.0, eval("@Member(\"HR\"; \"Finance\":\"Service\":\"Sales\")")); }
        @Test @DisplayName("@Implode space") void implodeSpace() { assertEquals("a b c", eval("@Implode(\"a\":\"b\":\"c\")")); }
        @Test @DisplayName("@Implode comma") void implodeComma() { assertEquals("a,b,c", eval("@Implode(\"a\":\"b\":\"c\"; \",\")")); }
        @Test @DisplayName("@Sort ascending") void sortAsc() { assertEquals(List.of("a","b","c"), eval("@Sort(\"c\":\"a\":\"b\")")); }
    }

    @Nested @DisplayName("@Transform")
    class TransformTests {
        @Test @DisplayName("sqrt") void sqrtPos() { assertEquals(List.of(2.0, 3.0, 4.0), eval("@Transform(4 : 9 : 16; \"x\"; @Sqrt(x))")); }
        @Test @DisplayName("negate") void negate() { assertEquals(List.of(-1.0, -2.0, -3.0), eval("@Transform(1 : 2 : 3; \"v\"; -v)")); }
        @Test @DisplayName("prepend star") void prependStar() { assertEquals(List.of("*a","*b","*c"), eval("@Transform(\"a\" : \"b\" : \"c\"; \"var\"; \"*\" + var)")); }
        @Test @DisplayName("filter sqrt") void filterSqrt() { assertEquals(List.of(2.0, 0.0, 3.0), eval("@Transform(4 : (-5) : 9; \"x\"; @If(x >= 0; @Sqrt(x); 0))")); }
    }
}
