package com.domcouch.formula.translate;

import com.domcouch.formula.Expr;
import com.domcouch.formula.Lexer;
import com.domcouch.formula.Parser;
import com.domcouch.formula.Token;

import java.util.List;

/**
 * Walks a formula AST and emits a N1QL WHERE clause.
 * Handles field references, logical/arithmetic operators,
 * and common @Functions used in view selection formulas.
 */
final class N1qlTranslator {

    private N1qlTranslator() {}

    /**
     * Translate a Domino value formula (column expression) to N1QL.
     * Handles string concatenation (+) as N1QL || and common value @Functions.
     * @param formula the formula string (e.g., {@code "FirstName + \" \" + LastName"})
     * @param currentUserName the user for @UserName resolution
     * @return N1QL value expression, or null if input is null
     */
    public static String translateValue(String formula, String currentUserName) {
        if (formula == null) return null;
        List<Token> tokens = Lexer.tokenize(formula);
        List<Expr> stmts = new Parser(tokens).parse();
        if (stmts.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        walkExpr(stmts.getFirst(), sb, currentUserName, true);
        return sb.toString().trim();
    }

    /**
     * Translate a Domino selection formula to N1QL.
     * @param formula the formula string
     * @param currentUserName the user for @UserName resolution
     * @return N1QL boolean expression, or null if input is null
     */
    public static String translate(String formula, String currentUserName) {
        if (formula == null) return null;
        if (formula.contains("doc.items.")) return formula;

        List<Token> tokens = Lexer.tokenize(formula);
        List<Expr> stmts = new Parser(tokens).parse();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stmts.size(); i++) {
            Expr stmt = stmts.get(i);
            if (stmt instanceof Expr.KeywordStatement ks && "SELECT".equals(ks.keyword())) {
                walkExpr(ks.body(), sb, currentUserName);
            } else {
                walkExpr(stmt, sb, currentUserName);
            }
            if (i < stmts.size() - 1 && isExpression(stmt)) {
                sb.append(" AND ");
            }
        }
        return sb.toString().trim();
    }

    private static boolean isExpression(Expr e) {
        return !(e instanceof Expr.KeywordStatement) && !(e instanceof Expr.Comment)
                && !(e instanceof Expr.DeleteField);
    }

    private static void walkExpr(Expr expr, StringBuilder sb, String currentUserName) {
        walkExpr(expr, sb, currentUserName, false);
    }

    /** Emit a field reference with .values (full array, not .values[0]) for array functions. */
    private static void walkFieldRef(Expr.Variable v, StringBuilder sb) {
        sb.append("doc.items.").append(escapeBacktick(v.name())).append(".`values`");
    }

    /** Emit array-aware argument: .values for Variables, .values[0] for others. */
    private static void walkArrayArg(Expr arg, StringBuilder sb) {
        if (arg instanceof Expr.Variable v) walkFieldRef(v, sb);
        else walkExpr(arg, sb, ""); // literal/expression — fallback
    }

