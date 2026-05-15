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
    private String currentUserName;
    private final ThreadLocal<Map<String, Object>> tempScope =
            ThreadLocal.withInitial(HashMap::new);
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

    /** Update the user name for {@code @UserName} resolution in subsequent evaluations. */
    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName != null ? currentUserName : "Anonymous";
    }

    /** Parse and evaluate a single formula. Convenience for testing. */
    public Object evalExpr(String formula, FormulaContext ctx) {
        tempScope.set(new HashMap<>());
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
        tempScope.set(new HashMap<>());
    }

    /** Evaluate a single expression in the given context. */
    public Object eval(Expr expr, FormulaContext ctx) {
        try {
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
        Object val = ev.eval(args.getFirst(), ctx);
        List<Object> sources = toList(val);
        List<Object> result = new ArrayList<>();
        for (Object src : sources) result.add(fn.applyAsDouble(toNumber(src)));
        return result.size() == 1 ? result.getFirst() : result;
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
        return result.size() == 1 ? result.getFirst() : result;
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
    private static Object middleExtract(Evaluator ev, java.util.List<Expr> args,
                                         FormulaContext ctx, boolean fromBack) {
        String s = toString(ev.eval(args.get(0), ctx));
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
            String sub = toString(fromObj);
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
                    int idx = start - toString(fromObj).length();
                    start = Math.max(0, idx + len);
                    end = idx;
                }
            }
        } else {
            String toSub = toString(toObj);
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
     * <p>
     * Supported operators:
     * <ul>
     *   <li>{@code ?} → any single char</li>
     *   <li>{@code *} → any string (0+ chars)</li>
     *   <li>{@code +X} → zero or more of element X (prefix quantifier)</li>
     *   <li>{@code {ABC}} → character set; {@code {A-F}} → range; {@code {!ABC}} → negated set</li>
     *   <li>{@code !X} → any char NOT matching X (single-char NOT)</li>
     *   <li>{@code |} → logical OR of two sub-patterns</li>
     *   <li>{@code &} → logical AND of two sub-patterns</li>
     *   <li>{@code \C} → escaped literal character</li>
     * </ul>
     * Simple characters are case-insensitive; {}-enclosed chars are case-sensitive.
     */
    private static java.util.regex.Pattern dominoPatternToRegex(String pattern) {
        // Phase 1: split by top-level | (OR) — each alternative must match independently
        java.util.List<String> orParts = splitTopLevel(pattern, '|');
        if (orParts.size() > 1) {
            String combined = orParts.stream()
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .map(Evaluator::convertLeaf)
                    .collect(java.util.stream.Collectors.joining("|"));
            if (combined.isEmpty()) combined = ".*";
            return java.util.regex.Pattern.compile(combined, java.util.regex.Pattern.CASE_INSENSITIVE);
        }

        // Phase 2: no top-level | — check for top-level & (AND)
        java.util.List<String> andParts = splitTopLevel(pattern, '&');
        if (andParts.size() > 1) {
            // Each AND part must match somewhere in the string
            String combined = "^" + andParts.stream()
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .map(p -> "(?=.*" + convertLeaf(p) + ")")
                    .collect(java.util.stream.Collectors.joining("")) + ".*$";
            return java.util.regex.Pattern.compile(combined, java.util.regex.Pattern.CASE_INSENSITIVE);
        }

        // Phase 3: single leaf pattern — convert directly
        return java.util.regex.Pattern.compile(convertLeaf(pattern), java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    /** Split pattern by operator ({@code |} or {@code &}), respecting {@code {}} nesting. */
    private static java.util.List<String> splitTopLevel(String pattern, char op) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == op && depth == 0) {
                parts.add(pattern.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(pattern.substring(start));
        return parts;
    }

    /** Convert a leaf pattern (no top-level {@code |} or {@code &}) to a regex string. */
    private static String convertLeaf(String pattern) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '+') {
                // Prefix quantifier: zero or more of the next element
                i++;
                if (i >= pattern.length()) break;
                PatternElem elem = readElement(pattern, i);
                regex.append("(?:").append(elem.regex).append(")*");
                i = elem.endPos;
            } else if (c == '!') {
                // Single-char NOT: any char that does NOT match the next element
                if (i + 1 >= pattern.length()) { regex.append('!'); i++; continue; }
                PatternElem elem = readElement(pattern, i + 1);
                if (elem.regex.equals(".*") || elem.regex.startsWith("(?:")) {
                    // !* or !+X → zero-width negative lookahead + match any char
                    regex.append("(?!").append(elem.regex).append(").");
                } else {
                    // !c, !?, !{set} → match any char EXCEPT the element
                    regex.append("(?!").append(elem.regex).append(").");
                }
                i = elem.endPos;
            } else if (c == '?') {
                regex.append('.'); i++;
            } else if (c == '*') {
                regex.append(".*"); i++;
            } else if (c == '{') {
                int end = pattern.indexOf('}', i);
                if (end < 0) { regex.append("\\{"); i++; }
                else {
                    String inner = pattern.substring(i + 1, end);
                    if (inner.startsWith("!")) {
                        regex.append("[^").append(inner.substring(1)).append(']');
                    } else {
                        regex.append('[').append(inner).append(']');
                    }
                    i = end + 1;
                }
            } else if (c == '\\' && i + 1 < pattern.length()) {
                regex.append("\\Q").append(pattern.charAt(i + 1)).append("\\E");
                i += 2;
            } else if (c == ' ') {
                // Spaces between patterns are ignored (used around | and &)
                i++;
            } else {
                regex.append(Character.toLowerCase(c));
                i++;
            }
        }
        return regex.toString();
    }

    /** Represents one parsed pattern element (for {@code +} and {@code !} prefix operators). */
    private record PatternElem(String regex, int endPos) {}

    /** Read one pattern element starting at {@code pos}. Used by {@code +} and {@code !}. */
    private static PatternElem readElement(String pattern, int pos) {
        if (pos >= pattern.length()) return new PatternElem("", pos);
        char c = pattern.charAt(pos);

        return switch (c) {
            case '?' -> new PatternElem(".", pos + 1);
            case '*' -> new PatternElem(".*", pos + 1);
            case '{' -> {
                int end = pattern.indexOf('}', pos);
                if (end < 0) yield new PatternElem("\\{", pos + 1);
                String inner = pattern.substring(pos + 1, end);
                String re = inner.startsWith("!")
                        ? "[^" + inner.substring(1) + "]"
                        : "[" + inner + "]";
                yield new PatternElem(re, end + 1);
            }
            case '!' -> {
                // Read the element after !
                if (pos + 1 >= pattern.length()) yield new PatternElem("!", pos + 1);
                PatternElem inner = readElement(pattern, pos + 1);
                String re;
                if (inner.regex.equals(".*") || inner.regex.startsWith("(?:")) {
                    re = "(?!" + inner.regex + ").";
                } else {
                    re = "(?!" + inner.regex + ").";
                }
                yield new PatternElem(re, inner.endPos);
            }
            case '\\' -> {
                if (pos + 1 < pattern.length())
                    yield new PatternElem("\\Q" + pattern.charAt(pos + 1) + "\\E", pos + 2);
                yield new PatternElem("\\\\", pos + 1);
            }
            default -> new PatternElem(String.valueOf(Character.toLowerCase(c)), pos + 1);
        };
    }

    /** Format a number according to a Domino format string (always US locale). */
    private static String formatNumber(double value, String format) {
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
    private static String formatDate(String dateStr, String format) {
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
        // Math functions
        functions.put("ABS", (ev, args, ctx) -> {
            Object val = ev.eval(args.getFirst(), ctx);
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add(Math.abs(toNumber(src)));
            return result.size() == 1 ? result.getFirst() : result;
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
            return result.size() == 1 ? result.getFirst() : result;
        });

        // Calendar functions
        functions.put("BUSINESSDAYS", (ev, args, ctx) -> {
            Object starts = ev.eval(args.get(0), ctx);
            Object ends = ev.eval(args.get(1), ctx);
            // Optional: days to exclude (1=Sunday..7=Saturday) and dates to exclude
            java.util.Set<Integer> excludeDays = new java.util.HashSet<>();
            if (args.size() > 2) {
                for (Object o : toList(ev.eval(args.get(2), ctx))) {
                    int d = (int) toNumber(o);
                    if (d >= 1 && d <= 7) excludeDays.add(d);
                }
            }
            java.util.Set<java.time.LocalDate> excludeDates = new java.util.HashSet<>();
            if (args.size() > 3) {
                for (Object o : toList(ev.eval(args.get(3), ctx))) {
                    java.time.LocalDate ld = parseDate(toString(o));
                    if (ld != null) excludeDates.add(ld);
                }
            }
            List<Object> startList = toList(starts);
            List<Object> endList = toList(ends);
            int size = Math.max(startList.size(), endList.size());
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                java.time.LocalDate s = parseDate(toString(startList.get(Math.min(i, startList.size() - 1))));
                java.time.LocalDate e = parseDate(toString(endList.get(Math.min(i, endList.size() - 1))));
                if (s == null || e == null || e.isBefore(s)) { result.add(-1.0); continue; }
                long days = 0;
                for (java.time.LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                    int dow = d.getDayOfWeek().getValue(); // 1=Mon..7=Sun
                    int dominoDow = (dow == 7) ? 1 : dow + 1; // convert to Domino 1=Sun..7=Sat
                    if (!excludeDays.contains(dominoDow) && !excludeDates.contains(d)) days++;
                }
                result.add(days < 0 ? -1.0 : (double) days);
            }
            return result.size() == 1 ? result.getFirst() : result;
        });

        // String functions
        functions.put("ASCII", (ev, args, ctx) -> {
            Object val = ev.eval(args.getFirst(), ctx);
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
            Object src = ev.eval(args.get(0), ctx);
            List<Object> sources = toList(src);
            String sep = args.size() > 1 ? toString(ev.eval(args.get(1), ctx)) : " ,;";
            if (sep.isEmpty()) sep = " ,;";
            boolean includeEmpties = args.size() > 2 && isTruthy(ev.eval(args.get(2), ctx));
            boolean newlineAsSep = args.size() <= 3 || isTruthy(ev.eval(args.get(3), ctx));
            // Build regex character class from separators
            String sepChars = newlineAsSep ? sep + "\n" : sep;
            String sepPattern = "[" + java.util.regex.Pattern.quote(sepChars) + "]+";
            java.util.List<Object> result = new java.util.ArrayList<>();
            for (Object item : sources) {
                String s = toString(item);
                String[] parts = s.split(sepPattern, includeEmpties ? -1 : 0);
                for (String p : parts) {
                    if (includeEmpties || !p.isEmpty()) result.add(p);
                }
            }
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
// @Repeat(string; count; [maxChars]) — repeat string count times, then truncate to maxChars.
// Result cannot exceed 1,024 characters per Domino spec.
        functions.put("REPEAT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            int n = (int) toNumber(ev.eval(args.get(1), ctx));
            int maxChars = args.size() > 2 ? (int) toNumber(ev.eval(args.get(2), ctx)) : 0;
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = toString(src);
                String repeated = s.repeat(Math.max(0, n));
                if (maxChars > 0 && repeated.length() > maxChars) {
                    repeated = repeated.substring(0, maxChars);
                }
                // Domino enforces 1,024 character limit
                if (repeated.length() > 1024) {
                    repeated = repeated.substring(0, 1024);
                }
                result.add(repeated);
            }
            return result.size() == 1 ? result.getFirst() : result;
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
// TODO: @Text missing date format string support (currently number formats only)
        functions.put("TEXT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            String format = args.size() > 1 ? toString(ev.eval(args.get(1), ctx)) : null;
            List<Object> sources = toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                if (src instanceof Number n && format != null && !format.isEmpty()) {
                    result.add(formatNumber(n.doubleValue(), format));
                } else if (format != null && !format.isEmpty() && (format.startsWith("D") || format.startsWith("T") || format.startsWith("S"))) {
                    result.add(formatDate(toString(src), format));
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
            return result.size() == 1 ? result.getFirst() : result;
        });

        // Type checking
        functions.put("ISNUMBER", (ev, args, ctx) -> {
            Object v = ev.eval(args.getFirst(), ctx);
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
            Object v = ev.eval(args.getFirst(), ctx);
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
        functions.put("ISAUTHOR", (ev, args, ctx) -> 1.0);
        functions.put("ISAVAILABLE", (ev, args, ctx) -> {
            String name = toString(ev.eval(args.getFirst(), ctx));
            return boolToNum(ctx.resolve(name) != null);
        });
        functions.put("ISUNAVAILABLE", (ev, args, ctx) -> {
            String name = toString(ev.eval(args.getFirst(), ctx));
            return boolToNum(ctx.resolve(name) == null);
        });
        functions.put("ISNEWDOC", (ev, args, ctx) -> {
            try { return boolToNum(ctx.getDocumentUNID().isEmpty()); }
            catch (ContextNotSupportedException e) { return 1.0; }
        });
        functions.put("ISRESPONSEDOC", (ev, args, ctx) -> {
            Object parent = ctx.resolve("PARENTUNID");
            return boolToNum(parent != null && !toString(parent).isEmpty());
        });
        functions.put("NOTEID", (ev, args, ctx) -> {
            try {
                return "NT" + ctx.getDocumentUNID().substring(0,
                        Math.min(8, ctx.getDocumentUNID().length()));
            } catch (ContextNotSupportedException e) { return ""; }
        });
        functions.put("INHERITEDDOCUMENTUNIQUEID", (ev, args, ctx) ->
                ctx.resolve("PARENTUNID") != null ? toString(ctx.resolve("PARENTUNID")) : "");
        functions.put("AUTHOR", (ev, args, ctx) -> ctx.resolve("AUTHORS") != null ? ctx.resolve("AUTHORS") : "");
// TODO: @Attachments returns attachment count from context; needs binary attachment support
        functions.put("ATTACHMENTS", (ev, args, ctx) -> {
            try { return (double) ctx.getAttachmentCount(); }
            catch (ContextNotSupportedException e) { return 0.0; }
        });
        functions.put("ISAVAILABLE", (ev, args, ctx) -> {
            if (args.getFirst() instanceof Expr.Variable v) {
                Object val = ctx.resolve(v.name());
                return boolToNum(val != null);
            }
            return 0.0;
        });

        // List functions
        functions.put("ELEMENTS", (ev, args, ctx) -> {
            Object val = ev.eval(args.getFirst(), ctx);
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
            return result.size() == 1 ? result.getFirst() : result;
        });

        // Control flow
        functions.put("IF", (ev, args, ctx) -> {
            // @If(cond1; action1; cond2; action2; ...; else)
            int n = args.size();
            if (n == 0) return "";
            if (n == 1) { ev.eval(args.getFirst(), ctx); return ""; } // cond only, no action
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
            throw new ReturnValue(ev.eval(args.getFirst(), ctx));
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
            return result.size() == 1 ? result.getFirst() : result;
        });

        // Date/time
        functions.put("CREATED", (ev, args, ctx) -> ctx.resolve("CREATED"));
        functions.put("MODIFIED", (ev, args, ctx) -> ctx.resolve("MODIFIED"));
// TODO: @Accessed resolves from context; needs Couchbase document metadata
        functions.put("ACCESSED", (ev, args, ctx) -> ctx.resolve("ACCESSED"));
// TODO: @AddedToThisFile resolves from context; needs Couchbase document metadata
        functions.put("ADDEDTOTHISFILE", (ev, args, ctx) -> ctx.resolve("ADDEDTOTHISFILE"));
        functions.put("NOW", (ev, args, ctx) -> DT_FMT.format(Instant.now()));
        functions.put("TODAY", (ev, args, ctx) ->
                DT_FMT.format(java.time.ZonedDateTime.now().toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault())));

        // @Adjust: apply in reverse order (seconds→years); optional [DST] keyword
        functions.put("ADJUST", (ev, args, ctx) -> {
            Object dateVal = ev.eval(args.get(0), ctx);
            int years = (int) toNumber(ev.eval(args.get(1), ctx));
            int months = (int) toNumber(ev.eval(args.get(2), ctx));
            int days = (int) toNumber(ev.eval(args.get(3), ctx));
            int hours = (int) toNumber(ev.eval(args.get(4), ctx));
            int minutes = (int) toNumber(ev.eval(args.get(5), ctx));
            int seconds = (int) toNumber(ev.eval(args.get(6), ctx));
            boolean inLocalTime = args.size() > 7 && "INLOCALTIME".equalsIgnoreCase(
                    toString(ev.eval(args.get(7), ctx)));
            List<Object> sources = toList(dateVal);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                java.time.ZonedDateTime dt = parseDateToZoned(toString(src));
                if (dt == null) { result.add(""); continue; }
                dt = dt.plusSeconds(seconds).plusMinutes(minutes).plusHours(hours)
                       .plusDays(days).plusMonths(months).plusYears(years);
                if (inLocalTime) {
                    // DST adjustment: keep wall-clock time at the adjusted date
                    dt = dt.withZoneSameLocal(java.time.ZoneId.systemDefault());
                }
                result.add(DT_FMT.format(dt));
            }
            return result.size() == 1 ? result.getFirst() : result;
        });

        // Security
        functions.put("USERNAME", (ev, args, ctx) -> currentUserName);
        functions.put("USERROLES", (ev, args, ctx) -> List.of()); // no roles in Couchbase
        functions.put("USERNAMESLIST", (ev, args, ctx) -> List.of(currentUserName));
        functions.put("DOMAIN", (ev, args, ctx) -> {
            try { return ctx.getDomain(); }
            catch (ContextNotSupportedException e) { return ""; }
        });
        functions.put("VERSION", (ev, args, ctx) -> "Domino 14.5 / Couchbase");
        functions.put("DBNAME", (ev, args, ctx) -> {
            try { return List.of(ctx.getServerName(), ctx.getDatabaseName()); }
            catch (ContextNotSupportedException e) { return List.of("", ""); }
        });
        functions.put("DBTITLE", (ev, args, ctx) -> {
            try { return ctx.getDatabaseTitle(); }
            catch (ContextNotSupportedException e) { return ""; }
        });
        functions.put("REPLICAID", (ev, args, ctx) -> {
            try { return ctx.getReplicaID(); }
            catch (ContextNotSupportedException e) { return ""; }
        });
        functions.put("SERVERNAME", (ev, args, ctx) -> {
            try { return ctx.getServerName(); }
            catch (ContextNotSupportedException e) { return ""; }
        });
        functions.put("DOCFIELDS", (ev, args, ctx) -> {
            try { return ctx.getFieldNames(); }
            catch (ContextNotSupportedException e) { return List.of(); }
        });
// TODO: @DocLength returns placeholder 0; needs Couchbase document metadata
        functions.put("DOCLENGTH", (ev, args, ctx) -> {
            try { return (double) ctx.getDocumentSize(); }
            catch (ContextNotSupportedException e) { return 0.0; }
        });
        functions.put("DOCUMENTUNIQUEID", (ev, args, ctx) -> {
            try { return ctx.getDocumentUNID(); }
            catch (ContextNotSupportedException e) { return ""; }
        });
// TODO: @DocLock uses context methods; lockDocument/unlockDocument/getDocumentLockStatus/isDocumentLockingEnabled
        functions.put("DOCLOCK", (ev, args, ctx) -> {
            if (args.isEmpty()) return "";
            String kw = toString(ev.eval(args.getFirst(), ctx));
            try {
                return switch (kw) {
                    case "LOCK" -> boolToNum(ctx.lockDocument());
                    case "UNLOCK" -> boolToNum(ctx.unlockDocument());
                    case "STATUS" -> ctx.getDocumentLockStatus();
                    case "LOCKINGENABLED" -> boolToNum(ctx.isDocumentLockingEnabled());
                    default -> "";
                };
            } catch (ContextNotSupportedException e) {
                return switch (kw) {
                    case "LOCK", "UNLOCK" -> 1.0;
                    case "STATUS" -> "";
                    case "LOCKINGENABLED" -> 0.0;
                    default -> "";
                };
            }
        });

        // Boolean constants
        functions.put("ALL", (ev, args, ctx) -> 1.0);
        functions.put("TRUE", (ev, args, ctx) -> 1.0);
        functions.put("FALSE", (ev, args, ctx) -> 0.0);
        functions.put("SUCCESS", (ev, args, ctx) -> 1.0);

        // Validation
        functions.put("FAILURE", (ev, args, ctx) ->
                args.isEmpty() ? "" : toString(ev.eval(args.getFirst(), ctx)));

        // Side-effects
        functions.put("DELETEFIELD", (ev, args, ctx) -> new Expr.DeleteField(
                args.isEmpty() ? new Expr.Variable("") : args.getFirst()));

        // Error handling
        functions.put("ERROR", (ev, args, ctx) -> ERROR_VALUE);
        functions.put("ISERROR", (ev, args, ctx) ->
                boolToNum(ev.eval(args.getFirst(), ctx) == ERROR_VALUE));

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
            double v = toNumber(ev.eval(args.getFirst(), ctx));
            return v > 0 ? 1.0 : v < 0 ? -1.0 : 0.0;
        });

        // ---- List manipulation ----
        functions.put("SUBSET", (ev, args, ctx) -> {
            List<Object> src = toList(ev.eval(args.get(0), ctx));
            int n = (int) toNumber(ev.eval(args.get(1), ctx));
            List<Object> r = new java.util.ArrayList<>();
            if (n > 0) for (int i = 0; i < n && i < src.size(); i++) r.add(src.get(i));
            else for (int i = src.size() - 1; i >= src.size() + n && i >= 0; i--) r.addFirst(src.get(i));
            return r.isEmpty() ? "" : r.size() == 1 ? r.getFirst() : r;
        });
        functions.put("UNIQUE", (ev, args, ctx) -> {
            List<Object> src = toList(ev.eval(args.getFirst(), ctx));
            java.util.LinkedHashSet<Object> seen = new java.util.LinkedHashSet<>();
            for (Object o : src) seen.add(toString(o));
            List<Object> r = new java.util.ArrayList<>(seen);
            return r.isEmpty() ? "" : r.size() == 1 ? r.getFirst() : r;
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
                    // Remove last N chars: @LeftBack("Lennard Wallace"; 3) → "Lennard Wall"
                    result.add(n <= 0 ? str : str.substring(0, Math.max(0, str.length() - n)));
                } else {
                    // Search from right to left; exclude separator from result
                    String sep = toString(arg);
                    int idx = str.lastIndexOf(sep);
                    result.add(idx < 0 ? str : str.substring(0, idx));
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
                    // Search from right to left (lastIndexOf per Domino spec)
                    String sep = toString(arg);
                    int idx = str.lastIndexOf(sep);
                    result.add(idx < 0 ? str : str.substring(idx + sep.length()));
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("MIDDLE", (ev, args, ctx) -> middleExtract(ev, args, ctx, false));
        functions.put("MIDDLEBACK", (ev, args, ctx) -> middleExtract(ev, args, ctx, true));
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
        functions.put("ISVALID", (ev, args, ctx) -> {
            try { return boolToNum(ctx.isDocumentValid()); }
            catch (ContextNotSupportedException e) { return 1.0; }
        });

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
            String escapeChar = args.size() > 2 ? toString(ev.eval(args.get(2), ctx)) : null;
            return boolToNum(anyPairMatch(str, pat, (s, pattern) -> {
                StringBuilder regex = new StringBuilder();
                regex.append("^(?i)"); // case-insensitive by default
                for (int i = 0; i < pattern.length(); i++) {
                    char c = pattern.charAt(i);
                    // Check escape character first — skip both escape char and escaped char
                    if (escapeChar != null && !escapeChar.isEmpty()
                            && pattern.startsWith(escapeChar, i)) {
                        i += escapeChar.length(); // skip past escape char
                        if (i < pattern.length()) {
                            regex.append(java.util.regex.Pattern.quote(
                                    String.valueOf(pattern.charAt(i))));
                        }
                        continue;
                    }
                    if (c == '_') regex.append('.');
                    else if (c == '%') regex.append(".*");
                    else regex.append(java.util.regex.Pattern.quote(String.valueOf(c)));
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
        // ---- Time zone ----
        functions.put("ZONE", (ev, args, ctx) -> {
            String timeDate = args.isEmpty() ? null : toString(ev.eval(args.get(0), ctx));
            try { return ctx.getTimeZoneOffset(timeDate); }
            catch (ContextNotSupportedException e) {
                return java.time.ZoneId.systemDefault().getId();
            }
        });
        functions.put("GETCURRENTTIMEZONE", (ev, args, ctx) -> {
            try { return ctx.getCanonicalTimeZone(); }
            catch (ContextNotSupportedException e) {
                return java.time.ZoneId.systemDefault().getId();
            }
        });
        functions.put("TIMETOTEXTINZONE", (ev, args, ctx) -> {
            String timeDate = toString(ev.eval(args.get(0), ctx));
            String timeZone = toString(ev.eval(args.get(1), ctx));
            String format = args.size() > 2 ? toString(ev.eval(args.get(2), ctx)) : "";
            try { return ctx.timeToTextInZone(timeDate, timeZone, format); }
            catch (ContextNotSupportedException e) { return ""; }
        });
        functions.put("TIMEZONETOTEXT", (ev, args, ctx) -> {
            String timeZone = toString(ev.eval(args.get(0), ctx));
            String format = args.size() > 1 ? toString(ev.eval(args.get(1), ctx)) : "";
            try { return ctx.timeZoneToText(timeZone, format); }
            catch (ContextNotSupportedException e) { return ""; }
        });

        // ---- Quick-win placeholders ----
        functions.put("CLIENTTYPE", (ev, args, ctx) -> "Notes");
        functions.put("DBEXISTS", (ev, args, ctx) -> 1.0);
        functions.put("LANGUAGEPREFERENCE", (ev, args, ctx) -> "EN");
        functions.put("LOCALE", (ev, args, ctx) -> java.util.Locale.getDefault().toString());
        functions.put("KEYWORDS", (ev, args, ctx) -> List.of());
        functions.put("THISNAME", (ev, args, ctx) -> "");
        functions.put("THISVALUE", (ev, args, ctx) -> "");
        functions.put("URLQUERYSTRING", (ev, args, ctx) -> "");
        functions.put("V3USERNAME", (ev, args, ctx) -> currentUserName);
        functions.put("V4USERACCESS", (ev, args, ctx) -> 1.0);
        functions.put("UNAVAILABLE", (ev, args, ctx) ->
                boolToNum(ctx.resolve(toString(ev.eval(args.get(0), ctx))) == null));
        functions.put("ENVIRONMENT", (ev, args, ctx) -> {
            String name = toString(ev.eval(args.getFirst(), ctx));
            try { return ctx.getEnvironmentValue(name); }
            catch (ContextNotSupportedException e) { return ""; }
        });
        functions.put("REGQUERYVALUE", (ev, args, ctx) -> "");
        functions.put("GETIMCONTACTLISTGROUPNAMES", (ev, args, ctx) -> List.of());
        functions.put("USERNAMELANGUAGE", (ev, args, ctx) -> "EN");

        // ---- Document lifecycle ----
// Document lifecycle — delegate to context; graceful fallback to 1.0
        functions.put("DELETEDOCUMENT", (ev, args, ctx) -> {
            try { ctx.markForDeletion(); return 1.0; }
            catch (ContextNotSupportedException e) { return 1.0; }
        });
        functions.put("UNDELETEDOCUMENT", (ev, args, ctx) -> {
            try { ctx.unmarkForDeletion(); return 1.0; }
            catch (ContextNotSupportedException e) { return 1.0; }
        });
        functions.put("HARDDELETEDOCUMENT", (ev, args, ctx) -> {
            try { ctx.hardDelete(); return 1.0; }
            catch (ContextNotSupportedException e) { return 1.0; }
        });
        functions.put("DOCCOMMITTEDLENGTH", (ev, args, ctx) -> {
            try { return (double) ctx.getDocumentSize(); }
            catch (ContextNotSupportedException e) { return 0.0; }
        });

        // ---- Folder operations (stubs) ----
        functions.put("ADDTOFOLDER", (ev, args, ctx) -> {
            String folderName = toString(ev.eval(args.getFirst(), ctx));
            try { ctx.addToFolder(folderName); return 1.0; }
            catch (ContextNotSupportedException e) { return 1.0; }
        });
        functions.put("WHICHFOLDERS", (ev, args, ctx) -> {
            try { return ctx.getFolderNames(); }
            catch (ContextNotSupportedException e) { return List.of(); }
        });
        functions.put("NARROW", (ev, args, ctx) -> 1.0);
        functions.put("WIDE", (ev, args, ctx) -> 1.0);

        // ---- Field access ----
        functions.put("GETFIELD", (ev, args, ctx) -> {
            String name = toString(ev.eval(args.get(0), ctx));
            Object val = ctx.resolve(name);
            return val != null ? val : "";
        });

// TODO: @Now missing [ServerTime] keyword and serverNames param support
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
            return boolToNum(anyPairMatch(a, b, String::contains));
        });
        functions.put("BEGINS", (ev, args, ctx) -> {
            Object a = ev.eval(args.get(0), ctx);
            Object b = ev.eval(args.get(1), ctx);
            return boolToNum(anyPairMatch(a, b, String::startsWith));
        });
        functions.put("ENDS", (ev, args, ctx) -> {
            Object a = ev.eval(args.get(0), ctx);
            Object b = ev.eval(args.get(1), ctx);
            return boolToNum(anyPairMatch(a, b, String::endsWith));
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
        functions.put("FOR", (ev, args, ctx) -> {
            // args: init, condition, increment, statement...
            if (args.size() < 4) return "";
            ev.eval(args.get(0), ctx); // init
            Object last = 1.0;
            while (isTruthy(ev.eval(args.get(1), ctx))) { // condition
                for (int i = 3; i < args.size(); i++) last = ev.eval(args.get(i), ctx); // statements
                ev.eval(args.get(2), ctx); // increment
            }
            return last;
        });

        // ---- @Transform: apply formula to each list element ----
        functions.put("TRANSFORM", (ev, args, ctx) -> {
            List<Object> list = toList(ev.eval(args.get(0), ctx));
            String varName = toString(ev.eval(args.get(1), ctx)).toUpperCase();
            Expr formula = args.get(2);
            List<Object> result = new ArrayList<>();
            for (Object elem : list) {
                // Create a context that binds the variable to current element
                FormulaContext elemCtx = name -> {
                    if (name.equals(varName)) return elem;
                    return ctx.resolve(name);
                };
                result.add(ev.eval(formula, elemCtx));
            }
            return result.isEmpty() ? "" : result.size() == 1 ? result.get(0) : result;
        });

        // ---- Phase 2: Variable/field manipulation ----
        functions.put("SET", (ev, args, ctx) -> {
            String varName = toString(ev.eval(args.get(0), ctx)).toUpperCase();
            Object val = ev.eval(args.get(1), ctx);
            ev.tempScope.get().put(varName, val);
            return val;
        });
        functions.put("SETFIELD", (ev, args, ctx) -> {
            String fieldName = toString(ev.eval(args.get(0), ctx)).toUpperCase();
            Object val = ev.eval(args.get(1), ctx);
            try { ctx.setField(fieldName, val); } catch (ContextNotSupportedException e) { /* no-op */ }
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
