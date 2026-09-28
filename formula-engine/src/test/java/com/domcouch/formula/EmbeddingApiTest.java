package com.domcouch.formula;

import com.domcouch.formula.translate.FormulaTranslator;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API for embedding the engine in form runtimes (e.g. DXL forms rendered in Vaadin):
 * current-field context, custom @Functions, error truthiness, referenced fields.
 */
@DisplayName("Embedding API")
class EmbeddingApiTest extends BaseFormulaTest {

    /** Context for the field currently being translated/validated. */
    private FormulaContext fieldCtx(String name, Object value) {
        return new FormulaContext() {
            @Override public Object resolve(String n) { return vars.get(n); }
            @Override public String getThisName() { return name; }
            @Override public Object getThisValue() { return value; }
        };
    }

    @Nested @DisplayName("@ThisName / @ThisValue")
    class ThisField {
        @Test @DisplayName("@ThisValue in input translation")
        void thisValueTrim() {
            assertEquals("Muster", evaluator.evalExpr("@ProperCase(@Trim(@ThisValue))", fieldCtx("Person_Nachname", "  muster ")));
        }

        @Test @DisplayName("@ThisName returns the current field name")
        void thisName() {
            assertEquals("Person_Nachname", evaluator.evalExpr("@ThisName", fieldCtx("Person_Nachname", "")));
        }

        @Test @DisplayName("@ThisValue in input validation")
        void thisValueValidation() {
            String formula = "@If(@ThisValue = \"\"; @Failure(\"Pflichtfeld\"); @Success)";
            assertEquals("Pflichtfeld", evaluator.evalExpr(formula, fieldCtx("X", "")));
            assertEquals(1.0, evaluator.evalExpr(formula, fieldCtx("X", "abc")));
        }

        @Test @DisplayName("null current value is \"\"")
        void nullValue() {
            assertEquals("", evaluator.evalExpr("@ThisValue", fieldCtx("X", null)));
        }
    }

    @Nested @DisplayName("registerFunction")
    class CustomFunctions {
        @Test @DisplayName("custom @Function is callable, name case-insensitive, @ optional")
        void register() {
            evaluator.registerFunction("@GetProfileField", (ev, args, ctx) ->
                    "profile:" + Evaluator.convertToString(ev.eval(args.get(1), ctx)));
            assertEquals("profile:ShowHiddenFields",
                    eval("@getprofilefield(\"UserProfile\"; \"ShowHiddenFields\"; @UserName)"));
            assertTrue(evaluator.isFunctionRegistered("GETPROFILEFIELD"));
            assertTrue(evaluator.getFunctionNames().contains("GETPROFILEFIELD"));
        }

        @Test @DisplayName("built-in can be replaced")
        void replaceBuiltin() {
            evaluator.registerFunction("UserName", (ev, args, ctx) -> "Bob");
            assertEquals("Bob", eval("@UserName"));
        }

        @Test @DisplayName("FormulaTranslator delegates registration")
        void translator() {
            var tr = new FormulaTranslator().registerFunction("Prompt", (ev, args, ctx) -> 1.0);
            assertTrue(tr.isFunctionRegistered("@Prompt"));
            assertEquals(1.0, tr.evaluate("@Prompt([Ok]; \"T\"; \"M\")", ctx()));
        }

        @Test @DisplayName("unknown @Function is not registered")
        void unknown() {
            assertFalse(evaluator.isFunctionRegistered("@NoSuchFunction"));
        }
    }

    @Nested @DisplayName("Error truthiness")
    class ErrorTruthiness {
        @Test @DisplayName("ERROR_VALUE is falsy (failing hide-when does not hide)")
        void errorIsFalsy() {
            assertFalse(Evaluator.isTruthy(eval("@NoSuchFunction(\"x\")")));
        }
    }

    @Nested @DisplayName("CompiledFormula references")
    class References {
        private final FormulaTranslator tr = new FormulaTranslator();

        @Test @DisplayName("fields excluding temp variables")
        void fields() {
            CompiledFormula cf = tr.compile(
                    "_bh := @Left(Adresse_GKZ; 3); @If(Person_Inaktiv = \"1\"; _bh; Person_Nachname)");
            assertEquals(Set.of("ADRESSE_GKZ", "PERSON_INAKTIV", "PERSON_NACHNAME"), cf.referencedFields());
            assertEquals(Set.of("LEFT", "IF"), cf.referencedFunctions());
        }

        @Test @DisplayName("FIELD assignment target counts as a field")
        void fieldAssign() {
            CompiledFormula cf = tr.compile("FIELD Status := @UpperCase(Code)");
            assertEquals(Set.of("STATUS", "CODE"), cf.referencedFields());
        }

        @Test @DisplayName("constant formula has no references")
        void constant() {
            CompiledFormula cf = tr.compile("\"\"");
            assertTrue(cf.referencedFields().isEmpty());
            assertTrue(cf.referencedFunctions().isEmpty());
        }
    }
}
