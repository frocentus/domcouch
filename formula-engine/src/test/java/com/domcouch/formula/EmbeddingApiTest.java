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
            assertTrue(tr.getFunctionNames().contains("PROMPT"));
            assertTrue(tr.getFunctionNames().contains("TRIM"));
            assertEquals(1.0, tr.evaluate("@Prompt([Ok]; \"T\"; \"M\")", ctx()));
        }

        @Test @DisplayName("setCurrentUserName reaches @UserName (no-arg constructor)")
        void userNameNoArgConstructor() {
            var tr = new FormulaTranslator();
            tr.setCurrentUserName("Bob");
            assertEquals("Bob", tr.evaluate("@UserName", ctx()));
        }

        @Test @DisplayName("unknown @Function is not registered")
        void unknown() {
            assertFalse(evaluator.isFunctionRegistered("@NoSuchFunction"));
        }
    }

    @Nested @DisplayName("FormulaInterrupt")
    class Interrupts {
        /** Host signal, e.g. "wait for the user to answer @Prompt". */
        static class NeedsInput extends FormulaInterrupt {
            final String title;
            NeedsInput(String title) { super("needs input"); this.title = title; }
        }

        private final FormulaTranslator tr = new FormulaTranslator();

        @Test @DisplayName("@Return inside @IfError returns instead of being swallowed")
        void returnInsideIfError() {
            String f = "@IfError(@Return(\"returned\"); \"swallowed\"); \"continued\"";
            assertEquals("returned", tr.evaluate(f, ctx()));
            assertEquals("returned", tr.evaluate(tr.compile(f), ctx()));
        }

        @Test @DisplayName("host interrupt passes through @IfError, @If, @Do and @For")
        void hostInterruptPropagates() {
            tr.registerFunction("Prompt", (ev, args, c) -> {
                throw new NeedsInput(Evaluator.convertToString(ev.eval(args.get(1), c)));
            });
            String f = "@IfError(@If(1; @Do(@For(i := 1; i < 2; i := i + 1; @Prompt([Ok]; \"Titel\"; \"Text\"))); 0); \"swallowed\")";
            NeedsInput thrown = assertThrows(NeedsInput.class, () -> tr.evaluate(f, ctx()));
            assertEquals("Titel", thrown.title);
        }

        @Test @DisplayName("replay: pause at @Prompt, answer, evaluate again")
        void replay() {
            java.util.List<Object> answers = new java.util.ArrayList<>();
            int[] call = {0};
            tr.registerFunction("Prompt", (ev, args, c) -> {
                int n = call[0]++;
                if (n < answers.size()) return answers.get(n);
                throw new NeedsInput(Evaluator.convertToString(ev.eval(args.get(1), c)));
            });
            CompiledFormula f = tr.compile(
                    "a := @Prompt([OkCancelEdit]; \"Erste\"; \"\"; \"\"); b := @Prompt([OkCancelEdit]; \"Zweite\"; \"\"; \"\"); a + \"/\" + b");

            java.util.List<String> dialogs = new java.util.ArrayList<>();
            Object result = null;
            while (result == null) {
                call[0] = 0;
                try {
                    result = tr.evaluate(f, ctx());
                } catch (NeedsInput pause) {
                    dialogs.add(pause.title);
                    answers.add("Antwort " + answers.size());   // the user answers the dialog
                }
            }
            assertEquals(java.util.List.of("Erste", "Zweite"), dialogs);
            assertEquals("Antwort 0/Antwort 1", result);
        }

        @Test @DisplayName("@Eval keeps the caller's temporary variables")
        void evalKeepsTempScope() {
            assertEquals("keep", tr.evaluate("x := \"keep\"; @Eval(\"1 + 1\"); x", ctx()));
            assertEquals("keep", tr.evaluate(tr.compile("x := \"keep\"; @Eval(\"1 + 1\"); x"), ctx()));
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

        @Test @DisplayName("unary ! and - operands are collected")
        void unaryOperators() {
            CompiledFormula cf = tr.compile("!(@UserRoles *= \"[Designer]\") & -Amount < 0");
            assertEquals(Set.of("AMOUNT"), cf.referencedFields());
            assertEquals(Set.of("USERROLES"), cf.referencedFunctions());
        }

        @Test @DisplayName("constant formula has no references")
        void constant() {
            CompiledFormula cf = tr.compile("\"\"");
            assertTrue(cf.referencedFields().isEmpty());
            assertTrue(cf.referencedFunctions().isEmpty());
        }
    }
}
