package com.domcouch.formula;

/**
 * Lexer token types for the Domino formula language.
 *
 * @see Lexer#tokenize(String)
 */
public enum TokenType {

    /** A variable reference (field name or temp variable), always uppercase. */
    VARIABLE,

    /** A string constant: {@code "..."} or {@code {...}}. Content is unescaped. */
    CONST_STRING,

    /** A numeric constant: integer, decimal, or scientific notation. */
    CONST_NUMBER,

    /** A time-date constant: bracket-enclosed time, date, or both. */
    CONST_DATETIME,

    /** A reserved word ({@code SELECT, FIELD, DEFAULT}) or bracket keyword ({@code [OK]}). */
    KEYWORD,

    /** An operator: {@code + - * / = <> != > < >= <= & | ! : := []}. */
    OPERATOR,

    /**
     * An {@code @Function} name. Always uppercase.
     * The following {@code (} and arguments are separate tokens.
     */
    AT_FUNCTION,

    /** Opening parenthesis {@code (}. */
    LPAREN,

    /** Closing parenthesis {@code )}. */
    RPAREN,

    /** Semicolon {@code ;} — separates statements and function arguments. */
    SEMICOLON,

    /** A {@code REM} comment. Lexeme is the comment text (empty if bare {@code REM}). */
    COMMENT,

    /** End-of-input sentinel (not produced by the Lexer). */
    EOF
}
