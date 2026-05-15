package com.domcouch.formula;

/**
 * Domino @Matches pattern-to-regex conversion.
 * Handles ?, *, +, {sets}, !, |, &amp;, \\ escape operators.
 */
final class PatternUtils {

    private PatternUtils() {}

    static java.util.regex.Pattern toRegex(String pattern) {
        java.util.List<String> orParts = splitTopLevel(pattern, '|');
        if (orParts.size() > 1) {
            String combined = orParts.stream().map(String::trim).filter(p -> !p.isEmpty())
                    .map(PatternUtils::convertLeaf).collect(java.util.stream.Collectors.joining("|"));
            if (combined.isEmpty()) combined = ".*";
            return java.util.regex.Pattern.compile(combined, java.util.regex.Pattern.CASE_INSENSITIVE);
        }
        java.util.List<String> andParts = splitTopLevel(pattern, '&');
        if (andParts.size() > 1) {
            String combined = "^" + andParts.stream().map(String::trim).filter(p -> !p.isEmpty())
                    .map(p -> "(?=.*" + convertLeaf(p) + ")").collect(java.util.stream.Collectors.joining("")) + ".*$";
            return java.util.regex.Pattern.compile(combined, java.util.regex.Pattern.CASE_INSENSITIVE);
        }
        return java.util.regex.Pattern.compile(convertLeaf(pattern), java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    private static java.util.List<String> splitTopLevel(String pattern, char op) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') depth++; else if (c == '}') depth--;
            else if (c == op && depth == 0) { parts.add(pattern.substring(start, i)); start = i + 1; }
        }
        parts.add(pattern.substring(start));
        return parts;
    }

    private static String convertLeaf(String pattern) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '+') { i++; if (i >= pattern.length()) break; Elem e = readElement(pattern, i); regex.append("(?:").append(e.regex).append(")*"); i = e.endPos; }
            else if (c == '!') {
                if (i + 1 >= pattern.length()) { regex.append('!'); i++; continue; }
                Elem e = readElement(pattern, i + 1);
                regex.append("(?!").append(e.regex).append(").");
                i = e.endPos;
            } else if (c == '?') { regex.append('.'); i++; }
            else if (c == '*') { regex.append(".*"); i++; }
            else if (c == '{') {
                int end = pattern.indexOf('}', i);
                if (end < 0) { regex.append("\\{"); i++; }
                else { String inner = pattern.substring(i + 1, end); regex.append(inner.startsWith("!") ? "[^" + inner.substring(1) + "]" : "[" + inner + "]"); i = end + 1; }
            } else if (c == '\\' && i + 1 < pattern.length()) { regex.append("\\Q").append(pattern.charAt(i + 1)).append("\\E"); i += 2; }
            else if (c == ' ') { i++; }
            else { regex.append(Character.toLowerCase(c)); i++; }
        }
        return regex.toString();
    }

    private record Elem(String regex, int endPos) {}

    private static Elem readElement(String pattern, int pos) {
        if (pos >= pattern.length()) return new Elem("", pos);
        char c = pattern.charAt(pos);
        return switch (c) {
            case '?' -> new Elem(".", pos + 1);
            case '*' -> new Elem(".*", pos + 1);
            case '{' -> { int end = pattern.indexOf('}', pos); if (end < 0) yield new Elem("\\{", pos + 1); String inner = pattern.substring(pos + 1, end); yield new Elem(inner.startsWith("!") ? "[^" + inner.substring(1) + "]" : "[" + inner + "]", end + 1); }
            case '!' -> { if (pos + 1 >= pattern.length()) yield new Elem("!", pos + 1); Elem inner = readElement(pattern, pos + 1); yield new Elem("(?!" + inner.regex + ").", inner.endPos); }
            case '\\' -> { if (pos + 1 < pattern.length()) yield new Elem("\\Q" + pattern.charAt(pos + 1) + "\\E", pos + 2); yield new Elem("\\\\", pos + 1); }
            default -> new Elem(String.valueOf(Character.toLowerCase(c)), pos + 1);
        };
    }
}