    private static void walkExpr(Expr expr, StringBuilder sb, String currentUserName, boolean valueMode) {
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
            case Expr.BinaryOp bo -> walkBinary(bo, sb, currentUserName, valueMode);
            case Expr.FunctionCall fc -> walkFunction(fc, sb, currentUserName, valueMode);
            case Expr.KeywordStatement ks -> walkExpr(ks.body(), sb, currentUserName, valueMode);
            case Expr.Comment c -> { /* skip */ }
            default -> sb.append(expr.toString());
        }
    }

    private static void walkBinary(Expr.BinaryOp bo, StringBuilder sb, String currentUserName) {
        walkBinary(bo, sb, currentUserName, false);
    }

    private static void walkBinary(Expr.BinaryOp bo, StringBuilder sb, String currentUserName, boolean valueMode) {
        String op = bo.op();
        if ("&".equals(op)) {
            sb.append("(");
            walkExpr(bo.left(), sb, currentUserName, valueMode);
            sb.append(" AND ");
            walkExpr(bo.right(), sb, currentUserName, valueMode);
            sb.append(")");
        } else if ("|".equals(op)) {
            sb.append("(");
            walkExpr(bo.left(), sb, currentUserName, valueMode);
            sb.append(" OR ");
            walkExpr(bo.right(), sb, currentUserName, valueMode);
            sb.append(")");
        } else if ("!".equals(op) && bo.left() == null) {
            sb.append("NOT (");
            walkExpr(bo.right(), sb, currentUserName, valueMode);
            sb.append(")");
        } else if ("=".equals(op)) {
            walkExpr(bo.left(), sb, currentUserName, valueMode);
            sb.append(" = ");
            walkExpr(bo.right(), sb, currentUserName, valueMode);
        } else if ("+".equals(op)) {
            String n1qlOp = valueMode ? "||" : "+";
            sb.append("(");
            walkExpr(bo.left(), sb, currentUserName, valueMode);
            sb.append(" ").append(n1qlOp).append(" ");
            walkExpr(bo.right(), sb, currentUserName, valueMode);
            sb.append(")");
        } else if ("-".equals(op) || "*".equals(op) || "/".equals(op)) {
            sb.append("(");
            walkExpr(bo.left(), sb, currentUserName, valueMode);
            sb.append(" ").append(op).append(" ");
            walkExpr(bo.right(), sb, currentUserName, valueMode);
            sb.append(")");
        } else {
            String n1qlOp = (op.equals("<>") || op.equals("><")) ? "!=" : op;
            walkExpr(bo.left(), sb, currentUserName, valueMode);
            sb.append(" ").append(n1qlOp).append(" ");
            walkExpr(bo.right(), sb, currentUserName, valueMode);
        }
    }

    private static void walkFunction(Expr.FunctionCall fc, StringBuilder sb, String currentUserName, boolean valueMode) {
        String name = fc.name();
        List<Expr> args = fc.args();
        switch (name) {
            case FunctionNames.ALL, FunctionNames.TRUE, FunctionNames.SUCCESS, FunctionNames.YES -> sb.append("true");
            case FunctionNames.FALSE, FunctionNames.NO -> sb.append("false");
            case FunctionNames.ISRESPONSEDOC -> sb.append("doc.parentUNID IS NOT MISSING");
            case FunctionNames.TODAY, FunctionNames.NOW -> sb.append("NOW_STR()");
            case FunctionNames.CREATED -> sb.append("doc.created");
            case FunctionNames.MODIFIED -> sb.append("doc.lastModified");
            case FunctionNames.USERNAME -> sb.append("'").append(currentUserName.replace("'", "''")).append("'");
            case FunctionNames.ISAVAILABLE -> {
                if (args.get(0) instanceof Expr.StringConst s) {
                    sb.append("doc.items.").append(escapeBacktick(s.value().toUpperCase())).append(".`values`[0]");
                } else walkExpr(args.get(0), sb, currentUserName, valueMode);
                sb.append(" IS NOT MISSING");
            }
            case FunctionNames.ISNUMBER -> { sb.append("IS_NUMBER("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.ISTEXT -> { sb.append("IS_STRING("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.CONTAINS -> { sb.append("CONTAINS("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.BEGINS -> { walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" LIKE ("); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(" || '%')"); }
            case FunctionNames.ENDS -> { walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" LIKE ('%' || "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.ISMEMBER -> { walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" IN "); walkExpr(args.get(1), sb, currentUserName, valueMode); }
            case FunctionNames.ISNOTMEMBER -> { walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" NOT IN "); walkExpr(args.get(1), sb, currentUserName, valueMode); }
            case FunctionNames.LOWERCASE -> { sb.append("LOWER("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.UPPERCASE -> { sb.append("UPPER("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.TRIM -> { sb.append("TRIM("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.LENGTH -> { sb.append("LENGTH("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.LEFT -> { sb.append("SUBSTR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", 0, "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.RIGHT -> { sb.append("SUBSTR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", LENGTH("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(") - "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.IF -> { if (args.size() >= 3) { sb.append("CASE WHEN "); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" THEN "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(" ELSE "); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(" END"); } }
            case FunctionNames.MONTH -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", 'month')"); }
            case FunctionNames.DAY -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", 'day')"); }
            case FunctionNames.YEAR -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", 'year')"); }
            case FunctionNames.HOUR -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", 'hour')"); }
            case FunctionNames.MINUTE -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", 'minute')"); }
            case FunctionNames.SECOND -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", 'second')"); }
            case FunctionNames.WEEKDAY -> { sb.append("DAYOFWEEK("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.TOMORROW -> sb.append("DATE_ADD_STR(NOW_STR(), 1, 'day')");
            case FunctionNames.YESTERDAY -> sb.append("DATE_ADD_STR(NOW_STR(), -1, 'day')");
            case FunctionNames.ABS -> { sb.append("ABS("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.SQRT -> { sb.append("SQRT("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.POWER -> { sb.append("POWER("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.EXP -> { sb.append("EXP("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.LOG -> { sb.append("LOG("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.LN -> { sb.append("LN("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.COS -> { sb.append("COS("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.SIN -> { sb.append("SIN("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.TAN -> { sb.append("TAN("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.PI -> sb.append("PI()");
            case FunctionNames.RANDOM -> {
                if (args.isEmpty()) sb.append("RANDOM()");
                else { sb.append("RANDOM("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            }
            case FunctionNames.INTEGER -> { sb.append("FLOOR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.ROUND -> { sb.append("ROUND("); walkExpr(args.get(0), sb, currentUserName, valueMode); if (args.size() > 1) { sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); } sb.append(")"); }
            case FunctionNames.REPLACESUBSTRING -> { sb.append("REPLACE("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.REPEAT -> { sb.append("REPEAT("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.NEWLINE -> sb.append("'\\n'");
            case FunctionNames.ELEMENTS -> { sb.append("ARRAY_LENGTH("); walkArrayArg(args.get(0), sb); sb.append(")"); }

            // ---- New translations for custom views ----
            case FunctionNames.ISNEWDOC -> sb.append("doc.unid IS MISSING");
            case FunctionNames.ISUNAVAILABLE -> { walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" IS MISSING"); }
            case FunctionNames.LIKE -> {
                walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" LIKE ");
                // Convert Domino pattern to SQL LIKE: _ stays _, % stays %
                // But need to handle escape char if provided
                walkExpr(args.get(1), sb, currentUserName, valueMode);
                if (args.size() > 2) { sb.append(" ESCAPE "); walkExpr(args.get(2), sb, currentUserName, valueMode); }
            }
            case FunctionNames.TEXT -> { sb.append("TO_STRING("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.TEXTTONUMBER -> { sb.append("TO_NUMBER("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.DATE -> { sb.append("DATE_STR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" || '-' || "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(" || '-' || "); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.ADJUST -> {
                sb.append("DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(");
                walkExpr(args.get(0), sb, currentUserName, valueMode);
                sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(", 'year')");
                sb.append(", "); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(", 'month')");
                sb.append(", "); walkExpr(args.get(3), sb, currentUserName, valueMode); sb.append(", 'day')");
                sb.append(", "); walkExpr(args.get(4), sb, currentUserName, valueMode); sb.append(", 'hour')");
                sb.append(", "); walkExpr(args.get(5), sb, currentUserName, valueMode); sb.append(", 'minute')");
                sb.append(", "); walkExpr(args.get(6), sb, currentUserName, valueMode); sb.append(", 'second')");
            }
            case FunctionNames.WORD -> { sb.append("SPLIT("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")["); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(" - 1]"); }
            case FunctionNames.ISNULL -> { walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" IS NULL"); }
            case FunctionNames.EXPLODE -> { sb.append("SPLIT("); walkExpr(args.get(0), sb, currentUserName, valueMode); if (args.size() > 1) { sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); } else sb.append(", ' ,;'"); sb.append(")"); }
            case FunctionNames.IMPLODE -> { sb.append("ARRAY_JOIN("); walkArrayArg(args.get(0), sb); if (args.size() > 1) { sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); } else sb.append(", ' '"); sb.append(")"); }
            case FunctionNames.COUNT -> { sb.append("ARRAY_LENGTH("); walkArrayArg(args.get(0), sb); sb.append(")"); }
            case FunctionNames.UNIQUE -> { sb.append("ARRAY_DISTINCT("); walkArrayArg(args.get(0), sb); sb.append(")"); }
            case FunctionNames.SORT -> { sb.append("ARRAY_SORT("); walkArrayArg(args.get(0), sb); sb.append(")"); }
            case FunctionNames.MEMBER -> {
                sb.append("ARRAY_POSITION("); walkArrayArg(args.get(1), sb);
                sb.append(", "); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(") + 1");
            }

            // ---- Value-expression extensions ----
            case FunctionNames.MODULO -> { sb.append("("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(" % "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.SIGN -> { sb.append("SIGN("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.MIDDLE -> { sb.append("SUBSTR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(" + 1, "); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.MIDDLEBACK -> { sb.append("SUBSTR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", LENGTH("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(") - "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(" - "); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(" + 1, "); walkExpr(args.get(2), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.MAX -> { sb.append("GREATEST("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.MIN -> { sb.append("LEAST("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(", "); walkExpr(args.get(1), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.PROPERCASE -> { sb.append("INITCAP("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            case FunctionNames.CHAR -> { sb.append("CHR("); walkExpr(args.get(0), sb, currentUserName, valueMode); sb.append(")"); }
            default -> sb.append(name.toLowerCase()).append("(")
                    .append(args.stream().map(Object::toString).reduce((a,b) -> a + ";" + b).orElse(""))
                    .append(")");
        }
    }

    private static String escapeBacktick(String s) { return s.replace("`", "\\`"); }

    private static String formatNumber(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
