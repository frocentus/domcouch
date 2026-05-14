package com.domcouch.formula;

/**
 * A single token produced by the {@link Lexer}.
 *
 * @param type     the token's type
 * @param lexeme   the raw text content. For {@link TokenType#VARIABLE},
 *                 {@link TokenType#KEYWORD}, and {@link TokenType#AT_FUNCTION}
 *                 the value is normalized to uppercase. For
 *                 {@link TokenType#CONST_STRING} escape sequences are resolved.
 * @param position the character offset in the original source string (0-based)
 */
public record Token(TokenType type, String lexeme, int position) {

    @Override
    public String toString() {
        return type + "('" + lexeme + "')@" + position;
    }
}
