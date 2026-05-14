package com.domcouch.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tokenizes Domino formula strings into a list of {@link Token} objects.
 *
 * <h2>Supported tokens</h2>
 * Variables, string/brace constants (with escape sequences), numeric constants
 * (integer, decimal, scientific), time-date constants, keywords, operators,
 * {@code @Functions}, punctuation, and {@code REM} comments.
 *
 * <h2>Case handling</h2>
 * Variables, keywords, and {@code @Function} names are normalized to uppercase.
 * String content preserves original case. Unknown bracket content (single
 * alphabetic word) is treated as a keyword for {@code @Command} compatibility.
 *
 * <h2>Thread safety</h2>
 * Stateless — {@code tokenize} is safe for concurrent use.
 */
public final class Lexer {

    private static final Set<String> RESERVED_WORDS = Set.of(
            "SELECT", "FIELD", "DEFAULT", "ENVIRONMENT", "REM",
            "DURING", "THEN", "ELSE", "END"
    );

    private static final Set<String> BRACKET_KEYWORDS = Set.of(
            "OK", "CANCEL", "YES", "NO", "OKCANCEL", "YESNO", "NOCANCEL",
            "ABORT", "RETRY", "IGNORE", "YESNOCANCEL"
    );

    private static final Set<String> MULTI_CHAR_OPS = Set.of(
            ":=", "<>", "!=", ">=", "<="
    );

    private Lexer() {}

    /**
     * Tokenize a Domino formula string.
     * @param input the formula source
     * @return list of tokens (empty for empty/whitespace input)
     * @throws NullPointerException if input is null
     * @throws FormulaParseException if input is malformed
     */
    public static List<Token> tokenize(String input) {
        if (input == null) throw new NullPointerException("input must not be null");

        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = input.length();

        while (i < len) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) { i++; continue; }

            // String constants: "..." or {...}
            if (c == '"' || c == '{') {
                StringResult sr = readString(input, i);
                tokens.add(sr.token);
                i = sr.endPos;
                continue;
            }

            // Comment: REM "...", REM {...}, or REM
            if (matchWord(input, i, "REM")) {
                int start = i;
                i += 3;
                while (i < len && Character.isWhitespace(input.charAt(i))) i++;
                String text = "";
                if (i < len && (input.charAt(i) == '"' || input.charAt(i) == '{')) {
                    StringResult sr = readString(input, i);
                    text = sr.lexeme;
                    i = sr.endPos;
                }
                while (i < len && input.charAt(i) != ';') i++;
                if (i < len) i++;
                tokens.add(new Token(TokenType.COMMENT, text, start));
                continue;
            }

            // Bracket: [KEYWORD], [datetime], or subscript opening
            if (c == '[') {
                int start = i;
                i++;
                StringBuilder content = new StringBuilder();
                while (i < len && input.charAt(i) != ']') {
                    content.append(input.charAt(i));
                    i++;
                }
                if (i >= len)
                    throw new FormulaParseException(4501, "Unclosed bracket at " + start, start);
                i++;

                String ct = content.toString().trim();
                if (BRACKET_KEYWORDS.contains(ct.toUpperCase())) {
                    tokens.add(new Token(TokenType.KEYWORD, ct.toUpperCase(), start));
                }
                else if (ct.matches("[A-Za-z]")) {
                    // Single letter: emit [ and ] for subscript support (e.g., items[n])
                    tokens.add(new Token(TokenType.OPERATOR, "[", start));
                    tokens.add(new Token(TokenType.VARIABLE, ct.toUpperCase(), start + 1));
                    tokens.add(new Token(TokenType.OPERATOR, "]", start + ct.length() + 1));
                }
                else if (ct.matches("[A-Za-z][A-Za-z]*")) {
                    // Multi-letter: treat as keyword (e.g., [EditClear])
                    tokens.add(new Token(TokenType.KEYWORD, ct.toUpperCase(), start));
                }
                else if (ct.matches("[+-]?\\d+")) {
                    tokens.add(new Token(TokenType.OPERATOR, "[", start));
                    tokens.add(new Token(TokenType.CONST_NUMBER, ct, start + 1));
                    tokens.add(new Token(TokenType.OPERATOR, "]", start + 1 + ct.length()));
                }
                else if (ct.matches("\\d{1,2}:\\d{2}(:\\d{2})?( [AP]M)?")
                        || ct.matches("\\d{1,2}/\\d{1,2}(/\\d{2,4})?( \\d{1,2}:\\d{2}( [AP]M)?)?")) {
                    tokens.add(new Token(TokenType.CONST_DATETIME, ct, start));
                }
                else {
                    tokens.add(new Token(TokenType.OPERATOR, "[", start));
                    List<Token> inner = tokenize(ct);
                    for (Token t : inner)
                        tokens.add(new Token(t.type(), t.lexeme(), t.position() + start + 1));
                    tokens.add(new Token(TokenType.OPERATOR, "]", start + ct.length() + 1));
                }
                continue;
            }

            // Numbers
            if (c == '+' || c == '-' || c == '.' || Character.isDigit(c)) {
                Token num = tryReadNumber(input, i);
                if (num != null) {
                    tokens.add(num);
                    i += num.lexeme().length();
                    continue;
                }
            }

