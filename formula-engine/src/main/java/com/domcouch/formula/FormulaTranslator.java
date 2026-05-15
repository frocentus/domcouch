package com.domcouch.formula;

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
 * against different contexts or compiled formulas.
 */
public class FormulaTranslator {

    private final Evaluator evaluator;
    private String currentUserName = "Anonymous";

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
     * Handles {@code SELECT}, {@code & | !} operators, common {@code @Functions},
     * field references, and {@code IS [NOT] MISSING}.
     *
     * @param formula the Domino-style selection formula (e.g., {@code "Form = 'Person'"})
     * @return the N1QL boolean expression, or {@code null} if input is null
     */
    public String toN1ql(String formula) { /* ... unchanged ... */ return translate(formula); }

    // ---- Evaluation mode (Lexer → Parser → Evaluator) ----

    /**
     * Evaluate a computed formula, parsing it on every call.
     * For repeated use against many documents, prefer {@link #compile(String)}
     * followed by {@link #evaluate(CompiledFormula, FormulaContext)}.
     *
     * @param formula the Domino formula string (e.g., {@code "LastName + \", \" + FirstName"})
     * @param ctx     the resolution context (typically {@link DocumentFormulaContext})
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
        Object result = "";
        try {
            for (Expr stmt : compiled.statements()) {
                result = evaluator.eval(stmt, ctx);
            }
        } catch (Evaluator.ReturnValue rv) {
            return rv.value;
        }
        return result;
    }

    // ---- internal helpers ----

    private String translate(String formula) {
        if (formula == null) return null;
        if (formula.contains("doc.items.")) return formula;

        String result = formula.trim();
        result = result.replaceFirst("(?i)^\\s*SELECT\\s+", "");
        result = replaceOpOutsideQuotes(result, " & ", " AND ");
        result = replaceOpOutsideQuotes(result, " | ", " OR ");
        result = result.replaceAll("!(?!\\s*=)", " NOT ");
        result = translateAtFunctions(result);
        result = result.replaceAll(
                "\\b([A-Z][A-Za-z0-9_]*)\\s+(IS\\s+(NOT\\s+)?MISSING)",
                "doc.items.$1 $2");
        result = result.replaceAll(
                "\\b([A-Z][A-Za-z0-9_]*)\\s*(=|!=|<>|>=?|<=?|LIKE)",
                "doc.items.$1.`values`[0] $2");
        return result;
    }

    private String replaceOpOutsideQuotes(String s, String from, String to) {
        StringBuilder sb = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            if (!inSingle && !inDouble && s.regionMatches(i, from, 0, from.length())) {
                sb.append(to);
                i += from.length();
            } else { sb.append(c); i++; }
        }
        return sb.toString();
    }

    private String translateAtFunctions(String f) {
        f = f.replaceAll("@All(?![A-Za-z0-9_])", "true");
        f = f.replace("@IsResponseDoc", "doc.parentUNID IS NOT MISSING");
        f = f.replaceAll("@IsAvailable\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*\\)", "doc.items.$1 IS NOT MISSING");
        f = f.replaceAll("@Contains\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*;\\s*([^)]+)\\s*\\)", "CONTAINS(doc.items.$1.`values`[0], $2)");
        f = f.replaceAll("@Begins\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*;\\s*([^)]+)\\s*\\)", "doc.items.$1.`values`[0] LIKE ($2 || '%')");
        f = f.replaceAll("@Ends\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*;\\s*([^)]+)\\s*\\)", "doc.items.$1.`values`[0] LIKE ('%' || $2)");
        f = f.replaceAll("@IsMember\\(\\s*([^;]+)\\s*;\\s*([A-Z][A-Za-z0-9_]*)\\s*\\)", "$1 IN doc.items.$2.`values`");
        f = f.replaceAll("@IsNotMember\\(\\s*([^;]+)\\s*;\\s*([A-Z][A-Za-z0-9_]*)\\s*\\)", "$1 NOT IN doc.items.$2.`values`");
        f = f.replaceAll("@LowerCase\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*\\)", "LOWER(doc.items.$1.`values`[0])");
        f = f.replaceAll("@UpperCase\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*\\)", "UPPER(doc.items.$1.`values`[0])");
        f = f.replaceAll("@Trim\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*\\)", "TRIM(doc.items.$1.`values`[0])");
        f = f.replaceAll("@Length\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*\\)", "LENGTH(doc.items.$1.`values`[0])");
        f = f.replaceAll("@Left\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*;\\s*(\\d+)\\s*\\)", "SUBSTR(doc.items.$1.`values`[0], 0, $2)");
        f = f.replaceAll("@Right\\(\\s*([A-Z][A-Za-z0-9_]*)\\s*;\\s*(\\d+)\\s*\\)", "SUBSTR(doc.items.$1.`values`[0], LENGTH(doc.items.$1.`values`[0]) - $2)");
        f = f.replace("@Today", "NOW_STR()");
        f = f.replace("@Now", "NOW_STR()");
        f = f.replaceAll("@Created(?![A-Za-z0-9_])", "doc.created");
        f = f.replaceAll("@Modified(?![A-Za-z0-9_])", "doc.lastModified");
        f = f.replace("@UserName", "'" + currentUserName.replace("'", "''") + "'");
        f = f.replaceAll("@IsNumber\\(", "IS_NUMBER(");
        f = f.replaceAll("@IsText\\(", "IS_STRING(");
        f = f.replaceAll("@If\\(\\s*([^;]+)\\s*;\\s*([^;]+)\\s*;\\s*([^)]+)\\s*\\)", "CASE WHEN $1 THEN $2 ELSE $3 END");
        return f;
    }
}
