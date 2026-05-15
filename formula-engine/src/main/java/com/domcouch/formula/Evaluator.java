package com.domcouch.formula;

import com.domcouch.formula.handlers.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Walks an {@link Expr} AST and evaluates it against a {@link FormulaContext}.
 * <p>
 * Handles constants, variables, binary operators, @Function dispatch,
 * type coercion, assignment, and control flow ({@code @If}, {@code @Do}, {@code @Return}).
 */
public class Evaluator {

    private final Map<String, FunctionHandler> functions;
    private volatile String currentUserName;
    public final ThreadLocal<Map<String, Object>> tempScope =
            ThreadLocal.withInitial(HashMap::new);
    public static final DateTimeFormatter DT_FMT = DateTimeFormatter
            .ofPattern("MM/dd/yyyy hh:mm:ss a").withZone(ZoneId.systemDefault());

    /** Sentinel value for @Error / @IsError. */
    public static final Object ERROR_VALUE = new Object();

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

    /** Update the user name for {@code @UserName} resolution in subsequent evaluations. */
    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName != null ? currentUserName : "Anonymous";
    }

    public String getCurrentUserName() { return currentUserName; }

    /** Parse and evaluate a single formula. Convenience for testing. */
    public Object evalExpr(String formula, FormulaContext ctx) {
        tempScope.set(new HashMap<>());
        try {
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
        } finally {
            tempScope.remove();
        }
    }

    /** Reset the temp variable scope for a new evaluation (called by FormulaTranslator). */
    public void initTempScope() {
        tempScope.set(new HashMap<>());
    }

    /** Remove the temp variable scope to prevent thread-local leaks (called by FormulaTranslator). */
    public void clearTempScope() {
        tempScope.remove();
    }

    /** Evaluate a single expression in the given context. */
    public Object eval(Expr expr, FormulaContext ctx) {
        return switch (expr) {
                case Expr.Variable v -> {
                    // Check temp scope first, then context
                    Map<String, Object> ts = tempScope.get();
                    if (ts.containsKey(v.name())) {
                        Object tv = ts.get(v.name());
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
                    try {
                        ctx.deleteField(((Expr.Variable) df.target()).name());
                    } catch (ContextNotSupportedException e) { /* no-op */ }
                    yield "";
                }
                case Expr.Comment c -> "";
            };
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
        return result.size() == 1 ? result.getFirst() : result;
    }

    /** Permuted: every combination of elements from left × right. */
    static Object permuted(Object left, Object right, BinaryOp op) {
        List<Object> l1 = toList(left);
        List<Object> l2 = toList(right);
        List<Object> result = new ArrayList<>();
        for (Object a : l1) for (Object b : l2) result.add(op.apply(a, b));
        return result.size() == 1 ? result.getFirst() : result;
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
            try {
                ctx.setField(name, val);
            } catch (ContextNotSupportedException e) { /* no-op */ }
        } else {
            tempScope.get().put(name, val != null ? val : "");
        }
        return val;
    }

    private Object evalDefaultAssign(Expr.DefaultAssign da, FormulaContext ctx) {
        String name = ((Expr.Variable) da.target()).name();
        Object existing = ctx.resolve(name);
        // Apply default if absent, empty, or zero
        if (existing != null && !isEmptyValue(existing)) {
            return existing;
        }
        Object defVal = eval(da.value(), ctx);
        try { ctx.setField(name, defVal); } catch (ContextNotSupportedException e) { /* no-op */ }
        return defVal;
    }

    /** True if the value is absent/empty/zero per Domino DEFAULT semantics. */
    private static boolean isEmptyValue(Object val) {
        if (val == null) return true;
        if (val instanceof List<?> l) return l.isEmpty();
        String s = convertToString(val);
        return s.isEmpty() || "0".equals(s);
    }

    // ---- @Function dispatch ----

    private Object callFunction(String name, List<Expr> args, FormulaContext ctx) {
        FunctionHandler handler = functions.get(name);
        if (handler != null) {
            return handler.call(this, args, ctx);
        }
        // Unknown function → return error value (detectable via @IsError)
        return ERROR_VALUE;
    }

    // ---- Arithmetic helpers ----

    private static Object add(Object left, Object right) {
        if (left instanceof String || right instanceof String)
            return convertToString(left) + convertToString(right);
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

    private static int compare(Object left, Object right) {
        if (left instanceof Number && right instanceof Number)
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        if (left instanceof Comparable && right instanceof Comparable
                && left.getClass().equals(right.getClass()))
            return ((Comparable) left).compareTo(right);
        return convertToString(left).compareTo(convertToString(right));
    }

    // ---- Type coercion ----

    public static double toNumber(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s && !s.isEmpty()) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }

    public static boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Number n) return n.doubleValue() != 0.0;
        if (val instanceof String s) return !s.isEmpty();
        if (val instanceof Boolean b) return b;
        return true;
    }

    public static double boolToNum(boolean b) { return b ? 1.0 : 0.0; }

    // ---- Date parsing ----

    private static final java.time.format.DateTimeFormatter[] DATE_PARSERS = {
            java.time.format.DateTimeFormatter.ISO_DATE_TIME,
            java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy[ hh:mm:ss a]"),
            java.time.format.DateTimeFormatter.ofPattern("M/d/yy[ hh:mm:ss a]"),
            java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy['T'HH:mm:ss'Z']"),
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
            // Time-only patterns (tried after full date-time patterns)
            java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a"),
            java.time.format.DateTimeFormatter.ofPattern("H:mm:ss"),
            java.time.format.DateTimeFormatter.ofPattern("hh:mm a"),
            java.time.format.DateTimeFormatter.ofPattern("h:mm a"),
            java.time.format.DateTimeFormatter.ofPattern("H:mm"),
    };

    /** Extract a date field (month, day, year) from a date string or object. */
    public static int extractDateField(Object val, java.time.temporal.ChronoField field) {
        if (val == null) return 0;
        String s = convertToString(val).trim();
        if (s.isEmpty()) return 0;
        for (var fmt : DATE_PARSERS) {
            try {
                var parsed = fmt.parseBest(s,
                        java.time.ZonedDateTime::from,
                        java.time.LocalDateTime::from,
                        java.time.LocalDate::from,
                        java.time.LocalTime::from);
                return ((java.time.temporal.TemporalAccessor) parsed).get(field);
            } catch (Exception e) { /* try next format */ }
        }
        return 0;
    }

    /** Parse a date string to ZonedDateTime, returning null on failure. */
    public static java.time.ZonedDateTime parseDateToZoned(String s) {
        for (var fmt : DATE_PARSERS) {
            try {
                var parsed = fmt.parseBest(s,
                        java.time.ZonedDateTime::from,
                        java.time.LocalDateTime::from,
                        java.time.LocalDate::from,
                        java.time.LocalTime::from);
                if (parsed instanceof java.time.ZonedDateTime zdt) return zdt;
                if (parsed instanceof java.time.LocalDateTime ldt)
                    return ldt.atZone(java.time.ZoneId.systemDefault());
                if (parsed instanceof java.time.LocalDate ld)
                    return ld.atStartOfDay(java.time.ZoneId.systemDefault());
                if (parsed instanceof java.time.LocalTime lt)
                    return lt.atDate(java.time.LocalDate.now())
                            .atZone(java.time.ZoneId.systemDefault());
            } catch (Exception e) { /* try next */ }
        }
        return null;
    }

    /** Parse a date string to LocalDate, returning null on failure. */
    public static java.time.LocalDate parseDate(String s) {
        java.time.ZonedDateTime zdt = parseDateToZoned(s);
        return zdt != null ? zdt.toLocalDate() : null;
    }

    /** Check if a string represents a valid number. */
    public static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    public static String convertToString(Object val) {
        if (val == null) return "";
        if (val instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d)
                && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE)
            return String.valueOf((long) d.doubleValue());
        return val.toString();
    }

    // ---- List operations ----

    @SuppressWarnings("unchecked")
    public static List<Object> toList(Object val) {
        if (val instanceof List l) return l;
        return List.of(val);
    }

    /** Map a single-arg math function over a value or list. */
    public static Object map1(Evaluator ev, List<Expr> args, FormulaContext ctx,
                                java.util.function.DoubleUnaryOperator fn) {
        Object val = ev.eval(args.getFirst(), ctx);
        List<Object> sources = toList(val);
        List<Object> result = new ArrayList<>();
        for (Object src : sources) result.add(fn.applyAsDouble(toNumber(src)));
        return result.size() == 1 ? result.getFirst() : result;
    }

    /** Map a dual-arg math function pair-wise over value(s) or list(s). */
    public static Object map2(Evaluator ev, List<Expr> args, FormulaContext ctx,
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
        return result.size() == 1 ? result.getFirst() : result;
    }

    /** Check if any pair (a,b) from two values (or lists) matches the predicate. */
    public static boolean anyPairMatch(Object a, Object b,
                                         java.util.function.BiPredicate<String, String> pred) {
        for (Object sa : toList(a)) {
            for (Object sb : toList(b)) {
                if (pred.test(convertToString(sa), convertToString(sb))) return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Object subscript(Object list, Object idx) {
        int i = (int) Math.round(toNumber(idx));
        if (list instanceof List l) {
            if (l.isEmpty()) return "";
            if (i < 1) i = 1;
            if (i > l.size()) i = l.size();
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
     * Shared implementation for @Middle and @MiddleBack.
     * Overloads: (str, off/d, n), (str, off, sub), (str, sub, n), (str, sub, sub)
     *
     * @Middle scans from left to right; @MiddleBack scans from right to left.
     * For offset: the middle begins one character AFTER the offset.
     * For negative numberchars: middle starts AT the offset/substring and goes left.
     */
    public static Object middleExtract(Evaluator ev, java.util.List<Expr> args,
                                       FormulaContext ctx, boolean fromBack) {
        String s = convertToString(ev.eval(args.get(0), ctx));
        if (s.isEmpty()) return "";
        Object fromObj = ev.eval(args.get(1), ctx);
        Object toObj = args.size() > 2 ? ev.eval(args.get(2), ctx) : null;

        int start, end;

        // Determine start position
        if (fromObj instanceof Number) {
            int off = ((Number) fromObj).intValue();
            if (fromBack) {
                // Offset counts from right: off=1 means last char, off=2 means second-to-last
                start = s.length() - off;
            } else {
                // Offset counts from left; middle begins one char AFTER offset
                start = off; // 1-based off → 0-based index after offset
            }
        } else {
            String sub = convertToString(fromObj);
            if (sub.isEmpty()) return "";
            int idx = fromBack ? s.lastIndexOf(sub) : s.indexOf(sub);
            if (idx < 0) return "";
            start = idx + sub.length(); // one char after substring
        }
        start = Math.max(0, Math.min(start, s.length()));

        // Determine end position
        if (toObj == null) {
            end = s.length();
        } else if (toObj instanceof Number) {
            int len = ((Number) toObj).intValue();
            if (len > 0) {
                end = Math.min(s.length(), start + len);
            } else {
                // Negative: start moves left by |len|.
                // For offset: reference is old_start (after offset), count includes offset char.
                // For substring: reference is idx (start of substring), count excludes substring.
                if (fromObj instanceof Number) {
                    start = Math.max(0, start + len);
                    end = start - len; // old start
                } else {
                    int idx = start - convertToString(fromObj).length();
                    start = Math.max(0, idx + len);
                    end = idx;
                }
            }
        } else {
            String toSub = convertToString(toObj);
            if (toSub.isEmpty()) { end = s.length(); }
            else {
                int idx = fromBack
                        ? s.lastIndexOf(toSub, Math.max(0, start - 1))
                        : s.indexOf(toSub, start);
                end = idx >= 0 ? idx : s.length();
            }
        }

        start = Math.max(0, Math.min(start, s.length()));
        end = Math.max(0, Math.min(end, s.length()));
        if (start > end) { int tmp = start; start = end; end = tmp; }
        return start == end ? "" : s.substring(start, end);
    }

    /**
     * Convert a Domino @Matches pattern to a Java regex Pattern.
     * Delegates to {@link PatternUtils#toRegex(String)}.
     */
    public static java.util.regex.Pattern dominoPatternToRegex(String pattern) {
        return PatternUtils.toRegex(pattern);
    }

    /** Format a number according to a Domino format string (always US locale). */
    public static String formatNumber(double value, String format) {
        java.util.Locale us = java.util.Locale.US;
        String upper = format.toUpperCase();
        int decimals = 2;
        if (upper.matches(".*[0-9]+")) {
            try { decimals = Integer.parseInt(upper.replaceAll("[^0-9]", "")); } catch (Exception e) {}
        }
        int minWidth = 0;
        // Fixed-width padding: format contains a number after all letters (e.g., "C,8" → width 8)
        var widthMatch = java.util.regex.Pattern.compile("([A-Z,]+)(\\d+)$").matcher(upper.strip());
        if (widthMatch.find()) {
            try { minWidth = Integer.parseInt(widthMatch.group(2)); } catch (Exception e) {}
        }

        if (upper.contains("S")) {
            String r = String.format(us, "%." + decimals + "E", value);
            if (minWidth > r.length()) r = String.format("%" + minWidth + "s", r);
            return r;
        }
        if (upper.contains("C")) {
            String r = "$" + String.format(us, "%." + decimals + "f", value);
            if (minWidth > r.length()) r = String.format("%" + minWidth + "s", r);
            return r;
        }
        if (upper.contains("%")) {
            String r = String.format(us, "%." + decimals + "f", value * 100) + "%";
            if (minWidth > r.length()) r = String.format("%" + minWidth + "s", r);
            return r;
        }
        String result = String.format(us, "%." + decimals + "f", value);
        if (upper.contains(",")) {
            String[] parts = result.split("\\.");
            String intPart = String.format(us, "%,d", (long) Math.abs(value));
            if (value < 0) intPart = "-" + intPart;
            result = parts.length > 1 ? intPart + "." + parts[1] : intPart;
        }
        if (upper.contains("(") && upper.contains(")") && value < 0)
            result = "(" + result.substring(1) + ")";
        if (minWidth > result.length())
            result = String.format("%" + minWidth + "s", result);
        return result;
    }

    /** Format a date/time string according to a Domino date format code. */
    // TODO: @Text date format strings (D0-D3, T0-T1, S0-S3) — basic support
    public static String formatDate(String dateStr, String format) {
        java.time.ZonedDateTime zdt = parseDateToZoned(dateStr);
        if (zdt == null) return dateStr;
        String upper = format.toUpperCase().strip();
        java.time.format.DateTimeFormatter fmt = switch (upper) {
            case "D0" -> java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");
            case "D1" -> java.time.format.DateTimeFormatter.ofPattern("MM/dd");
            case "D2" -> java.time.format.DateTimeFormatter.ofPattern("MM/yyyy");
            case "D3" -> java.time.format.DateTimeFormatter.ofPattern("yyyy/MM");
            case "T0" -> java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
            case "T1" -> java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            case "S0" -> java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");
            case "S1" -> java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
            case "S2" -> java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            case "S3" -> java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            default -> null;
        };
        return fmt != null ? fmt.format(zdt) : dateStr;
    }

    // ---- Built-in function registration ----

    private void registerBuiltins() {
        MathHandlers.register(functions);
        StringHandlers.register(functions);
        DateTimeHandlers.register(functions);
        MiscHandlers.register(functions);
    }

    /** Exception thrown by @Return to unwind evaluation. */
    public static class ReturnValue extends RuntimeException {
        public final Object value;
        public ReturnValue(Object value) { this.value = value; }
    }
}