            // @Function
            if (c == '@') {
                int start = i;
                i++;
                StringBuilder fn = new StringBuilder();
                while (i < len && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    fn.append(Character.toUpperCase(input.charAt(i)));
                    i++;
                }
                if (fn.isEmpty())
                    throw new FormulaParseException(4503, "@ without name at " + start, start);
                tokens.add(new Token(TokenType.AT_FUNCTION, fn.toString(), start));
                continue;
            }

            // Multi-char operators
            if (i + 1 < len) {
                String two = input.substring(i, i + 2);
                if (MULTI_CHAR_OPS.contains(two)) {
                    tokens.add(new Token(TokenType.OPERATOR, two, i));
                    i += 2;
                    continue;
                }
            }

            // Single-char operators and punctuation
            switch (c) {
                case '(' -> { tokens.add(new Token(TokenType.LPAREN, "(", i)); i++; continue; }
                case ')' -> { tokens.add(new Token(TokenType.RPAREN, ")", i)); i++; continue; }
                case ';' -> { tokens.add(new Token(TokenType.SEMICOLON, ";", i)); i++; continue; }
                case '+', '/', '=', '>', '<', '&', '|', '!', ':' ->
                    { tokens.add(new Token(TokenType.OPERATOR, String.valueOf(c), i)); i++; continue; }
                case '*' -> {
                    // Check for permuted operator: *+ *- ** */ *= *> *< *>= *<= *!=
                    if (i + 1 < len) {
                        char next = input.charAt(i + 1);
                        if ("+-*/=><!" .indexOf(next) >= 0) {
                            if (i + 2 < len && (next == '>' || next == '<' || next == '!')
                                    && input.charAt(i + 2) == '=') {
                                tokens.add(new Token(TokenType.OPERATOR, "*" + next + "=", i));
                                i += 3; continue;
                            }
                            tokens.add(new Token(TokenType.OPERATOR, "*" + next, i));
                            i += 2; continue;
                        }
                    }
                    tokens.add(new Token(TokenType.OPERATOR, "*", i)); i++; continue;
                }
            }

            if (c == '-') { tokens.add(new Token(TokenType.OPERATOR, "-", i)); i++; continue; }

            // Identifier
            if (c == '$' || Character.isLetter(c)) {
                int start = i;
                StringBuilder id = new StringBuilder();
                while (i < len && (Character.isLetterOrDigit(input.charAt(i))
                        || input.charAt(i) == '_' || input.charAt(i) == '$')) {
                    id.append(Character.toUpperCase(input.charAt(i)));
                    i++;
                }
                String idStr = id.toString();
                tokens.add(new Token(
                        RESERVED_WORDS.contains(idStr) ? TokenType.KEYWORD : TokenType.VARIABLE,
                        idStr, start));
                continue;
            }

            throw new FormulaParseException(4502, "Unexpected '" + c + "' at " + i, i);
        }
        return tokens;
    }

    // ---- helpers ----

    private record StringResult(Token token, String lexeme, int endPos) {}

    private static StringResult readString(String input, int start) {
        char delim = input.charAt(start);
        char close = (delim == '"') ? '"' : '}';
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        int len = input.length();
        while (i < len) {
            char c = input.charAt(i);
            if (c == close)
                return new StringResult(
                        new Token(TokenType.CONST_STRING, sb.toString(), start), sb.toString(), i + 1);
            if (c == '\\' && i + 1 < len) {
                char next = input.charAt(i + 1);
                if (next == close || next == '\\' || (delim == '{' && next == '"')) {
                    sb.append(next); i += 2;
                } else { sb.append(c); i++; }
            } else { sb.append(c); i++; }
        }
        throw new FormulaParseException(4501, "Unclosed string at " + start, start);
    }

    private static Token tryReadNumber(String input, int start) {
        int i = start, len = input.length();
        StringBuilder num = new StringBuilder();
        char first = input.charAt(i);
        if (first == '+' || first == '-') { num.append(first); i++; }
        boolean hasDigits = false;
        while (i < len && Character.isDigit(input.charAt(i))) { num.append(input.charAt(i)); i++; hasDigits = true; }
        if (i < len && input.charAt(i) == '.') { num.append('.'); i++;
            while (i < len && Character.isDigit(input.charAt(i))) { num.append(input.charAt(i)); i++; hasDigits = true; } }
        if (!hasDigits) return null;
        if (i < len && (input.charAt(i) == 'e' || input.charAt(i) == 'E')) { num.append(input.charAt(i)); i++;
            if (i < len && (input.charAt(i) == '+' || input.charAt(i) == '-')) { num.append(input.charAt(i)); i++; }
            while (i < len && Character.isDigit(input.charAt(i))) { num.append(input.charAt(i)); i++; } }
        return new Token(TokenType.CONST_NUMBER, num.toString(), start);
    }

    private static boolean matchWord(String input, int pos, String word) {
        int rem = input.length() - pos;
        if (rem < word.length()) return false;
        if (!input.regionMatches(true, pos, word, 0, word.length())) return false;
        int after = pos + word.length();
        return after >= input.length() || !Character.isLetter(input.charAt(after));
    }
}
