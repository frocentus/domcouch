package com.domcouch.formula;

import java.time.Instant;
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
    private final String currentUserName;
    private Map<String, Object> tempScope; // per-evaluation temp variables
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter
            .ofPattern("MM/dd/yyyy hh:mm:ss a").withZone(ZoneId.systemDefault());

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

        // Binary operators
        Object left = eval(bo.left(), ctx);
        Object right = eval(bo.right(), ctx);

        return switch (bo.op()) {
            case "+" -> add(left, right);
            case "-" -> subtract(left, right);
            case "*" -> multiply(left, right);
            case "/" -> divide(left, right);
            case "=" -> boolToNum(compare(left, right) == 0);
            case "<>" , "!=", "><" -> boolToNum(compare(left, right) != 0);
            case ">" -> boolToNum(compare(left, right) > 0);
            case "<" -> boolToNum(compare(left, right) < 0);
            case ">=" -> boolToNum(compare(left, right) >= 0);
            case "<=" -> boolToNum(compare(left, right) <= 0);
            case "&" -> boolToNum(isTruthy(left) && isTruthy(right));
            case "|" -> boolToNum(isTruthy(left) || isTruthy(right));
            default -> throw new FormulaParseException(4502,
                    "Unknown operator: " + bo.op(), -1);
        };
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

    // ---- Built-in function registration ----

    private void registerBuiltins() {
        // String functions
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

        // Conversion
        functions.put("TEXT", (ev, args, ctx) -> toString(ev.eval(args.get(0), ctx)));
        functions.put("TEXTTONUMBER", (ev, args, ctx) -> toNumber(ev.eval(args.get(0), ctx)));

        // Type checking
        functions.put("ISNUMBER", (ev, args, ctx) -> {
            Object v = ev.eval(args.get(0), ctx);
            if (v instanceof Number) return 1.0;
            if (v instanceof String s && !s.isEmpty()) {
                try { Double.parseDouble(s); return 1.0; } catch (NumberFormatException e) {}
            }
            return 0.0;
        });
        functions.put("ISTEXT", (ev, args, ctx) ->
                boolToNum(ev.eval(args.get(0), ctx) instanceof String));

        // Existence
        functions.put("ISAVAILABLE", (ev, args, ctx) -> {
            // args[0] is a Variable — check if it exists in context
            if (args.get(0) instanceof Expr.Variable v) {
                Object val = ctx.resolve(v.name());
                return boolToNum(val != null && !val.toString().isEmpty());
            }
            return 0.0;
        });

        // List functions
        functions.put("ELEMENTS", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            if (val instanceof List l) return (double) l.size();
            return 1.0; // scalar = 1 element
        });
        functions.put("ISMEMBER", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            Object list = ev.eval(args.get(1), ctx);
            if (list instanceof List l) return boolToNum(l.contains(val));
            return boolToNum(toString(val).equals(toString(list)));
        });
        functions.put("ISNOTMEMBER", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            Object list = ev.eval(args.get(1), ctx);
            if (list instanceof List l) return boolToNum(!l.contains(val));
            return boolToNum(!toString(val).equals(toString(list)));
        });

        // Control flow
        functions.put("IF", (ev, args, ctx) -> {
            boolean cond = isTruthy(ev.eval(args.get(0), ctx));
            return ev.eval(args.get(cond ? 1 : 2), ctx);
        });
        functions.put("DO", (ev, args, ctx) -> {
            Object last = "";
            for (Expr arg : args) last = ev.eval(arg, ctx);
            return last;
        });
        functions.put("RETURN", (ev, args, ctx) -> {
            throw new ReturnValue(ev.eval(args.get(0), ctx));
        });

        // Date/time
        functions.put("CREATED", (ev, args, ctx) -> ctx.resolve("CREATED"));
        functions.put("NOW", (ev, args, ctx) -> DT_FMT.format(Instant.now()));
        functions.put("TODAY", (ev, args, ctx) -> DT_FMT.format(Instant.now()));

        // Security
        functions.put("USERNAME", (ev, args, ctx) -> currentUserName);

        // Boolean constants
        functions.put("ALL", (ev, args, ctx) -> 1.0);
        functions.put("TRUE", (ev, args, ctx) -> 1.0);
        functions.put("FALSE", (ev, args, ctx) -> 0.0);

        // Side-effects
        functions.put("DELETEFIELD", (ev, args, ctx) -> new Expr.DeleteField( // target from FIELD statement
                args.isEmpty() ? new Expr.Variable("") : args.get(0)));

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
            // @While(condition; body; increment)
            Object last = "";
            while (isTruthy(ev.eval(args.get(0), ctx))) {
                last = ev.eval(args.get(1), ctx);  // body
                if (args.size() > 2) ev.eval(args.get(2), ctx);  // increment
            }
            return last;
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
