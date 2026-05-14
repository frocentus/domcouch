package com.domcouch.formula;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Math Functions")
class MathFunctionsTest extends BaseFormulaTest {

    @Nested @DisplayName("Math functions")
    class MathTests {
        @Test @DisplayName("@Pi") void pi() { assertEquals(Math.PI, (Double) eval("@Pi"), 0.0001); }
        @Test @DisplayName("@Power") void power() { assertEquals(8.0, eval("@Power(2; 3)")); }
        @Test @DisplayName("@Sqrt") void sqrt() { assertEquals(3.0, eval("@Sqrt(9)")); }
        @Test @DisplayName("@Exp") void exp() { assertEquals(Math.exp(1.25), (Double) eval("@Exp(1.25)"), 0.0001); }
        @Test @DisplayName("@Log") void log() { assertEquals(0.0, eval("@Log(1)")); }
        @Test @DisplayName("@Cos") void cos() { assertEquals(1.0, (Double) eval("@Cos(2 * @Pi)"), 0.0001); }
        @Test @DisplayName("@Sin") void sin() { assertEquals(Math.sin(0.5), (Double) eval("@Sin(0.5)"), 0.0001); }
        @Test @DisplayName("@Tan") void tan() { assertEquals(Math.tan(0.5), (Double) eval("@Tan(0.5)"), 0.0001); }
        @Test @DisplayName("@Integer") void integer() { assertEquals(3.0, eval("@Integer(3.7)")); }
        @Test @DisplayName("@Round") void round() { assertEquals(4.0, eval("@Round(3.7)")); }
    }

    @Nested @DisplayName("Arc trig")
    class ArcTrigTests {
        @Test @DisplayName("@ATan") void atan() { assertEquals(Math.atan(1.0), (Double) eval("@ATan(1)"), 0.0001); }
        @Test @DisplayName("@ATan2") void atan2() { assertEquals(Math.atan2(1.0, 1.0), (Double) eval("@ATan2(1; 1)"), 0.0001); }
        @Test @DisplayName("@ASin") void asin() { assertEquals(Math.asin(0.5), (Double) eval("@ASin(0.5)"), 0.0001); }
        @Test @DisplayName("@ACos") void acos() { assertEquals(Math.acos(0.5), (Double) eval("@ACos(0.5)"), 0.0001); }
    }

    @Nested @DisplayName("@Abs")
    class AbsTests {
        @Test @DisplayName("negative") void negative() { assertEquals(2.16, eval("@Abs(-2.16)")); }
        @Test @DisplayName("positive") void positive() { assertEquals(2.16, eval("@Abs(2.16)")); }
        @Test @DisplayName("list") void list() { assertEquals(java.util.List.of(2.15, 2.16), eval("@Abs(2.15 : (-2.16))")); }
        @Test @DisplayName("field") void field() { vars.put("NET", -5.0); assertEquals(5.0, eval("@Abs(Net)")); }
    }

    @Nested @DisplayName("@Ln")
    class LnTests {
        @Test @DisplayName("@Ln(2)") void ln1() { assertEquals(Math.log(2), (Double) eval("@Ln(2)"), 0.0001); }
    }

    @Nested @DisplayName("@FloatEq")
    class FloatEqTests {
        @Test @DisplayName("equal within epsilon") void eq() { assertEquals(1.0, eval("@FloatEq(1.000001; 1.000002; 0.001)")); }
        @Test @DisplayName("not equal") void neq() { assertEquals(0.0, eval("@FloatEq(1.0; 2.0; 0.1)")); }
    }

    @Nested @DisplayName("@Max @Min @Sum @Modulo @Sign")
    class AggregationTests {
        @Test @DisplayName("@Max(1;3)") void maxSimple() { assertEquals(3.0, eval("@Max(1; 3)")); }
        @Test @DisplayName("@Max pairwise") void maxPairwise() { assertEquals(99.0, eval("@Max(99:2:3; 5:6:7:8)")); }
        @Test @DisplayName("@Min(35;100)") void minSimple() { assertEquals(35.0, eval("@Min(35; 100)")); }
        @Test @DisplayName("@Min negative") void minNeg() { assertEquals(-35.0, eval("@Min((-3.5):(-35):100; (-2):45:54)")); }
        @Test @DisplayName("@Sum list") void sumList() { assertEquals(3.0, eval("@Sum(1 : 2)")); }
        @Test @DisplayName("@Sum neg pair") void sumNeg() { assertEquals(11.0, eval("@Sum((-1):2; (-10):20)")); }
        @Test @DisplayName("@Modulo(4;3)") void mod1() { assertEquals(1.0, eval("@Modulo(4; 3)")); }
        @Test @DisplayName("@Modulo(4;2)") void mod2() { assertEquals(0.0, eval("@Modulo(4; 2)")); }
        @Test @DisplayName("@Modulo neg") void modNeg() { assertEquals(1.0, eval("@Modulo((-14); 3)")); }
        @Test @DisplayName("@Sign positive") void signPos() { assertEquals(1.0, eval("@Sign(42)")); }
        @Test @DisplayName("@Sign negative") void signNeg() { assertEquals(-1.0, eval("@Sign(-42)")); }
        @Test @DisplayName("@Sign zero") void signZero() { assertEquals(0.0, eval("@Sign(0)")); }
    }
}
