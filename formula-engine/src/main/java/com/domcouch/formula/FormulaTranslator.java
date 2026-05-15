package com.domcouch.formula;

import java.util.List;

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

    // ---- internal helpers: AST-based N1QL translation ----

    private String translate(String formula) {
        List<Token> tokens = Lexer.tokenize(formula);
        List<Expr> stmts = new Parser(tokens).parse();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stmts.size(); i++) {
            Expr stmt = stmts.get(i);
            if (stmt instanceof Expr.KeywordStatement ks && "SELECT".equals(ks.keyword())) {
                walkForN1ql(ks.body(), sb);
            } else {
                walkForN1ql(stmt, sb);
            }
            if (i < stmts.size() - 1 && isExpression(stmt)) {
                sb.append(" AND ");
            }
        }
        return sb.toString().trim();
    }

    /** True for expression nodes that don't already emit a complete N1QL clause. */
    private static boolean isExpression(Expr e) {
        return !(e instanceof Expr.KeywordStatement) && !(e instanceof Expr.Comment)
                && !(e instanceof Expr.DeleteField);
    }

    private void walkForN1ql(Expr expr, StringBuilder sb) {
        switch (expr) {
            case Expr.Variable v -> sb.append("doc.items.").append(escapeBacktick(v.name()))
                    .append(".`values`[0]");
            case Expr.StringConst s -> sb.append("'").append(s.value().replace("'", "''")).append("'");
            case Expr.NumberConst n -> sb.append(formatNumber(n.value()));
            case Expr.DateTimeConst d -> sb.append("'").append(d.raw().replace("'", "''")).append("'");
            case Expr.KeywordExpr kw -> {
                String v = kw.value().toString();
                if ("TRUE".equals(v) || "ALL".equals(v) || "YES".equals(v) || "SUCCESS".equals(v))
                    sb.append("true");
                else if ("FALSE".equals(v) || "NO".equals(v))
                    sb.append("false");
                else sb.append("'").append(v).append("'");
            }
            case Expr.BinaryOp bo -> walkBinary(bo, sb);
            case Expr.FunctionCall fc -> walkFunction(fc, sb);
            case Expr.KeywordStatement ks -> walkForN1ql(ks.body(), sb);
            case Expr.Comment c -> { /* skip */ }
            default -> sb.append(expr.toString()); // fallback: raw representation
        }
    }

    private void walkBinary(Expr.BinaryOp bo, StringBuilder sb) {
        String op = bo.op();
        // Logical operators
        if ("&".equals(op)) {
            sb.append("(");
            walkForN1ql(bo.left(), sb);
            sb.append(" AND ");
            walkForN1ql(bo.right(), sb);
            sb.append(")");
        } else if ("|".equals(op)) {
            sb.append("(");
            walkForN1ql(bo.left(), sb);
            sb.append(" OR ");
            walkForN1ql(bo.right(), sb);
            sb.append(")");
        } else if ("!".equals(op) && bo.left() == null) {
            // Unary NOT
            sb.append("NOT (");
            walkForN1ql(bo.right(), sb);
            sb.append(")");
        } else if ("=".equals(op)) {
            walkForN1ql(bo.left(), sb);
            sb.append(" = ");
            walkForN1ql(bo.right(), sb);
        } else {
            // Comparison operators: <>, ><, !=, >, <, >=, <=
            String n1qlOp = (op.equals("<>") || op.equals("><")) ? "!=" : op;
            walkForN1ql(bo.left(), sb);
            sb.append(" ").append(n1qlOp).append(" ");
            walkForN1ql(bo.right(), sb);
        }
    }

    private void walkFunction(Expr.FunctionCall fc, StringBuilder sb) {
        String name = fc.name();
        List<Expr> args = fc.args();
        switch (name) {
            case "ALL", "TRUE", "SUCCESS" -> sb.append("true");
            case "FALSE", "NO" -> sb.append("false");
            case "ISRESPONSEDOC" -> sb.append("doc.parentUNID IS NOT MISSING");
            case "TODAY", "NOW" -> sb.append("NOW_STR()");
            case "CREATED" -> sb.append("doc.created");
            case "MODIFIED" -> sb.append("doc.lastModified");
            case "USERNAME" -> sb.append("'").append(currentUserName.replace("'", "''")).append("'");
            case "ISAVAILABLE" -> {
                // String literal: convert to field reference
                // Variable: already emitted as doc.items.NAME.values[0]
                if (args.get(0) instanceof Expr.StringConst s) {
                    sb.append("doc.items.").append(escapeBacktick(s.value().toUpperCase()))
                            .append(".`values`[0]");
                } else {
                    walkForN1ql(args.get(0), sb);
                }
                sb.append(" IS NOT MISSING");
            }
            case "ISNUMBER" -> { sb.append("IS_NUMBER("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "ISTEXT" -> { sb.append("IS_STRING("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "CONTAINS" -> {
                sb.append("CONTAINS(");
                walkForN1ql(args.get(0), sb); sb.append(", ");
                walkForN1ql(args.get(1), sb); sb.append(")");
            }
            case "BEGINS" -> {
                walkForN1ql(args.get(0), sb); sb.append(" LIKE (");
                walkForN1ql(args.get(1), sb); sb.append(" || '%')");
            }
            case "ENDS" -> {
                walkForN1ql(args.get(0), sb); sb.append(" LIKE ('%' || ");
                walkForN1ql(args.get(1), sb); sb.append(")");
            }
            case "ISMEMBER" -> {
                walkForN1ql(args.get(0), sb); sb.append(" IN ");
                walkForN1ql(args.get(1), sb);
            }
            case "ISNOTMEMBER" -> {
                walkForN1ql(args.get(0), sb); sb.append(" NOT IN ");
                walkForN1ql(args.get(1), sb);
            }
            case "LOWERCASE" -> {
                sb.append("LOWER("); walkForN1ql(args.get(0), sb); sb.append(")");
            }
            case "UPPERCASE" -> {
                sb.append("UPPER("); walkForN1ql(args.get(0), sb); sb.append(")");
            }
            case "TRIM" -> {
                sb.append("TRIM("); walkForN1ql(args.get(0), sb); sb.append(")");
            }
            case "LENGTH" -> {
                sb.append("LENGTH("); walkForN1ql(args.get(0), sb); sb.append(")");
            }
            case "LEFT" -> {
                sb.append("SUBSTR("); walkForN1ql(args.get(0), sb);
                sb.append(", 0, "); walkForN1ql(args.get(1), sb); sb.append(")");
            }
            case "RIGHT" -> {
                sb.append("SUBSTR("); walkForN1ql(args.get(0), sb);
                sb.append(", LENGTH("); walkForN1ql(args.get(0), sb);
                sb.append(") - "); walkForN1ql(args.get(1), sb); sb.append(")");
            }
            case "IF" -> {
                if (args.size() >= 3) {
                    sb.append("CASE WHEN "); walkForN1ql(args.get(0), sb);
                    sb.append(" THEN "); walkForN1ql(args.get(1), sb);
                    sb.append(" ELSE "); walkForN1ql(args.get(2), sb);
                    sb.append(" END");
                }
            }

            // ---- Date extraction (Couchbase N1QL DATE_PART_STR) ----
            case "MONTH" -> { sb.append("DATE_PART_STR("); walkForN1ql(args.get(0), sb); sb.append(", 'month')"); }
            case "DAY" -> { sb.append("DATE_PART_STR("); walkForN1ql(args.get(0), sb); sb.append(", 'day')"); }
            case "YEAR" -> { sb.append("DATE_PART_STR("); walkForN1ql(args.get(0), sb); sb.append(", 'year')"); }
            case "HOUR" -> { sb.append("DATE_PART_STR("); walkForN1ql(args.get(0), sb); sb.append(", 'hour')"); }
            case "MINUTE" -> { sb.append("DATE_PART_STR("); walkForN1ql(args.get(0), sb); sb.append(", 'minute')"); }
            case "SECOND" -> { sb.append("DATE_PART_STR("); walkForN1ql(args.get(0), sb); sb.append(", 'second')"); }
            case "WEEKDAY" -> { sb.append("DAYOFWEEK("); walkForN1ql(args.get(0), sb); sb.append(")"); }

            // ---- Date construction ----
            case "TOMORROW" -> sb.append("DATE_ADD_STR(NOW_STR(), 1, 'day')");
            case "YESTERDAY" -> sb.append("DATE_ADD_STR(NOW_STR(), -1, 'day')");

            // ---- Math (Couchbase N1QL equivalents) ----
            case "ABS" -> { sb.append("ABS("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "SQRT" -> { sb.append("SQRT("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "POWER" -> { sb.append("POWER("); walkForN1ql(args.get(0), sb); sb.append(", "); walkForN1ql(args.get(1), sb); sb.append(")"); }
            case "EXP" -> { sb.append("EXP("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "LOG" -> { sb.append("LOG("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "LN" -> { sb.append("LN("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "COS" -> { sb.append("COS("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "SIN" -> { sb.append("SIN("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "TAN" -> { sb.append("TAN("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "PI" -> sb.append("PI()");
            case "INTEGER" -> { sb.append("FLOOR("); walkForN1ql(args.get(0), sb); sb.append(")"); }
            case "ROUND" -> { sb.append("ROUND("); walkForN1ql(args.get(0), sb); if (args.size() > 1) { sb.append(", "); walkForN1ql(args.get(1), sb); } sb.append(")"); }

            // ---- String (Couchbase N1QL equivalents) ----
            case "REPLACESUBSTRING" -> { sb.append("REPLACE("); walkForN1ql(args.get(0), sb); sb.append(", "); walkForN1ql(args.get(1), sb); sb.append(", "); walkForN1ql(args.get(2), sb); sb.append(")"); }
            case "REPEAT" -> { sb.append("REPEAT("); walkForN1ql(args.get(0), sb); sb.append(", "); walkForN1ql(args.get(1), sb); sb.append(")"); }
            case "NEWLINE" -> sb.append("CHR(10)");

            // ---- List ----
            case "ELEMENTS" -> { sb.append("ARRAY_LENGTH("); walkForN1ql(args.get(0), sb); sb.append(")"); }

            default -> sb.append(name.toLowerCase()).append("(") // unknown: pass through
                    .append(args.stream().map(Object::toString).reduce((a,b) -> a + ";" + b).orElse(""))
                    .append(")");
        }
    }

    private static String escapeBacktick(String s) {
        return s.replace("`", "\\`");
    }

    private static String formatNumber(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf((long) d);
        return String.valueOf(d);
    }

    // ---- removed: old regex-based translate(), replaceOpOutsideQuotes(),
    //      replaceNotOutsideQuotes(), translateAtFunctions()
}
