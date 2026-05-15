package com.domcouch.formula.translate;

import com.domcouch.formula.*;

/**
 * Translates Domino formulas to Couchbase N1QL WHERE clauses (selection mode)
 * and evaluates computed formulas against document contexts (runtime mode).
 *
 * <h2>Two modes</h2>
 *
 * <b>Query translation</b> — regex-based, optimised for selection formulas:
 * <pre>{@code
 *   FormulaTranslator ft = new FormulaTranslator("Alice");
 *   String n1ql = ft.toN1ql("Form = 'Person' & Status = 'Active'");
 *   // → "doc.items.Form.`values`[0] = 'Person' AND doc.items.Status.`values`[0] = 'Active'"
 * }</pre>
 *
 * <b>Computed evaluation</b> — full Lexer → Parser → Evaluator pipeline:
 * <pre>{@code
 *   FormulaContext ctx = new DocumentFormulaContext(document);
 *   Object result = ft.evaluate("LastName + \", \" + FirstName", ctx);
 *   // → "Smith, John"
 * }</pre>
 *
 * <b>Compiled evaluation</b> — parse once, evaluate many times (recommended):
 * <pre>{@code
 *   CompiledFormula fullName = ft.compile("FirstName + \" \" + LastName");
 *   for (Document doc : documents) {
 *       DocumentFormulaContext ctx = new DocumentFormulaContext(doc);
 *       String name = (String) ft.evaluate(fullName, ctx);
 *   }
 * }</pre>
 *
 * <h2>Security</h2>
 * {@link #toN1ql(String)} embeds string literals directly into N1QL. Formulas passed
 * to this method must be developer-controlled, not end-user input. For user-supplied
 * text search, use parameterized {@code FTSearch}.
 *
 * <h2>Thread safety</h2>
 * Instances are thread-safe for concurrent evaluation. The {@link #compile(String)}
 * method may be called from any thread; evaluation methods may be called concurrently
 * against different contexts or compiled formulas. The {@code currentUserName} field
 * is volatile — calling {@link #setCurrentUserName(String)} while another thread is
 * evaluating may cause that evaluation to see the updated name on its next
 * {@code @UserName} access, but will not corrupt the evaluation itself.
 */
public class FormulaTranslator {

    private final Evaluator evaluator;
    private volatile String currentUserName = "Anonymous";

    /** Create a translator with the default user ({@code "Anonymous"}). */
    public FormulaTranslator() {
        this("Anonymous");
    }

    /**
     * Create a translator for a specific user.
     * @param currentUserName the username for {@code @UserName} resolution
     */
    public FormulaTranslator(String currentUserName) {
        this.currentUserName = currentUserName != null ? currentUserName : "Anonymous";
        this.evaluator = new Evaluator(currentUserName);
    }

    /** Update the user name for both query translation and formula evaluation. */
    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName != null ? currentUserName : "Anonymous";
        this.evaluator.setCurrentUserName(this.currentUserName);
    }

    /** @return the current user name used for {@code @UserName} resolution */
    public String getCurrentUserName() {
        return currentUserName;
    }

    // ---- Query mode (regex-based) ----

    /**
     * Translate a Domino selection formula to a N1QL {@code WHERE} clause.
     * <p>
     * The result is a boolean expression suitable for use after {@code WHERE}:
     * <pre>
     *   SELECT doc.* FROM bucket.scope.collection AS doc
     *   WHERE doc._type = 'domcouch.document' AND (<b>n1qlFormula</b>)
     * </pre>
     * Handles Domino view selection formulas: {@code SELECT}, {@code & | !}
     * operators, field references, and common {@code @Functions}.
     * <p>
     * Uses the Lexer → Parser pipeline for robust parsing (case-insensitive,
     * spacing-optional, quote-aware) then walks the AST to emit N1QL.
     *
     * @param formula the Domino-style selection formula (e.g., {@code "Form = 'Person'"})
     * @return the N1QL boolean expression, or {@code null} if input is null
     * @throws FormulaParseException if the formula cannot be parsed
     */
    public String toN1ql(String formula) {
        if (formula == null) return null;
        if (formula.contains("doc.items.")) return formula;
        return translate(formula);
    }

    /**
     * Translate a Domino value formula (column expression) to a N1QL value expression.
     * String concatenation ({@code +}) maps to N1QL {@code ||}.
     *
     * @param formula the Domino value formula (e.g., {@code "FirstName + \" \" + LastName"})
     * @return the N1QL value expression, or null if input is null
     * @throws FormulaParseException if the formula cannot be parsed
     */
    public String toN1qlValue(String formula) {
        if (formula == null) return null;
        return N1qlTranslator.translateValue(formula, currentUserName);
    }

    // ---- Evaluation mode (Lexer → Parser → Evaluator) ----

    /**
     * Evaluate a computed formula, parsing it on every call.
     * For repeated use against many documents, prefer {@link #compile(String)}
     * followed by {@link #evaluate(CompiledFormula, FormulaContext)}.
     *
     * @param formula the Domino formula string (e.g., {@code "LastName + \", \" + FirstName"})
     * @param ctx     the resolution context (typically {@link FormulaContext})
     * @return the computed value ({@code String}, {@code Double}, {@code List}, etc.)
     */
    public Object evaluate(String formula, FormulaContext ctx) {
        return evaluator.evalExpr(formula, ctx);
    }

    /**
     * Compile a formula once for repeated evaluation. The returned
     * {@link CompiledFormula} can be evaluated against any number of
     * {@link FormulaContext}s without re-parsing.
     *
     * @param formula the Domino formula string
     * @return a compiled formula ready for evaluation
     * @throws FormulaParseException if the formula cannot be parsed
     */
    public CompiledFormula compile(String formula) {
        return new CompiledFormula(new Parser(Lexer.tokenize(formula)).parse(), formula);
    }

    /**
     * Evaluate a pre-compiled formula — only runs the Evaluator stage,
     * skipping Lexer and Parser. This is the recommended path for
     * computed fields on forms that are evaluated against many documents.
     *
     * @param compiled a formula previously compiled via {@link #compile(String)}
     * @param ctx      the resolution context
     * @return the computed value
     */
    public Object evaluate(CompiledFormula compiled, FormulaContext ctx) {
        evaluator.initTempScope();
        try {
            Object result = "";
            try {
                for (Expr stmt : compiled.statements()) {
                    result = evaluator.eval(stmt, ctx);
                }
            } catch (Evaluator.ReturnValue rv) {
                return rv.value;
            }
            return result;
        } finally {
            evaluator.clearTempScope();
        }
    }

    // ---- internal helpers ----

    private String translate(String formula) {
        return N1qlTranslator.translate(formula, currentUserName);
    }
}