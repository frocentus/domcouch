package com.domcouch.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * String @Function handlers: @Ascii, @Char, @Compare, @Explode, @Trim,
 * @UpperCase, @LowerCase, @Length, @Left, @Right, @Repeat, @Matches,
 * @Contains, @Begins, @Ends, @ReplaceSubstring, @Word, @FileDir,
 * @LeftBack, @RightBack, @Middle, @MiddleBack, @ProperCase, @NewLine.
 */
final class StringHandlers {

    private StringHandlers() {}

    static void register(Map<String, FunctionHandler> functions) {
        functions.put("ASCII", (ev, args, ctx) -> {
            Object val = ev.eval(args.getFirst(), ctx);
            boolean allInRange = args.size() > 1 && "ALLINRANGE".equalsIgnoreCase(Evaluator.toString(ev.eval(args.get(1), ctx)));
            List<Object> sources = Evaluator.toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = Evaluator.toString(src);
                StringBuilder sb = new StringBuilder();
                for (char c : s.toCharArray()) sb.append(c >= 32 && c <= 127 ? c : '?');
                String converted = sb.toString();
                if (allInRange && converted.indexOf('?') >= 0) converted = "";
                result.add(converted);
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("CHAR", (ev, args, ctx) -> {
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            java.nio.charset.Charset cp850 = java.nio.charset.Charset.forName("Cp850");
            for (Object src : sources) {
                int code = (int) Evaluator.toNumber(src) & 0xFF;
                result.add(new String(new byte[]{(byte) code}, cp850));
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("COMPARE", (ev, args, ctx) -> {
            List<Object> l1 = Evaluator.toList(ev.eval(args.get(0), ctx));
            List<Object> l2 = Evaluator.toList(ev.eval(args.get(1), ctx));
            int size = Math.max(l1.size(), l2.size());
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String s1 = Evaluator.toString(l1.get(Math.min(i, l1.size() - 1)));
                String s2 = Evaluator.toString(l2.get(Math.min(i, l2.size() - 1)));
                int cmp = s1.compareTo(s2);
                result.add(cmp < 0 ? -1.0 : cmp > 0 ? 1.0 : 0.0);
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("EXPLODE", (ev, args, ctx) -> {
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx));
            String sep = args.size() > 1 ? Evaluator.toString(ev.eval(args.get(1), ctx)) : " ,;";
            if (sep.isEmpty()) sep = " ,;";
            boolean includeEmpties = args.size() > 2 && Evaluator.isTruthy(ev.eval(args.get(2), ctx));
            boolean newlineAsSep = args.size() <= 3 || Evaluator.isTruthy(ev.eval(args.get(3), ctx));
            String sepChars = newlineAsSep ? sep + "\n" : sep;
            String sepPattern = "[" + java.util.regex.Pattern.quote(sepChars) + "]+";
            java.util.List<Object> result = new java.util.ArrayList<>();
            for (Object item : sources) {
                String[] parts = Evaluator.toString(item).split(sepPattern, includeEmpties ? -1 : 0);
                for (String p : parts) if (includeEmpties || !p.isEmpty()) result.add(p);
            }
            return result.isEmpty() ? "" : result.size() == 1 ? result.get(0) : result;
        });
        functions.put("TRIM", (ev, args, ctx) -> {
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String trimmed = Evaluator.toString(src).replaceAll("  +", " ").trim();
                if (!trimmed.isEmpty()) result.add(trimmed);
            }
            return result.isEmpty() ? "" : result.size() == 1 ? result.get(0) : result;
        });
        functions.put("UPPERCASE", (ev, args, ctx) -> {
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add(Evaluator.toString(src).toUpperCase());
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("LOWERCASE", (ev, args, ctx) -> {
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add(Evaluator.toString(src).toLowerCase());
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("LENGTH", (ev, args, ctx) -> {
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx));
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add((double) Evaluator.toString(src).length());
            return result.size() == 1 ? result.get(0) : result;
        });

        // @Left / @Right
        functions.put("LEFT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx); Object arg2 = ev.eval(args.get(1), ctx);
            List<Object> sources = Evaluator.toList(val); List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = Evaluator.toString(src);
                if (arg2 instanceof Number || (arg2 instanceof String s2 && Evaluator.isNumeric(s2))) {
                    int n = (int) Evaluator.toNumber(arg2);
                    result.add(n < 0 ? s : s.substring(0, Math.min(n, s.length())));
                } else {
                    int idx = s.indexOf(Evaluator.toString(arg2));
                    result.add(idx >= 0 ? s.substring(0, idx) : "");
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("RIGHT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx); Object arg2 = ev.eval(args.get(1), ctx);
            List<Object> sources = Evaluator.toList(val); List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = Evaluator.toString(src);
                if (arg2 instanceof Number || (arg2 instanceof String s2 && Evaluator.isNumeric(s2))) {
                    int n = (int) Evaluator.toNumber(arg2);
                    result.add(n < 0 ? s : s.substring(Math.max(0, s.length() - n)));
                } else {
                    int idx = s.indexOf(Evaluator.toString(arg2));
                    result.add(idx >= 0 ? s.substring(idx + Evaluator.toString(arg2).length()) : "");
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // @Repeat
        functions.put("REPEAT", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            int n = (int) Evaluator.toNumber(ev.eval(args.get(1), ctx));
            int maxChars = args.size() > 2 ? (int) Evaluator.toNumber(ev.eval(args.get(2), ctx)) : 0;
            List<Object> sources = Evaluator.toList(val); List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = Evaluator.toString(src);
                String repeated = s.repeat(Math.max(0, n));
                if (maxChars > 0 && repeated.length() > maxChars) repeated = repeated.substring(0, maxChars);
                if (repeated.length() > 1024) repeated = repeated.substring(0, 1024);
                result.add(repeated);
            }
            return result.size() == 1 ? result.getFirst() : result;
        });

        // @Matches
        functions.put("MATCHES", (ev, args, ctx) -> {
            Object str = ev.eval(args.get(0), ctx); Object pat = ev.eval(args.get(1), ctx);
            return Evaluator.boolToNum(Evaluator.anyPairMatch(str, pat, (s, pattern) -> {
                try { return Evaluator.dominoPatternToRegex(pattern).matcher(s).matches(); }
                catch (Exception e) { return false; }
            }));
        });

        // @Contains, @Begins, @Ends
        functions.put("CONTAINS", (ev, args, ctx) -> Evaluator.boolToNum(Evaluator.anyPairMatch(ev.eval(args.get(0), ctx), ev.eval(args.get(1), ctx), String::contains)));
        functions.put("BEGINS", (ev, args, ctx) -> Evaluator.boolToNum(Evaluator.anyPairMatch(ev.eval(args.get(0), ctx), ev.eval(args.get(1), ctx), String::startsWith)));
        functions.put("ENDS", (ev, args, ctx) -> Evaluator.boolToNum(Evaluator.anyPairMatch(ev.eval(args.get(0), ctx), ev.eval(args.get(1), ctx), String::endsWith)));

        // @ReplaceSubstring
        functions.put("REPLACESUBSTRING", (ev, args, ctx) -> {
            Object source = ev.eval(args.get(0), ctx); Object from = ev.eval(args.get(1), ctx); Object to = ev.eval(args.get(2), ctx);
            List<Object> fromList = Evaluator.toList(from); List<Object> toList = Evaluator.toList(to);
            List<Object> sources = Evaluator.toList(source); List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String current = Evaluator.toString(src);
                for (int i = 0; i < fromList.size(); i++) {
                    String f = Evaluator.toString(fromList.get(i));
                    String t = Evaluator.toString(i < toList.size() ? toList.get(i) : toList.get(toList.size() - 1));
                    current = current.replace(f, t);
                }
                result.add(current);
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        // @Word
        functions.put("WORD", (ev, args, ctx) -> {
            Object source = ev.eval(args.get(0), ctx); String sep = Evaluator.toString(ev.eval(args.get(1), ctx));
            if (sep.isEmpty()) sep = " "; double num = Evaluator.toNumber(ev.eval(args.get(2), ctx)); int n = (int) num;
            if (n == 0) n = 1;
            List<Object> sources = Evaluator.toList(source); List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                String s = Evaluator.toString(src);
                String[] parts = s.split(java.util.regex.Pattern.quote(sep), -1);
                if (n > 0) result.add(n <= parts.length ? parts[n - 1] : "");
                else { int idx = parts.length + n; result.add(idx >= 0 && idx < parts.length ? parts[idx] : ""); }
            }
            return result.size() == 1 ? result.get(0) : result;
        });

        functions.put("FILEDIR", (ev, args, ctx) -> {
            String path = Evaluator.toString(ev.eval(args.get(0), ctx));
            int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return lastSep < 0 ? "" : path.substring(0, lastSep + 1);
        });

        // Substring from end
        functions.put("LEFTBACK", (ev, args, ctx) -> {
            Object src = ev.eval(args.get(0), ctx); Object arg = ev.eval(args.get(1), ctx);
            List<Object> sources = Evaluator.toList(src); List<Object> result = new ArrayList<>();
            for (Object item : sources) {
                String str = Evaluator.toString(item);
                if (arg instanceof Number) {
                    int n = ((Number) arg).intValue();
                    result.add(n <= 0 ? str : str.substring(0, Math.max(0, str.length() - n)));
                } else {
                    String sep = Evaluator.toString(arg); int idx = str.lastIndexOf(sep);
                    result.add(idx < 0 ? str : str.substring(0, idx));
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("RIGHTBACK", (ev, args, ctx) -> {
            Object src = ev.eval(args.get(0), ctx); Object arg = ev.eval(args.get(1), ctx);
            List<Object> sources = Evaluator.toList(src); List<Object> result = new ArrayList<>();
            for (Object item : sources) {
                String str = Evaluator.toString(item);
                if (arg instanceof Number) {
                    int n = ((Number) arg).intValue();
                    result.add(n <= 0 ? "" : str.substring(Math.max(0, str.length() - n)));
                } else {
                    String sep = Evaluator.toString(arg); int idx = str.lastIndexOf(sep);
                    result.add(idx < 0 ? str : str.substring(idx + sep.length()));
                }
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("MIDDLE", (ev, args, ctx) -> Evaluator.middleExtract(ev, args, ctx, false));
        functions.put("MIDDLEBACK", (ev, args, ctx) -> Evaluator.middleExtract(ev, args, ctx, true));
        functions.put("PROPERCASE", (ev, args, ctx) -> {
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx)); List<Object> result = new ArrayList<>();
            for (Object o : sources) {
                String s = Evaluator.toString(o); StringBuilder sb = new StringBuilder(); boolean cap = true;
                for (char c : s.toCharArray()) { sb.append(cap ? Character.toUpperCase(c) : Character.toLowerCase(c)); cap = !Character.isLetterOrDigit(c); }
                result.add(sb.toString());
            }
            return result.size() == 1 ? result.get(0) : result;
        });
        functions.put("NEWLINE", (ev, args, ctx) -> "\n");
    }
}
