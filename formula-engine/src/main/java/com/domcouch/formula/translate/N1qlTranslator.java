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
public final class N1qlTranslator {

    private N1qlTranslator() {}

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
            case Expr.BinaryOp bo -> walkBinary(bo, sb, currentUserName);
            case Expr.FunctionCall fc -> walkFunction(fc, sb, currentUserName);
            case Expr.KeywordStatement ks -> walkExpr(ks.body(), sb, currentUserName);
            case Expr.Comment c -> { /* skip */ }
            default -> sb.append(expr.toString());
        }
    }

    private static void walkBinary(Expr.BinaryOp bo, StringBuilder sb, String currentUserName) {
        String op = bo.op();
        if ("&".equals(op)) {
            sb.append("(");
            walkExpr(bo.left(), sb, currentUserName);
            sb.append(" AND ");
            walkExpr(bo.right(), sb, currentUserName);
            sb.append(")");
        } else if ("|".equals(op)) {
            sb.append("(");
            walkExpr(bo.left(), sb, currentUserName);
            sb.append(" OR ");
            walkExpr(bo.right(), sb, currentUserName);
            sb.append(")");
        } else if ("!".equals(op) && bo.left() == null) {
            sb.append("NOT (");
            walkExpr(bo.right(), sb, currentUserName);
            sb.append(")");
        } else if ("=".equals(op)) {
            walkExpr(bo.left(), sb, currentUserName);
            sb.append(" = ");
            walkExpr(bo.right(), sb, currentUserName);
        } else if ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op)) {
            sb.append("(");
            walkExpr(bo.left(), sb, currentUserName);
            sb.append(" ").append(op).append(" ");
            walkExpr(bo.right(), sb, currentUserName);
            sb.append(")");
        } else {
            String n1qlOp = (op.equals("<>") || op.equals("><")) ? "!=" : op;
            walkExpr(bo.left(), sb, currentUserName);
            sb.append(" ").append(n1qlOp).append(" ");
            walkExpr(bo.right(), sb, currentUserName);
        }
    }

    private static void walkFunction(Expr.FunctionCall fc, StringBuilder sb, String currentUserName) {
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
                if (args.get(0) instanceof Expr.StringConst s) {
                    sb.append("doc.items.").append(escapeBacktick(s.value().toUpperCase())).append(".`values`[0]");
                } else walkExpr(args.get(0), sb, currentUserName);
                sb.append(" IS NOT MISSING");
            }
            case "ISNUMBER" -> { sb.append("IS_NUMBER("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "ISTEXT" -> { sb.append("IS_STRING("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "CONTAINS" -> { sb.append("CONTAINS("); walkExpr(args.get(0), sb, currentUserName); sb.append(", "); walkExpr(args.get(1), sb, currentUserName); sb.append(")"); }
            case "BEGINS" -> { walkExpr(args.get(0), sb, currentUserName); sb.append(" LIKE ("); walkExpr(args.get(1), sb, currentUserName); sb.append(" || '%')"); }
            case "ENDS" -> { walkExpr(args.get(0), sb, currentUserName); sb.append(" LIKE ('%' || "); walkExpr(args.get(1), sb, currentUserName); sb.append(")"); }
            case "ISMEMBER" -> { walkExpr(args.get(0), sb, currentUserName); sb.append(" IN "); walkExpr(args.get(1), sb, currentUserName); }
            case "ISNOTMEMBER" -> { walkExpr(args.get(0), sb, currentUserName); sb.append(" NOT IN "); walkExpr(args.get(1), sb, currentUserName); }
            case "LOWERCASE" -> { sb.append("LOWER("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "UPPERCASE" -> { sb.append("UPPER("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "TRIM" -> { sb.append("TRIM("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "LENGTH" -> { sb.append("LENGTH("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "LEFT" -> { sb.append("SUBSTR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", 0, "); walkExpr(args.get(1), sb, currentUserName); sb.append(")"); }
            case "RIGHT" -> { sb.append("SUBSTR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", LENGTH("); walkExpr(args.get(0), sb, currentUserName); sb.append(") - "); walkExpr(args.get(1), sb, currentUserName); sb.append(")"); }
            case "IF" -> { if (args.size() >= 3) { sb.append("CASE WHEN "); walkExpr(args.get(0), sb, currentUserName); sb.append(" THEN "); walkExpr(args.get(1), sb, currentUserName); sb.append(" ELSE "); walkExpr(args.get(2), sb, currentUserName); sb.append(" END"); } }
            case "MONTH" -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", 'month')"); }
            case "DAY" -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", 'day')"); }
            case "YEAR" -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", 'year')"); }
            case "HOUR" -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", 'hour')"); }
            case "MINUTE" -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", 'minute')"); }
            case "SECOND" -> { sb.append("DATE_PART_STR("); walkExpr(args.get(0), sb, currentUserName); sb.append(", 'second')"); }
            case "WEEKDAY" -> { sb.append("DAYOFWEEK("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "TOMORROW" -> sb.append("DATE_ADD_STR(NOW_STR(), 1, 'day')");
            case "YESTERDAY" -> sb.append("DATE_ADD_STR(NOW_STR(), -1, 'day')");
            case "ABS" -> { sb.append("ABS("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "SQRT" -> { sb.append("SQRT("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "POWER" -> { sb.append("POWER("); walkExpr(args.get(0), sb, currentUserName); sb.append(", "); walkExpr(args.get(1), sb, currentUserName); sb.append(")"); }
            case "EXP" -> { sb.append("EXP("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "LOG" -> { sb.append("LOG("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "LN" -> { sb.append("LN("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "COS" -> { sb.append("COS("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "SIN" -> { sb.append("SIN("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "TAN" -> { sb.append("TAN("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "PI" -> sb.append("PI()");
            case "INTEGER" -> { sb.append("FLOOR("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "ROUND" -> { sb.append("ROUND("); walkExpr(args.get(0), sb, currentUserName); if (args.size() > 1) { sb.append(", "); walkExpr(args.get(1), sb, currentUserName); } sb.append(")"); }
            case "REPLACESUBSTRING" -> { sb.append("REPLACE("); walkExpr(args.get(0), sb, currentUserName); sb.append(", "); walkExpr(args.get(1), sb, currentUserName); sb.append(", "); walkExpr(args.get(2), sb, currentUserName); sb.append(")"); }
            case "REPEAT" -> { sb.append("REPEAT("); walkExpr(args.get(0), sb, currentUserName); sb.append(", "); walkExpr(args.get(1), sb, currentUserName); sb.append(")"); }
            case "NEWLINE" -> sb.append("CHR(10)");
            case "ELEMENTS" -> { sb.append("ARRAY_LENGTH("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }

            // ---- New translations for custom views ----
            case "ISNEWDOC" -> sb.append("doc.unid IS MISSING");
            case "ISUNAVAILABLE" -> { walkExpr(args.get(0), sb, currentUserName); sb.append(" IS MISSING"); }
            case "LIKE" -> {
                walkExpr(args.get(0), sb, currentUserName); sb.append(" LIKE ");
                // Convert Domino pattern to SQL LIKE: _ stays _, % stays %
                // But need to handle escape char if provided
                walkExpr(args.get(1), sb, currentUserName);
                if (args.size() > 2) { sb.append(" ESCAPE "); walkExpr(args.get(2), sb, currentUserName); }
            }
            case "TEXT" -> { sb.append("TO_STRING("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "TEXTTONUMBER" -> { sb.append("TO_NUMBER("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
            case "DATE" -> { sb.append("DATE_STR("); walkExpr(args.get(0), sb, currentUserName); sb.append(" || '-' || "); walkExpr(args.get(1), sb, currentUserName); sb.append(" || '-' || "); walkExpr(args.get(2), sb, currentUserName); sb.append(")"); }
            case "ADJUST" -> {
                sb.append("DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(DATE_ADD_STR(");
                walkExpr(args.get(0), sb, currentUserName);
                sb.append(", "); walkExpr(args.get(1), sb, currentUserName); sb.append(", 'year')");
                sb.append(", "); walkExpr(args.get(2), sb, currentUserName); sb.append(", 'month')");
                sb.append(", "); walkExpr(args.get(3), sb, currentUserName); sb.append(", 'day')");
                sb.append(", "); walkExpr(args.get(4), sb, currentUserName); sb.append(", 'hour')");
                sb.append(", "); walkExpr(args.get(5), sb, currentUserName); sb.append(", 'minute')");
                sb.append(", "); walkExpr(args.get(6), sb, currentUserName); sb.append(", 'second')");
            }
            case "WORD" -> { sb.append("SPLIT("); walkExpr(args.get(0), sb, currentUserName); sb.append(", "); walkExpr(args.get(1), sb, currentUserName); sb.append(")["); walkExpr(args.get(2), sb, currentUserName); sb.append(" - 1]"); }
            case "ISNULL" -> { walkExpr(args.get(0), sb, currentUserName); sb.append(" IS NULL"); }
            case "EXPLODE" -> { sb.append("SPLIT("); walkExpr(args.get(0), sb, currentUserName); if (args.size() > 1) { sb.append(", "); walkExpr(args.get(1), sb, currentUserName); } else sb.append(", ' ,;'"); sb.append(")"); }
            case "IMPLODE" -> { sb.append("ARRAY_JOIN("); walkExpr(args.get(0), sb, currentUserName); if (args.size() > 1) { sb.append(", "); walkExpr(args.get(1), sb, currentUserName); } else sb.append(", ' '"); sb.append(")"); }
            case "COUNT" -> { sb.append("ARRAY_LENGTH("); walkExpr(args.get(0), sb, currentUserName); sb.append(")"); }
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
