package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Control Flow")
class ControlFlowTest extends BaseFormulaTest {

    @Nested @DisplayName("@While")
    class WhileTests {
        @Test @DisplayName("simple counter") void simpleCounter() {
            assertEquals(4.0, eval("n := 1; @While(n <= 3; n := n + 1; 0); n")); }
        @Test @DisplayName("@While with @Do") void whileWithDo() {
            assertEquals(15.0, eval("sum := 0; i := 1; @While(i <= 5; @Do(sum := sum + i; i := i + 1); 0); sum")); }
        @Test @DisplayName("@While empty condition") void whileEmpty() {
            assertEquals(0.0, eval("n := 0; @While(n > 10; n := n + 1; 0); n")); }
    }

    @Nested @DisplayName("@Set and @SetField")
    class SetTests {
        @Test @DisplayName("@Set assigns temp variable") void setTempVar() {
            assertEquals("hello", eval("@Set(\"x\"; \"hello\"); x")); }
        @Test @DisplayName("@SetField writes to document") void setFieldWrites() {
            CachedEvaluationTest.MapFormulaContext mctx = new CachedEvaluationTest.MapFormulaContext(new HashMap<>());
            evaluator.evalExpr("@SetField(\"Subject\"; \"Updated\")", mctx);
            assertEquals("Updated", mctx.get("SUBJECT")); }
    }

    @Nested @DisplayName("@DoWhile")
    class DoWhileTests {
        @Test @DisplayName("executes body then checks condition") void doWhile() {
            assertEquals(1.0, eval("x := 0; @DoWhile(x := x + 1; x < 3)")); }
    }

    @Nested @DisplayName("@For")
    class ForTests {
        @Test @DisplayName("simple counter") void counter() { assertEquals(3.0, eval("x := 0; @For(i := 1; i <= 3; i := i + 1; x := x + 1); x")); }
        @Test @DisplayName("loop sum 1..5") void loopDo() { assertEquals(15.0, eval("t := 0; @For(n := 1; n <= 5; n := n + 1; t := t + n); t")); }
        @Test @DisplayName("subscript items[1]") void subscript1() { vars.put("ITEMS", List.of("a","b","c")); assertEquals("a", eval("items[1]")); }
        @Test @DisplayName("subscript in @For literal") void subscriptForLiteral() {
            vars.put("ITEMS", List.of("a","b","c")); assertEquals("aaa", eval("t := \"\"; @For(n := 1; n <= 3; n := n + 1; t := t + items[1]); t")); }
        @Test @DisplayName("subscript in @For body") void subscriptBody() {
            vars.put("ITEMS", List.of("a","b","c")); assertEquals("abc", eval("t := \"\"; @For(n := 1; n <= @Elements(items); n := n + 1; t := t + items[n]); t")); }
    }

    @Nested @DisplayName("@Eval")
    class EvalTests {
        @Test @DisplayName("basic meta-evaluation") void basic() { assertEquals(3.0, eval("@Eval(\"1 + 2\")")); }
        @Test @DisplayName("assignment in eval") void assignInEval() { assertEquals("rebar", eval("@Eval(\"x := \\\"re\\\"; x + \\\"bar\\\"\")")); }
    }

    @Nested @DisplayName("@Error/@IsError")
    class ErrorTests {
        @Test @DisplayName("@IsError detects @Error") void isErrorDetects() { assertEquals(1.0, eval("@IsError(@Error)")); }
        @Test @DisplayName("non-error not error") void notError() { assertEquals(0.0, eval("@IsError(42)")); }
    }

    @Nested @DisplayName("@CheckFormulaSyntax")
    class CheckFormulaSyntaxTests {
        @Test @DisplayName("valid formula") void valid() { assertEquals("1", eval("@CheckFormulaSyntax(\"1 + 2\")")); }
        @Test @DisplayName("invalid formula") void invalid() { assertTrue(eval("@CheckFormulaSyntax(\"@Foo(\")") instanceof List); }
    }
}
