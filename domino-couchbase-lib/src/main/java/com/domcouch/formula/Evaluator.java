package com.domcouch.formula;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Walks an {@link Expr} AST and evaluates it against a {@link FormulaContext}.
 * <p>
 * Handles constants, variables, binary operators, @Function dispatch,
 * type coercion, assignment, and control flow ({@code @If}, {@code @Do}, {@code @Return}).
 */
public class Evaluator {

    private final Map<String, FunctionHandler> functions;
    private final String currentUserName;
    private Map<String, Object> tempScope; // per-evaluation temp variables
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter
            .ofPattern("MM/dd/yyyy hh:mm:ss a").withZone(ZoneId.systemDefault());

    /** Sentinel value for @Error / @IsError. */
    static final Object ERROR_VALUE = new Object();

    /** Create an Evaluator with the default user ("Anonymous") and built-in functions. */
    public Evaluator() {
        this("Anonymous");
    }

    /** Create an Evaluator for a specific user (for @UserName). */
    public Evaluator(String currentUserName) {
        this.currentUserName = currentUserName != null ? currentUserName : "Anonymous";
        this.functions = new HashMap<>();
        registerBuiltins();
    }

    /** Parse and evaluate a single formula. Convenience for testing. */
    public Object evalExpr(String formula, FormulaContext ctx) {
        this.tempScope = new HashMap<>();
        List<Token> tokens = Lexer.tokenize(formula);
        List<Expr> stmts = new Parser(tokens).parse();
        Object result = "";
        try {
            for (Expr stmt : stmts) {
                result = eval(stmt, ctx);
            }
        } catch (ReturnValue rv) {
            return rv.value;
        }
        return result;
    }

    /** Reset the temp variable scope for a new evaluation (called by FormulaTranslator). */
    void initTempScope() {
        this.tempScope = new HashMap<>();
    }

    /** Evaluate a single expression in the given context. */
    public Object eval(Expr expr, FormulaContext ctx) {
        try {
            return switch (expr) {
                case Expr.Variable v -> {
                    // Check temp scope first, then context
                    if (tempScope != null && tempScope.containsKey(v.name())) {
                        Object tv = tempScope.get(v.name());
                        yield tv != null ? tv : "";
                    }
                    Object val = ctx.resolve(v.name());
                    yield val != null ? val : "";
                }
                case Expr.StringConst s -> s.value();
                case Expr.NumberConst n -> n.value();
                case Expr.DateTimeConst d -> d.raw(); // raw string, parsed on use
                case Expr.KeywordExpr kw -> kw.value();
                case Expr.FunctionCall fc -> callFunction(fc.name(), fc.args(), ctx);
                case Expr.BinaryOp bo -> evalBinary(bo, ctx);
                case Expr.Assignment a -> evalAssign(a, ctx, false);
                case Expr.FieldAssign fa -> evalAssign(fa, ctx, "FIELD");
                case Expr.DefaultAssign da -> evalDefaultAssign(da, ctx);
                case Expr.EnvironmentAssign ea -> {
                    Object val = eval(ea.value(), ctx);
                    // no-op: just return the value
                    yield val;
                }
                case Expr.KeywordStatement ks -> eval(ks.body(), ctx);
                case Expr.DeleteField df -> {
                    ctx.deleteField(((Expr.Variable) df.target()).name());
                    yield "";
                }
                case Expr.Comment c -> "";
            };
        } catch (ReturnValue rv) {
            throw rv; // propagate to top-level handler
        }
    }

    // ---- Binary operators ----

    private Object evalBinary(Expr.BinaryOp bo, FormulaContext ctx) {
        // Unary operator (null left)
        if (bo.left() == null) {
            Object right = eval(bo.right(), ctx);
            return switch (bo.op()) {
                case "!" -> boolToNum(!isTruthy(right));
                case "-" -> -toNumber(right);
                default -> right; // unary + has no effect
            };
        }

        // Subscript
        if (bo.op().equals("[]")) {
            Object list = eval(bo.left(), ctx);
            Object idx = eval(bo.right(), ctx);
            return subscript(list, idx);
        }

        // List constructor
        if (bo.op().equals(":")) {
            Object left = eval(bo.left(), ctx);
            Object right = eval(bo.right(), ctx);
            return listCons(left, right);
        }

        // Binary operators with pair-wise list semantics
        Object left = eval(bo.left(), ctx);
        Object right = eval(bo.right(), ctx);

        return switch (bo.op()) {
            case "+" -> pairwise(left, right, Evaluator::add);
            case "-" -> pairwise(left, right, Evaluator::subtract);
            case "*" -> pairwise(left, right, Evaluator::multiply);
            case "/" -> pairwise(left, right, Evaluator::divide);
            case "=" -> boolToNum(anyPairwise(left, right, (a, b) -> compare(a, b) == 0));
            case "<>", "!=", "><" -> boolToNum(anyPairwise(left, right, (a, b) -> compare(a, b) != 0));
            case ">" -> boolToNum(anyPairwise(left, right, (a, b) -> compare(a, b) > 0));
            case "<" -> boolToNum(anyPairwise(left, right, (a, b) -> compare(a, b) < 0));
            case ">=" -> boolToNum(anyPairwise(left, right, (a, b) -> compare(a, b) >= 0));
            case "<=" -> boolToNum(anyPairwise(left, right, (a, b) -> compare(a, b) <= 0));

            // Permuted operators
            case "*+" -> permuted(left, right, Evaluator::add);
            case "*-" -> permuted(left, right, Evaluator::subtract);
            case "**" -> permuted(left, right, Evaluator::multiply);
            case "*/" -> permuted(left, right, Evaluator::divide);
            case "*>" -> boolToNum(anyPermuted(left, right, (a, b) -> compare(a, b) > 0));
            case "*<" -> boolToNum(anyPermuted(left, right, (a, b) -> compare(a, b) < 0));
            case "*>=" -> boolToNum(anyPermuted(left, right, (a, b) -> compare(a, b) >= 0));
            case "*<=" -> boolToNum(anyPermuted(left, right, (a, b) -> compare(a, b) <= 0));
            case "*=" -> boolToNum(anyPermuted(left, right, (a, b) -> compare(a, b) == 0));
            case "*!=" -> boolToNum(anyPermuted(left, right, (a, b) -> compare(a, b) != 0));

            case "&" -> boolToNum(isTruthy(left) && isTruthy(right));
            case "|" -> boolToNum(isTruthy(left) || isTruthy(right));
            default -> throw new FormulaParseException(4502,
                    "Unknown operator: " + bo.op(), -1);
        };
    }

    // ---- Pair-wise and permuted list operations ----

    @FunctionalInterface
    interface BinaryOp { Object apply(Object a, Object b); }

    /** Pair-wise: element-by-element, repeat last of shorter list. */
    static Object pairwise(Object left, Object right, BinaryOp op) {
        List<Object> l1 = toList(left);
        List<Object> l2 = toList(right);
        int size = Math.max(l1.size(), l2.size());
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Object a = l1.get(Math.min(i, l1.size() - 1));
            Object b = l2.get(Math.min(i, l2.size() - 1));
            result.add(op.apply(a, b));
        }
        return result.size() == 1 ? result.get(0) : result;
    }

    /** Permuted: every combination of elements from left × right. */
    static Object permuted(Object left, Object right, BinaryOp op) {
        List<Object> l1 = toList(left);
        List<Object> l2 = toList(right);
        List<Object> result = new ArrayList<>();
        for (Object a : l1) for (Object b : l2) result.add(op.apply(a, b));
        return result.size() == 1 ? result.get(0) : result;
    }

    /** Returns true if any pair-wise comparison yields truthy. */
    static boolean anyPairwise(Object left, Object right, BinaryOp op) {
        List<Object> l1 = toList(left);
        List<Object> l2 = toList(right);
        int size = Math.max(l1.size(), l2.size());
        for (int i = 0; i < size; i++) {
            Object a = l1.get(Math.min(i, l1.size() - 1));
            Object b = l2.get(Math.min(i, l2.size() - 1));
            if (isTruthy(op.apply(a, b))) return true;
        }
        return false;
    }

    /** Returns true if any permuted comparison yields truthy. */
    static boolean anyPermuted(Object left, Object right, BinaryOp op) {
        List<Object> l1 = toList(left);
        List<Object> l2 = toList(right);
        for (Object a : l1) for (Object b : l2) if (isTruthy(op.apply(a, b))) return true;
        return false;
    }

    // ---- Assignment ----

    private Object evalAssign(Expr.Assignment a, FormulaContext ctx, boolean isField) {
        return evalAssign(a.target(), a.value(), ctx, isField ? "FIELD" : null);
    }

    private Object evalAssign(Expr.FieldAssign fa, FormulaContext ctx, String kind) {
        return evalAssign(fa.target(), fa.value(), ctx, kind);
    }

    private Object evalAssign(Expr target, Expr value, FormulaContext ctx, String kind) {
        String name = ((Expr.Variable) target).name();
        Object val = eval(value, ctx);
        if (val instanceof Expr.DeleteField df) {
            ctx.deleteField(name);
            return "";
        }
        if ("FIELD".equals(kind)) {
            ctx.setField(name, val);
        } else {
            if (tempScope != null) tempScope.put(name, val != null ? val : "");
        }
        return val;
    }

    private Object evalDefaultAssign(Expr.DefaultAssign da, FormulaContext ctx) {
        String name = ((Expr.Variable) da.target()).name();
        Object existing = ctx.resolve(name);
        if (existing != null && !existing.toString().isEmpty() && !"0".equals(String.valueOf(existing))) {
            return existing;
        }
        return eval(da.value(), ctx);
    }

    // ---- @Function dispatch ----

    private Object callFunction(String name, List<Expr> args, FormulaContext ctx) {
        FunctionHandler handler = functions.get(name);
        if (handler != null) {
            return handler.call(this, args, ctx);
        }
        // Unknown function → return "" (Domino-style error suppression)
        return "";
    }

    // ---- Arithmetic helpers ----

    private static Object add(Object left, Object right) {
        if (left instanceof String || right instanceof String)
            return toString(left) + toString(right);
        return toNumber(left) + toNumber(right);
    }

    private static Object subtract(Object left, Object right) {
        return toNumber(left) - toNumber(right);
    }

    private static Object multiply(Object left, Object right) {
        return toNumber(left) * toNumber(right);
    }

    private static Object divide(Object left, Object right) {
        double r = toNumber(right);
        if (r == 0.0) return 0.0;
        return toNumber(left) / r;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object left, Object right) {
        if (left instanceof Number && right instanceof Number)
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        if (left instanceof Comparable && right instanceof Comparable)
            return ((Comparable) left).compareTo(right);
        return toString(left).compareTo(toString(right));
    }

    // ---- Type coercion ----

    static double toNumber(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s && !s.isEmpty()) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }

    static boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Number n) return n.doubleValue() != 0.0;
        if (val instanceof String s) return !s.isEmpty();
        if (val instanceof Boolean b) return b;
        return true;
    }

    static double boolToNum(boolean b) { return b ? 1.0 : 0.0; }

    // ---- Date parsing ----

    private static final java.time.format.DateTimeFormatter[] DATE_PARSERS = {
            java.time.format.DateTimeFormatter.ISO_DATE_TIME,
            java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy[ hh:mm:ss a]"),
            java.time.format.DateTimeFormatter.ofPattern("M/d/yy[ hh:mm:ss a]"),
            java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy['T'HH:mm:ss'Z']"),
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
    };

    /** Extract a date field (month, day, year) from a date string or object. */
    private static int extractDateField(Object val, java.time.temporal.ChronoField field) {
        if (val == null) return 0;
        String s = toString(val).trim();
        if (s.isEmpty()) return 0;
        for (var fmt : DATE_PARSERS) {
            try {
                var parsed = fmt.parseBest(s,
                        java.time.ZonedDateTime::from,
                        java.time.LocalDateTime::from,
                        java.time.LocalDate::from);
                return ((java.time.temporal.TemporalAccessor) parsed).get(field);
            } catch (Exception e) { /* try next format */ }
        }
        return 0;
    }

    /** Parse a date string to ZonedDateTime, returning null on failure. */
    private static java.time.ZonedDateTime parseDateToZoned(String s) {
        for (var fmt : DATE_PARSERS) {
            try {
                var parsed = fmt.parseBest(s,
                        java.time.ZonedDateTime::from,
                        java.time.LocalDateTime::from,
                        java.time.LocalDate::from);
                if (parsed instanceof java.time.ZonedDateTime zdt) return zdt;
                if (parsed instanceof java.time.LocalDateTime ldt)
                    return ldt.atZone(java.time.ZoneId.systemDefault());
                if (parsed instanceof java.time.LocalDate ld)
                    return ld.atStartOfDay(java.time.ZoneId.systemDefault());
            } catch (Exception e) { /* try next */ }
        }
        return null;
    }

    /** Parse a date string to LocalDate, returning null on failure. */
    private static java.time.LocalDate parseDate(String s) {
        java.time.ZonedDateTime zdt = parseDateToZoned(s);
        return zdt != null ? zdt.toLocalDate() : null;
    }

    static boolean isFalsy(Object val) { return !isTruthy(val); }

    /** Check if a string represents a valid number. */
    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    static String toString(Object val) {
        if (val == null) return "";
        if (val instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf((long) d.doubleValue());
        return val.toString();
    }

    // ---- List operations ----

    @SuppressWarnings("unchecked")
    private static List<Object> toList(Object val) {
        if (val instanceof List l) return l;
        return List.of(val);
    }

    /** Map a single-arg math function over a value or list. */
    private static Object map1(Evaluator ev, List<Expr> args, FormulaContext ctx,
                                java.util.function.DoubleUnaryOperator fn) {
        Object val = ev.eval(args.get(0), ctx);
        List<Object> sources = toList(val);
        List<Object> result = new ArrayList<>();
        for (Object src : sources) result.add(fn.applyAsDouble(toNumber(src)));
        return result.size() == 1 ? result.get(0) : result;
    }

    /** Map a dual-arg math function pair-wise over value(s) or list(s). */
    private static Object map2(Evaluator ev, List<Expr> args, FormulaContext ctx,
                                java.util.function.DoubleBinaryOperator fn) {
        Object val1 = ev.eval(args.get(0), ctx);
        Object val2 = ev.eval(args.get(1), ctx);
        List<Object> list1 = toList(val1);
        List<Object> list2 = toList(val2);
        int size = Math.max(list1.size(), list2.size());
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            double a = toNumber(list1.get(Math.min(i, list1.size() - 1)));
            double b = toNumber(list2.get(Math.min(i, list2.size() - 1)));
            result.add(fn.applyAsDouble(a, b));
        }
        return result.size() == 1 ? result.get(0) : result;
    }

    /** Check if any pair (a,b) from two values (or lists) matches the predicate. */
    private static boolean anyPairMatch(Object a, Object b,
                                         java.util.function.BiPredicate<String, String> pred) {
        for (Object sa : toList(a)) {
            for (Object sb : toList(b)) {
                if (pred.test(toString(sa), toString(sb))) return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Object subscript(Object list, Object idx) {
        int i = (int) Math.round(toNumber(idx));
        if (list instanceof List l) {
            if (i < 1 || i > l.size()) return "";
            return l.get(i - 1); // 1-based
        }
        if (i == 1) return list; // scalar treated as 1-element list
        return "";
    }

    @SuppressWarnings("unchecked")
    private static Object listCons(Object left, Object right) {
        List<Object> result = new ArrayList<>();
        if (left instanceof List l) result.addAll(l);
        else result.add(left);
        if (right instanceof List l) result.addAll(l);
        else result.add(right);
        return result;
    }

    /**
     * Convert a Domino @Matches pattern to a Java regex Pattern.
     * Handles: ? → ., * → .*, {ABC} → [ABC], {A-F} → [A-F].
     * Simple characters are case-insensitive; {}-enclosed chars are case-sensitive.
     */
    private static java.util.regex.Pattern dominoPatternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '?') { regex.append('.'); i++; }
            else if (c == '*') { regex.append(".*"); i++; }
            else if (c == '{') {
                int end = pattern.indexOf('}', i);
                if (end < 0) { regex.append("\\{"); i++; }
                else {
                    String inner = pattern.substring(i + 1, end);
                    regex.append('[').append(inner).append(']');
                    i = end + 1;
                }
            } else if (c == '\\' && i + 1 < pattern.length()) {
                regex.append("\\Q").append(pattern.charAt(i + 1)).append("\\E");
                i += 2;
            } else if ("!|&".indexOf(c) >= 0) {
                regex.append(".*"); // skip unimplemented operators for now
                i++;
            } else {
                regex.append(Character.toLowerCase(c));
                i++;
            }
        }
        return java.util.regex.Pattern.compile(regex.toString(), java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    /** Format a number according to a Domino format string (always US locale). */
    private static String formatNumber(double value, String format) {
        java.util.Locale us = java.util.Locale.US;
        String upper = format.toUpperCase();
        int decimals = 2;
        if (upper.matches(".*[0-9]+")) {
            try { decimals = Integer.parseInt(upper.replaceAll("[^0-9]", "")); } catch (Exception e) {}
        }
        if (upper.contains("S")) return String.format(us, "%." + decimals + "E", value);
        if (upper.contains("C")) return "$" + String.format(us, "%." + decimals + "f", value);
        if (upper.contains("%")) return String.format(us, "%." + decimals + "f", value * 100) + "%";
        String result = String.format(us, "%." + decimals + "f", value);
        if (upper.contains(",")) {
            String[] parts = result.split("\\.");
            String intPart = String.format(us, "%,d", (long) Math.abs(value));
            if (value < 0) intPart = "-" + intPart;
            result = parts.length > 1 ? intPart + "." + parts[1] : intPart;
        }
        if (upper.contains("(") && upper.contains(")") && value < 0)
            result = "(" + result.substring(1) + ")";
        return result;
    }

    // ---- Built-in function registration ----

    private void registerBuiltins() {
        // Math functions
        functions.put("ABS", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add(Math.abs(toNumber(src)));
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("ACOS", (ev, args, ctx) -> map1(ev, args, ctx, Math::acos));
        functions.put("ASIN", (ev, args, ctx) -> map1(ev, args, ctx, Math::asin));
        functions.put("ATAN", (ev, args, ctx) -> map1(ev, args, ctx, Math::atan));
        functions.put("ATAN2", (ev, args, ctx) -> map2(ev, args, ctx, Math::atan2));
        functions.put("COS", (ev, args, ctx) -> map1(ev, args, ctx, Math::cos));
        functions.put("SIN", (ev, args, ctx) -> map1(ev, args, ctx, Math::sin));
        functions.put("TAN", (ev, args, ctx) -> map1(ev, args, ctx, Math::tan));
        functions.put("EXP", (ev, args, ctx) -> map1(ev, args, ctx, Math::exp));
        functions.put("LOG", (ev, args, ctx) -> map1(ev, args, ctx, Math::log10));
        functions.put("SQRT", (ev, args, ctx) -> map1(ev, args, ctx, Math::sqrt));
        functions.put("PI", (ev, args, ctx) -> Math.PI);
        functions.put("POWER", (ev, args, ctx) -> map2(ev, args, ctx, Math::pow));
        functions.put("INTEGER", (ev, args, ctx) -> map1(ev, args, ctx, v -> (double) (long) v));
        functions.put("ROUND", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            double factor = args.size() > 1 ? toNumber(ev.eval(args.get(1), ctx)) : 1.0;
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                double v = toNumber(src);
                result.add(Math.round(v / factor) * factor);
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // Calendar functions
        functions.put("BUSINESSDAYS", (ev, args, ctx) -> {
            Object starts = ev.eval(args.get(0), ctx);
            Object ends = ev.eval(args.get(1), ctx);
            List<Object> startList = toList(starts);
            List<Object> endList = toList(ends);
            int size = Math.max(startList.size(), endList.size());
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                java.time.LocalDate s = parseDate(toString(startList.get(Math.min(i, startList.size() - 1))));
                java.time.LocalDate e = parseDate(toString(endList.get(Math.min(i, endList.size() - 1))));
                if (s == null || e == null || e.isBefore(s)) { result.add(-1.0); continue; }
                long days = 0;
                for (java.time.LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) days++;
                result.add((double) days);
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // String functions
        functions.put("ASCII", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            boolean allInRange = args.size() > 1;
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src);
                StringBuilder sb = new StringBuilder();
                for (char c : s.toCharArray()) {
                    sb.append(c >= 32 && c <= 127 ? c : '?');
                }
                String converted = sb.toString();
                if (allInRange && converted.indexOf('?') >= 0) converted = "";
                result.add(converted);
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("CHAR", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            java.nio.charset.Charset cp850 = java.nio.charset.Charset.forName("Cp850");
            for (Object src : sources) {
                int code = (int) toNumber(src) & 0xFF;
                result.add(new String(new byte[]{(byte) code}, cp850));
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("COMPARE", (ev, args, ctx) -> {
            List<Object> list1 = toList(ev.eval(args.get(0), ctx));
            List<Object> list2 = toList(ev.eval(args.get(1), ctx));
            int size = Math.max(list1.size(), list2.size());
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String s1 = toString(list1.get(Math.min(i, list1.size() - 1)));
                String s2 = toString(list2.get(Math.min(i, list2.size() - 1)));
                int cmp = s1.compareTo(s2);
                result.add(cmp < 0 ? -1.0 : cmp > 0 ? 1.0 : 0.0);
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("EXPLODE", (ev, args, ctx) -> {
            String s = toString(ev.eval(args.get(0), ctx));
            String sep = args.size() > 1 ? toString(ev.eval(args.get(1), ctx)) : " ,;";
            if (sep.isEmpty()) sep = " ,;";
            // Build regex character class from separators
            StringBuilder regex = new StringBuilder("[");
            for (char c : sep.toCharArray()) regex.append("\\").append(c);
            regex.append("]+");
            String[] parts = s.split(regex.toString());
            java.util.List<Object> result = new java.util.ArrayList<>();
            for (String p : parts) if (!p.isEmpty()) result.add(p);
            return result.isEmpty() ? "" : result.size() == 1 ? result.get(0) : result;
        });
        functions.put("TRIM", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src);
                // Collapse consecutive spaces, then trim
                String trimmed = s.replaceAll("  +", " ").trim();
                if (!trimmed.isEmpty()) result.add(trimmed);
            }
            if (result.isEmpty()) return "";
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("UPPERCASE", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add(toString(src).toUpperCase());
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("LOWERCASE", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add(toString(src).toLowerCase());
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("LENGTH", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add((double) toString(src).length());
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("LEFT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            Object arg2 = ev.eval(args.get(1), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src);
                if (arg2 instanceof Number || (arg2 instanceof String && isNumeric((String) arg2))) {
                    int n = (int) toNumber(arg2);
                    result.add(n < 0 ? s : s.substring(0, Math.min(n, s.length())));
                } else {
                    String sub = toString(arg2);
                    int idx = s.indexOf(sub);
                    result.add(idx >= 0 ? s.substring(0, idx) : "");
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("RIGHT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            Object arg2 = ev.eval(args.get(1), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src);
                if (arg2 instanceof Number || (arg2 instanceof String && isNumeric((String) arg2))) {
                    int n = (int) toNumber(arg2);
                    result.add(n < 0 ? s : s.substring(Math.max(0, s.length() - n)));
                } else {
                    String sub = toString(arg2);
                    int idx = s.indexOf(sub);
                    result.add(idx >= 0 ? s.substring(idx + sub.length()) : "");
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("REPEAT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            int n = (int) toNumber(ev.eval(args.get(1), ctx));
            int maxChars = args.size() > 2 ? (int) toNumber(ev.eval(args.get(2), ctx)) : Integer.MAX_VALUE;
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src);
                String repeated = s.repeat(Math.max(0, n));
                if (repeated.length() > maxChars) repeated = repeated.substring(0, maxChars);
                result.add(repeated);
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // ---- Pattern matching ----
        functions.put("MATCHES", (ev, args, ctx) -> {
            Object str = ev.eval(args.get(0), ctx);
            Object pat = ev.eval(args.get(1), ctx);
            return boolToNum(anyPairMatch(str, pat, (s, pattern) -> {
                try {
                    java.util.regex.Pattern regex = dominoPatternToRegex(pattern);
                    return regex.matcher(s).matches();
                } catch (Exception e) {
                    return false;
                }
            }));
        });

        // Conversion
        functions.put("TEXT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            String format = args.size() > 1 ? toString(ev.eval(args.get(1), ctx)) : null;
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                if (src instanceof Number n && format != null && !format.isEmpty()) {
                    result.add(formatNumber(n.doubleValue(), format));
                } else {
                    result.add(toString(src));
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("TEXTTONUMBER", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src).trim();
                if (s.isEmpty()) { result.add(0.0); continue; }
                // Extract leading numeric portion: "12ABC" → 12, "ABC12" → 0
                double d = 0.0;
                try {
                    // Try full parse first
                    d = Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    // Find leading numeric prefix
                    int end = 0;
                    if (end < s.length() && (s.charAt(end) == '+' || s.charAt(end) == '-')) end++;
                    while (end < s.length() && (Character.isDigit(s.charAt(end)) || s.charAt(end) == '.')) end++;
                    if (end > 0 && (s.charAt(0) == '-' || s.charAt(0) == '+' || Character.isDigit(s.charAt(0)))) {
                        try { d = Double.parseDouble(s.substring(0, end)); } catch (NumberFormatException e2) {}
                    }
                }
                result.add(d);
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // Type checking
        functions.put("ISNUMBER", (ev, args, ctx) -> {
            Object v = ev.eval(args.get(0), ctx);
            if (v instanceof Number) return 1.0;
            if (v instanceof List<?> list) {
                for (Object elem : list) {
                    if (!(elem instanceof Number)) return 0.0;
                }
                return list.isEmpty() ? 0.0 : 1.0;
            }
            return 0.0;
        });
        functions.put("ISTEXT", (ev, args, ctx) -> {
            Object v = ev.eval(args.get(0), ctx);
            if (v instanceof String) return 1.0;
            if (v instanceof List<?> list) {
                for (Object elem : list) {
                    if (!(elem instanceof String)) return 0.0;
                }
                return list.isEmpty() ? 0.0 : 1.0;
            }
            return 0.0;
        });

        // Existence
        functions.put("ISAVAILABLE", (ev, args, ctx) -> {
            if (args.get(0) instanceof Expr.Variable v) {
                Object val = ctx.resolve(v.name());
                return boolToNum(val != null);
            }
            return 0.0;
        });

        // List functions
        functions.put("ELEMENTS", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            if (val instanceof List l) return (double) l.size();
            if (val == null || (val instanceof String s && s.isEmpty())) return 0.0;
            return 1.0;
        });
        functions.put("COUNT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            if (val instanceof List l) return l.isEmpty() ? 1.0 : (double) l.size();
            return 1.0; // scalar or null → 1
        });
        functions.put("ISMEMBER", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            Object list = ev.eval(args.get(1), ctx);
            List<Object> list2 = toList(list);
            if (val instanceof List<?> l1) {
                // Both lists: ALL elements of l1 must be in list2
                for (Object elem : l1) {
                    if (!list2.contains(elem)) return 0.0;
                }
                return l1.isEmpty() ? 0.0 : 1.0;
            }
            return boolToNum(list2.contains(val));
        });
        functions.put("ISNOTMEMBER", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            Object list = ev.eval(args.get(1), ctx);
            List<Object> list2 = toList(list);
            if (val instanceof List<?> l1) {
                // Both lists: NO element of l1 may be in list2
                for (Object elem : l1) {
                    if (list2.contains(elem)) return 0.0;
                }
                return l1.isEmpty() ? 1.0 : 1.0;
            }
            return boolToNum(!list2.contains(val));
        });

        // List replacement (element-level, not string-level)
        functions.put("REPLACE", (ev, args, ctx) -> {
            List<Object> source = toList(ev.eval(args.get(0), ctx));
            List<Object> from = toList(ev.eval(args.get(1), ctx));
            List<Object> to = toList(ev.eval(args.get(2), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : source) {
                int idx = from.indexOf(src);
                result.add(idx >= 0 && idx < to.size() ? to.get(idx) : src);
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // Control flow
        functions.put("IF", (ev, args, ctx) -> {
            // @If(cond1; action1; cond2; action2; ...; else)
            int n = args.size();
            if (n == 0) return "";
            if (n == 1) { ev.eval(args.get(0), ctx); return ""; } // cond only, no action
            for (int i = 0; i + 1 < n; i += 2) {
                boolean cond = isTruthy(ev.eval(args.get(i), ctx));
                if (cond) return ev.eval(args.get(i + 1), ctx);
            }
            // No condition matched — return else_action (last arg) if odd count
            if (n % 2 == 1) return ev.eval(args.get(n - 1), ctx);
            return "";
        });
        functions.put("DO", (ev, args, ctx) -> {
            Object last = "";
            for (Expr arg : args) last = ev.eval(arg, ctx);
            return last;
        });
        functions.put("RETURN", (ev, args, ctx) -> {
            throw new ReturnValue(ev.eval(args.get(0), ctx));
        });

        // Date construction
        functions.put("DATE", (ev, args, ctx) -> {
            if (args.size() >= 3 && ev.eval(args.get(0), ctx) instanceof Number) {
                int year = (int) toNumber(ev.eval(args.get(0), ctx));
                int month = (int) toNumber(ev.eval(args.get(1), ctx));
                int day = (int) toNumber(ev.eval(args.get(2), ctx));
                int hour = args.size() >= 6 ? (int) toNumber(ev.eval(args.get(3), ctx)) : 0;
                int min = args.size() >= 6 ? (int) toNumber(ev.eval(args.get(4), ctx)) : 0;
                int sec = args.size() >= 6 ? (int) toNumber(ev.eval(args.get(5), ctx)) : 0;
                var dt = java.time.LocalDateTime.of(year, month, day, hour, min, sec);
                return DT_FMT.format(dt.atZone(java.time.ZoneId.systemDefault()));
            }
            // time-date overload: strip time
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                java.time.ZonedDateTime zdt = parseDateToZoned(toString(src));
                if (zdt != null) {
                    result.add(DT_FMT.format(zdt.toLocalDate().atStartOfDay(zdt.getZone())));
                } else result.add("");
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // Date/time
        functions.put("CREATED", (ev, args, ctx) -> ctx.resolve("CREATED"));
        functions.put("MODIFIED", (ev, args, ctx) -> ctx.resolve("MODIFIED"));
        functions.put("ACCESSED", (ev, args, ctx) -> ctx.resolve("ACCESSED"));
        functions.put("ADDEDTOTHISFILE", (ev, args, ctx) -> ctx.resolve("ADDEDTOTHISFILE"));
        functions.put("NOW", (ev, args, ctx) -> DT_FMT.format(Instant.now()));
        functions.put("TODAY", (ev, args, ctx) ->
                DT_FMT.format(java.time.ZonedDateTime.now().toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault())));

        // @Adjust: apply adjustments in reverse order (seconds→years)
        functions.put("ADJUST", (ev, args, ctx) -> {
            Object dateVal = ev.eval(args.get(0), ctx);
            int years = (int) toNumber(ev.eval(args.get(1), ctx));
            int months = (int) toNumber(ev.eval(args.get(2), ctx));
            int days = (int) toNumber(ev.eval(args.get(3), ctx));
            int hours = (int) toNumber(ev.eval(args.get(4), ctx));
            int minutes = (int) toNumber(ev.eval(args.get(5), ctx));
            int seconds = (int) toNumber(ev.eval(args.get(6), ctx));
            List<Object> sources = toList(dateVal);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                java.time.ZonedDateTime dt = parseDateToZoned(toString(src));
                if (dt == null) { result.add(""); continue; }
                // Apply last-to-first: seconds, minutes, hours, days, months, years
                dt = dt.plusSeconds(seconds).plusMinutes(minutes).plusHours(hours)
                       .plusDays(days).plusMonths(months).plusYears(years);
                result.add(DT_FMT.format(dt));
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // Security
        functions.put("USERNAME", (ev, args, ctx) -> currentUserName);
        functions.put("DOCFIELDS", (ev, args, ctx) -> ctx.getFieldNames());
        functions.put("DOCLENGTH", (ev, args, ctx) -> 0.0);
        functions.put("DOCUMENTUNIQUEID", (ev, args, ctx) -> ctx.getDocumentUNID());
        functions.put("DOCLOCK", (ev, args, ctx) -> {
            if (args.isEmpty()) return "";
            String kw = toString(ev.eval(args.get(0), ctx));
            return switch (kw) {
                case "LOCK", "UNLOCK" -> 1.0;
                case "STATUS" -> "";
                case "LOCKINGENABLED" -> 0.0;
                default -> "";
            };
        }); // requires Couchbase document metadata

        // Boolean constants
        functions.put("ALL", (ev, args, ctx) -> 1.0);
        functions.put("TRUE", (ev, args, ctx) -> 1.0);
        functions.put("FALSE", (ev, args, ctx) -> 0.0);
        functions.put("SUCCESS", (ev, args, ctx) -> 1.0);

        // Validation
        functions.put("FAILURE", (ev, args, ctx) ->
                args.isEmpty() ? "" : toString(ev.eval(args.get(0), ctx)));

        // Side-effects
        functions.put("DELETEFIELD", (ev, args, ctx) -> new Expr.DeleteField(
                args.isEmpty() ? new Expr.Variable("") : args.get(0)));

        // Error handling
        functions.put("ERROR", (ev, args, ctx) -> ERROR_VALUE);
        functions.put("ISERROR", (ev, args, ctx) ->
                boolToNum(ev.eval(args.get(0), ctx) == ERROR_VALUE));

        // ---- List aggregation ----
        functions.put("MAX", (ev, args, ctx) -> {
            double max = Double.NEGATIVE_INFINITY;
            for (Expr arg : args) for (Object o : toList(ev.eval(arg, ctx))) max = Math.max(max, toNumber(o));
            return max == (int) max ? (double) (int) max : max;
        });
        functions.put("MIN", (ev, args, ctx) -> {
            double min = Double.POSITIVE_INFINITY;
            for (Expr arg : args) for (Object o : toList(ev.eval(arg, ctx))) min = Math.min(min, toNumber(o));
            return min == (int) min ? (double) (int) min : min;
        });
        functions.put("SUM", (ev, args, ctx) -> {
            double sum = 0;
            for (Expr arg : args) for (Object o : toList(ev.eval(arg, ctx))) sum += toNumber(o);
            return sum == (int) sum ? (double) (int) sum : sum;
        });
        functions.put("MODULO", (ev, args, ctx) -> map2(ev, args, ctx, (a, b) -> {
            if (b == 0) return 0.0;
            double r = a % b;
            return r < 0 ? r + Math.abs(b) : r;
        }));
        functions.put("SIGN", (ev, args, ctx) -> {
            double v = toNumber(ev.eval(args.get(0), ctx));
            return v > 0 ? 1.0 : v < 0 ? -1.0 : 0.0;
        });

        // ---- List manipulation ----
        functions.put("SUBSET", (ev, args, ctx) -> {
            List<Object> src = toList(ev.eval(args.get(0), ctx));
            int n = (int) toNumber(ev.eval(args.get(1), ctx));
            List<Object> r = new java.util.ArrayList<>();
            if (n > 0) for (int i = 0; i < n && i < src.size(); i++) r.add(src.get(i));
            else for (int i = src.size() - 1; i >= src.size() + n && i >= 0; i--) r.add(0, src.get(i));
            return r.isEmpty() ? "" : r.size() == 1 ? r.get(0) : r;
        });
        functions.put("UNIQUE", (ev, args, ctx) -> {
            List<Object> src = toList(ev.eval(args.get(0), ctx));
            java.util.LinkedHashSet<Object> seen = new java.util.LinkedHashSet<>();
            for (Object o : src) seen.add(toString(o));
            List<Object> r = new java.util.ArrayList<>(seen);
            return r.isEmpty() ? "" : r.size() == 1 ? r.get(0) : r;
        });
        functions.put("MEMBER", (ev, args, ctx) -> {
            Object needle = ev.eval(args.get(0), ctx);
            List<Object> haystack = toList(ev.eval(args.get(1), ctx));
            for (int i = 0; i < haystack.size(); i++) {
                if (toString(needle).equals(toString(haystack.get(i)))) return (double) (i + 1);
            }
            return 0.0;
        });
        functions.put("IMPLODE", (ev, args, ctx) -> {
            List<Object> src = toList(ev.eval(args.get(0), ctx));
            String sep = args.size() > 1 ? toString(ev.eval(args.get(1), ctx)) : " ";
            return src.isEmpty() ? "" : String.join(sep, src.stream().map(Evaluator::toString).toList());
        });
        functions.put("SORT", (ev, args, ctx) -> {
            List<Object> src = new java.util.ArrayList<>(toList(ev.eval(args.get(0), ctx)));
            src.sort((a, b) -> toString(a).compareTo(toString(b)));
            return src.isEmpty() ? "" : src.size() == 1 ? src.get(0) : src;
        });

        // ---- String: substring from end ----
        functions.put("LEFTBACK", (ev, args, ctx) -> {
            Object src = ev.eval(args.get(0), ctx);
            Object arg = ev.eval(args.get(1), ctx);
            List<Object> sources = toList(src);
            List<Object> result = new ArrayList<>();
            for (Object item : sources) {
                String str = toString(item);
                if (arg instanceof Number) {
                    int n = ((Number) arg).intValue();
                    result.add(n <= 0 ? "" : str.substring(0, Math.min(n, str.length())));
                } else {
                    String sep = toString(arg);
                    int idx = str.lastIndexOf(sep);
                    result.add(idx < 0 ? str : str.substring(0, idx + sep.length()));
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("RIGHTBACK", (ev, args, ctx) -> {
            Object src = ev.eval(args.get(0), ctx);
            Object arg = ev.eval(args.get(1), ctx);
            List<Object> sources = toList(src);
            List<Object> result = new ArrayList<>();
            for (Object item : sources) {
                String str = toString(item);
                if (arg instanceof Number) {
                    int n = ((Number) arg).intValue();
                    result.add(n <= 0 ? "" : str.substring(Math.max(0, str.length() - n)));
                } else {
                    String sep = toString(arg);
                    int idx = str.indexOf(sep);
                    result.add(idx < 0 ? str : str.substring(idx + sep.length()));
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("MIDDLE", (ev, args, ctx) -> {
            String s = toString(ev.eval(args.get(0), ctx));
            Object fromObj = ev.eval(args.get(1), ctx);
            if (fromObj instanceof Number) {
                int start = ((Number) fromObj).intValue();
                if (args.size() > 2) {
                    Object toObj = ev.eval(args.get(2), ctx);
                    if (toObj instanceof Number) {
                        int len = ((Number) toObj).intValue();
                        if (len < 0) return s.substring(Math.max(0, s.length() + len), Math.min(s.length(), start));
                        return start <= 0 ? "" : s.substring(start - 1, Math.min(s.length(), start - 1 + len));
                    }
                    String sep = toString(toObj);
                    if (sep.isEmpty()) return s.substring(start - 1);
                    int from = start <= 0 ? 0 : start - 1;
                    int to = s.indexOf(sep, from);
                    return to < 0 ? s.substring(from) : s.substring(from, to);
                }
                return start <= 0 ? "" : s.substring(start - 1);
            }
            // fromObj is a separator string
            String sep = toString(fromObj);
            int idx = s.indexOf(sep);
            if (idx < 0) return "";
            int start = idx + sep.length();
            if (args.size() > 2) {
                Object toObj = ev.eval(args.get(2), ctx);
                if (toObj instanceof Number) {
                    int n = ((Number) toObj).intValue();
                    if (n > 0) return start + n <= s.length() ? s.substring(start, start + n) : s.substring(start);
                    int end = start + (-n) > s.length() ? s.length() : start + (-n);
                    return s.substring(start, end);
                }
                String toSep = toString(toObj);
                if (toSep.isEmpty()) return s.substring(start);
                int to = s.indexOf(toSep, start);
                return to < 0 ? s.substring(start) : s.substring(start, to);
            }
            return s.substring(start);
        });
        functions.put("MIDDLEBACK", (ev, args, ctx) -> {
            String s = toString(ev.eval(args.get(0), ctx));
            Object fromObj = ev.eval(args.get(1), ctx);
            if (fromObj instanceof Number) {
                int start = ((Number) fromObj).intValue();
                int from = s.length() - start + 1;
                if (args.size() > 2) {
                    Object toObj = ev.eval(args.get(2), ctx);
                    if (toObj instanceof Number) {
                        int len = ((Number) toObj).intValue();
                        from = s.length() - Math.abs(start) + 1;
                        return s.substring(Math.max(0, from - len), Math.min(s.length(), from));
                    }
                    String sep = toString(toObj);
                    if (sep.isEmpty()) return s.substring(from - 1);
                    int to = s.lastIndexOf(sep, from - 1);
                    return to < 0 ? s.substring(0, from) : s.substring(to + sep.length(), from);
                }
                return from <= 0 ? "" : s.substring(0, from);
            }
            // fromObj is separator string
            String sep = toString(fromObj);
            if (sep.isEmpty()) return s;
            int idxEnd = s.lastIndexOf(sep);
            if (idxEnd < 0) return "";
            if (args.size() > 2) {
                Object toObj = ev.eval(args.get(2), ctx);
                if (toObj instanceof Number) {
                    int n = ((Number) toObj).intValue();
                    if (n > 0) {
                        int end = Math.min(idxEnd + n, s.length());
                        return s.substring(idxEnd, end);
                    }
                    int to = idxEnd + (-n);
                    to = Math.max(0, idxEnd - to);
                    return s.substring(to, idxEnd);
                }
                String toSep = toString(toObj);
                if (toSep.isEmpty()) return s.substring(idxEnd);
                int to = s.lastIndexOf(toSep, idxEnd - 1);
                return to < 0 ? s.substring(0, idxEnd) : s.substring(to + toSep.length(), idxEnd);
            }
            return s.substring(idxEnd);
        });
        functions.put("PROPERCASE", (ev, args, ctx) -> {
            List<Object> sources = toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object o : sources) {
                String s = toString(o);
                StringBuilder sb = new StringBuilder();
                boolean cap = true;
                for (char c : s.toCharArray()) {
                    sb.append(cap ? Character.toUpperCase(c) : Character.toLowerCase(c));
                    cap = !Character.isLetterOrDigit(c);
                }
                result.add(sb.toString());
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // ---- Type checking ----
        functions.put("ISNULL", (ev, args, ctx) ->
                boolToNum(ev.eval(args.get(0), ctx) == null || "".equals(toString(ev.eval(args.get(0), ctx)))));
        functions.put("ISVALID", (ev, args, ctx) -> 1.0); // document is always valid in our context

        // ---- Boolean constants ----
        functions.put("YES", (ev, args, ctx) -> 1.0);
        functions.put("NO", (ev, args, ctx) -> 0.0);
        functions.put("NOTHING", (ev, args, ctx) -> "");
        functions.put("NEWLINE", (ev, args, ctx) -> "\n");
        functions.put("RANDOM", (ev, args, ctx) -> Math.random());

        // ---- Time part extraction ----
        functions.put("SECOND", (ev, args, ctx) -> map1(ev, args, ctx, s ->
                (double) extractDateField(s, java.time.temporal.ChronoField.SECOND_OF_MINUTE)));
        functions.put("MINUTE", (ev, args, ctx) -> map1(ev, args, ctx, s ->
                (double) extractDateField(s, java.time.temporal.ChronoField.MINUTE_OF_HOUR)));
        functions.put("HOUR", (ev, args, ctx) -> map1(ev, args, ctx, s ->
                (double) extractDateField(s, java.time.temporal.ChronoField.HOUR_OF_DAY)));
        functions.put("WEEKDAY", (ev, args, ctx) -> map1(ev, args, ctx, s -> {
            long v = extractDateField(s, java.time.temporal.ChronoField.DAY_OF_WEEK);
            // Domino: Sunday=1, Saturday=7. Java: Monday=1, Sunday=7. Convert.
            return v == 7 ? 1.0 : (double) (v + 1);
        }));
        functions.put("TOMORROW", (ev, args, ctx) ->
                DT_FMT.format(java.time.ZonedDateTime.now().plusDays(1)));
        functions.put("YESTERDAY", (ev, args, ctx) ->
                DT_FMT.format(java.time.ZonedDateTime.now().minusDays(1)));
        functions.put("ZONE", (ev, args, ctx) ->
                java.time.ZoneId.systemDefault().getId());
        functions.put("TIME", (ev, args, ctx) -> {
            if (args.size() >= 3 && ev.eval(args.get(0), ctx) instanceof Number) {
                int h = (int) toNumber(ev.eval(args.get(0), ctx));
                int m = (int) toNumber(ev.eval(args.get(1), ctx));
                int s = args.size() > 2 ? (int) toNumber(ev.eval(args.get(2), ctx)) : 0;
                return DT_FMT.format(java.time.ZonedDateTime.now()
                        .withHour(h).withMinute(m).withSecond(s).withNano(0));
            }
            // Strip date, keep time
            List<Object> sources = toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                java.time.ZonedDateTime zdt = parseDateToZoned(toString(src));
                if (zdt == null) result.add("");
                else result.add(DT_FMT.format(zdt.toLocalTime().atDate(java.time.LocalDate.of(1970,1,1))
                        .atZone(zdt.getZone())));
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("TIMEMERGE", (ev, args, ctx) -> {
            List<Object> dates = toList(ev.eval(args.get(0), ctx));
            List<Object> times = toList(ev.eval(args.get(1), ctx));
            int size = Math.max(dates.size(), times.size());
            List<Object> result = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                String ds = toString(dates.get(Math.min(i, dates.size() - 1)));
                String ts = toString(times.get(Math.min(i, times.size() - 1)));
                java.time.ZonedDateTime d = parseDateToZoned(ds);
                java.time.ZonedDateTime t = parseDateToZoned(ts);
                if (d == null || t == null) { result.add(""); continue; }
                result.add(DT_FMT.format(d.toLocalDate().atTime(t.toLocalTime()).atZone(d.getZone())));
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // ---- Float equality ----
        functions.put("FLOATEQ", (ev, args, ctx) -> {
            double a = toNumber(ev.eval(args.get(0), ctx));
            double b = toNumber(ev.eval(args.get(1), ctx));
            double eps = args.size() > 2 ? toNumber(ev.eval(args.get(2), ctx)) : 1e-15;
            return boolToNum(Math.abs(a - b) <= eps);
        });

        // ---- Math: natural log ----
        functions.put("LN", (ev, args, ctx) -> map1(ev, args, ctx, Math::log));

        // ---- Pattern matching ----
        functions.put("LIKE", (ev, args, ctx) -> {
            Object str = ev.eval(args.get(0), ctx);
            Object pat = ev.eval(args.get(1), ctx);
            String escape = args.size() > 2 ? toString(ev.eval(args.get(2), ctx)) : null;
            return boolToNum(anyPairMatch(str, pat, (s, pattern) -> {
                // Convert Domino LIKE pattern to regex
                String esc = escape != null && !escape.isEmpty() ? escape : "\\\\";
                StringBuilder regex = new StringBuilder();
                regex.append("^(?i)"); // case-insensitive by default
                for (int i = 0; i < pattern.length(); i++) {
                    char c = pattern.charAt(i);
                    if (c == '_') regex.append('.');
                    else if (c == '%') regex.append(".*");
                    else if (escape != null && i < pattern.length() - escape.length()) {
                        String sub = pattern.substring(i, i + escape.length());
                        if (sub.equals(escape)) { regex.append(Pattern.quote(escape)); i += escape.length() - 1; continue; }
                    }
                    else regex.append(Pattern.quote(String.valueOf(c)));
                }
                regex.append("$");
                return s.matches(regex.toString());
            }));
        });

        // ---- Error handling ----
        functions.put("IFERROR", (ev, args, ctx) -> {
            try {
                return ev.eval(args.get(0), ctx);
            } catch (Exception e) {
                return args.size() > 1 ? ev.eval(args.get(1), ctx) : "";
            }
        });

        // ---- Data conversion ----
        functions.put("ISTIME", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(val);
            for (Object src : sources) {
                if (src instanceof Number) return 0.0;
                if (parseDateToZoned(toString(src)) == null) return 0.0;
            }
            return 1.0;
        });
        functions.put("TEXTTOTIME", (ev, args, ctx) -> {
            List<Object> sources = toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                java.time.ZonedDateTime zdt = parseDateToZoned(toString(src));
                result.add(zdt == null ? "" : DT_FMT.format(zdt));
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("TONUMBER", (ev, args, ctx) -> map1(ev, args, ctx, s -> {
            try { return toNumber(s); } catch (Exception e) { return 0.0; }
        }));
        functions.put("TOTIME", (ev, args, ctx) -> {
            List<Object> sources = toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                java.time.ZonedDateTime zdt = parseDateToZoned(toString(src));
                result.add(zdt == null ? "" : DT_FMT.format(zdt));
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        // Time-zone conversions: placeholder (defer)
        functions.put("TIMETOTEXTINZONE", (ev, args, ctx) ->
                toString(ev.eval(args.get(0), ctx)));
        functions.put("TIMEZONETOTEXT", (ev, args, ctx) -> "UTC");

        // Formula validation
        functions.put("CHECKFORMULASYNTAX", (ev, args, ctx) -> {
            String formula = toString(ev.eval(args.get(0), ctx));
            try {
                Lexer.tokenize(formula);
                new Parser(Lexer.tokenize(formula)).parse();
                return "1";
            } catch (FormulaParseException e) {
                return List.of(e.getMessage(), "1", String.valueOf(e.position + 1), "1", String.valueOf(e.position + 1), "1", formula);
            }
        });

        functions.put("EVAL", (ev, args, ctx) -> {
            String formula = toString(ev.eval(args.get(0), ctx));
            return ev.evalExpr(formula, ctx);
        });

        // Command no-ops
        FunctionHandler noop = (ev, args, ctx) -> "";
        functions.put("COMMAND", noop);
        functions.put("POSTEDCOMMAND", noop);

        // ---- Phase 2: String matching (list-aware) ----
        functions.put("CONTAINS", (ev, args, ctx) -> {
            Object a = ev.eval(args.get(0), ctx);
            Object b = ev.eval(args.get(1), ctx);
            return boolToNum(anyPairMatch(a, b, (s1, s2) -> s1.contains(s2)));
        });
        functions.put("BEGINS", (ev, args, ctx) -> {
            Object a = ev.eval(args.get(0), ctx);
            Object b = ev.eval(args.get(1), ctx);
            return boolToNum(anyPairMatch(a, b, (s1, s2) -> s1.startsWith(s2)));
        });
        functions.put("ENDS", (ev, args, ctx) -> {
            Object a = ev.eval(args.get(0), ctx);
            Object b = ev.eval(args.get(1), ctx);
            return boolToNum(anyPairMatch(a, b, (s1, s2) -> s1.endsWith(s2)));
        });

        functions.put("FILEDIR", (ev, args, ctx) -> {
            String path = toString(ev.eval(args.get(0), ctx));
            int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return lastSep < 0 ? "" : path.substring(0, lastSep + 1);
        });

        // ---- Phase 2: String manipulation ----
        functions.put("REPLACESUBSTRING", (ev, args, ctx) -> {
            Object source = ev.eval(args.get(0), ctx);
            Object from = ev.eval(args.get(1), ctx);
            Object to = ev.eval(args.get(2), ctx);
            List<Object> fromList = toList(from);
            List<Object> toList = toList(to);
            // Process each source element through all from→to replacements sequentially
            List<Object> sources = toList(source);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String current = toString(src);
                for (int i = 0; i < fromList.size(); i++) {
                    String f = toString(fromList.get(i));
                    String t = toString(i < toList.size() ? toList.get(i) : toList.get(toList.size() - 1));
                    current = current.replace(f, t);
                }
                result.add(current);
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("WORD", (ev, args, ctx) -> {
            Object source = ev.eval(args.get(0), ctx);
            String sep = toString(ev.eval(args.get(1), ctx));
            if (sep.isEmpty()) sep = " ";
            double num = toNumber(ev.eval(args.get(2), ctx));
            int n = (int) num;
            if (n == 0) n = 1; // 0 = first word

            List<Object> sources = toList(source);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src);
                // Split on separator, preserving empty strings between consecutive separators
                String[] parts = s.split(java.util.regex.Pattern.quote(sep), -1);
                if (n > 0) {
                    result.add(n <= parts.length ? parts[n - 1] : "");
                } else {
                    int idx = parts.length + n; // -1 → last
                    result.add(idx >= 0 && idx < parts.length ? parts[idx] : "");
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // ---- Phase 2: Date extraction ----
        functions.put("MONTH", (ev, args, ctx) ->
                (double) extractDateField(ev.eval(args.get(0), ctx), java.time.temporal.ChronoField.MONTH_OF_YEAR));
        functions.put("DAY", (ev, args, ctx) ->
                (double) extractDateField(ev.eval(args.get(0), ctx), java.time.temporal.ChronoField.DAY_OF_MONTH));
        functions.put("YEAR", (ev, args, ctx) ->
                (double) extractDateField(ev.eval(args.get(0), ctx), java.time.temporal.ChronoField.YEAR));

        // ---- Phase 2: Control flow ----
        functions.put("WHILE", (ev, args, ctx) -> {
            while (isTruthy(ev.eval(args.get(0), ctx))) {
                for (int i = 1; i < args.size(); i++) ev.eval(args.get(i), ctx);
            }
            return 1.0;
        });
        functions.put("DOWHILE", (ev, args, ctx) -> {
            // Last arg is condition, rest are statements
            do {
                for (int i = 0; i < args.size() - 1; i++) ev.eval(args.get(i), ctx);
            } while (isTruthy(ev.eval(args.get(args.size() - 1), ctx)));
            return 1.0;
        });

        // ---- Phase 2: Variable/field manipulation ----
        functions.put("SET", (ev, args, ctx) -> {
            String varName = toString(ev.eval(args.get(0), ctx)).toUpperCase();
            Object val = ev.eval(args.get(1), ctx);
            if (ev.tempScope != null) ev.tempScope.put(varName, val);
            return val;
        });
        functions.put("SETFIELD", (ev, args, ctx) -> {
            String fieldName = toString(ev.eval(args.get(0), ctx)).toUpperCase();
            Object val = ev.eval(args.get(1), ctx);
            ctx.setField(fieldName, val);
            return val;
        });
    }

    /** Exception thrown by @Return to unwind evaluation. */
    static class ReturnValue extends RuntimeException {
        final Object value;
        ReturnValue(Object value) { this.value = value; }

        /** Unwrap a ReturnValue or re-throw other RuntimeExceptions. */
        static Object unwrap(RuntimeException e) {
            if (e instanceof ReturnValue rv) return rv.value;
            throw e;
        }
    }
}
