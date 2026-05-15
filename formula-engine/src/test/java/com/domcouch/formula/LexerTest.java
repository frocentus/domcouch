package com.domcouch.formula;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("Lexer")
class LexerTest {

    // ---- Variables ----

    @Test
    @DisplayName("simple variable")
    void simpleVariable() {
        List<Token> tokens = Lexer.tokenize("FirstName");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "FIRSTNAME", 0);
    }

    @Test
    @DisplayName("variable with underscores")
    void variableWithUnderscores() {
        List<Token> tokens = Lexer.tokenize("my_field_name");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "MY_FIELD_NAME", 0);
    }

    @Test
    @DisplayName("variable with dollar sign (system field)")
    void variableWithDollar() {
        List<Token> tokens = Lexer.tokenize("$TITLE");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "$TITLE", 0);
    }

    @Test
    @DisplayName("variable case normalized to uppercase")
    void variableCaseNormalized() {
        List<Token> tokens = Lexer.tokenize("lastname");
        assertToken(tokens.get(0), TokenType.VARIABLE, "LASTNAME", 0);
    }

    // ---- String Constants ----

    @Test
    @DisplayName("double-quoted string constant")
    void doubleQuotedString() {
        List<Token> tokens = Lexer.tokenize("\"Hello World\"");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_STRING, "Hello World", 0);
    }

    @Test
    @DisplayName("double-quoted string with escaped quote")
    void doubleQuotedStringWithEscapedQuote() {
        List<Token> tokens = Lexer.tokenize("\"Type \\\"Yes\\\" or \\\"No\\\"\"");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_STRING, "Type \"Yes\" or \"No\"", 0);
    }

    @Test
    @DisplayName("double-quoted string with escaped backslash")
    void doubleQuotedStringWithEscapedBackslash() {
        List<Token> tokens = Lexer.tokenize("\"Type \\\\Yes\\\\ or \\\\No\\\\\"");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_STRING, "Type \\Yes\\ or \\No\\", 0);
    }

    @Test
    @DisplayName("brace-delimited string constant")
    void braceDelimitedString() {
        List<Token> tokens = Lexer.tokenize("{Hello World}");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_STRING, "Hello World", 0);
    }

    @Test
    @DisplayName("brace-delimited string with embedded quotes")
    void braceWithEmbeddedQuotes() {
        List<Token> tokens = Lexer.tokenize("{He said \"Hi\" to me}");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_STRING, "He said \"Hi\" to me", 0);
    }

    @Test
    @DisplayName("brace-delimited string with escaped right brace")
    void braceWithEscapedBrace() {
        List<Token> tokens = Lexer.tokenize("{Use \\} to close}");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_STRING, "Use } to close", 0);
    }

    // ---- Numeric Constants ----

    @Test
    @DisplayName("integer")
    void integerConstant() {
        List<Token> tokens = Lexer.tokenize("123");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "123", 0);
    }

    @Test
    @DisplayName("negative integer")
    void negativeInteger() {
        List<Token> tokens = Lexer.tokenize("-123");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "-123", 0);
    }

    @Test
    @DisplayName("decimal number")
    void decimalNumber() {
        List<Token> tokens = Lexer.tokenize("3.14");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "3.14", 0);
    }

    @Test
    @DisplayName("leading decimal point")
    void leadingDecimal() {
        List<Token> tokens = Lexer.tokenize(".123");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, ".123", 0);
    }

    @Test
    @DisplayName("trailing decimal point")
    void trailingDecimal() {
        List<Token> tokens = Lexer.tokenize("123.");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "123.", 0);
    }

    @Test
    @DisplayName("scientific notation positive exponent")
    void scientificPositive() {
        List<Token> tokens = Lexer.tokenize("123E2");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "123E2", 0);
    }

    @Test
    @DisplayName("scientific notation negative exponent")
    void scientificNegative() {
        List<Token> tokens = Lexer.tokenize("123E-2");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "123E-2", 0);
    }

    @Test
    @DisplayName("scientific notation lowercase e")
    void scientificLowercaseE() {
        List<Token> tokens = Lexer.tokenize("1.5e3");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "1.5e3", 0);
    }

    @Test
    @DisplayName("malformed scientific: 1e (no exponent digits)")
    void malformedScientificNoDigits() {
        assertThrows(FormulaParseException.class, () -> Lexer.tokenize("1e"));
    }

    @Test
    @DisplayName("malformed scientific: 1e+ (sign but no exponent digits)")
    void malformedScientificSignNoDigits() {
        assertThrows(FormulaParseException.class, () -> Lexer.tokenize("1e+"));
    }

    @Test
    @DisplayName("malformed scientific: -2E- (sign but no exponent digits)")
    void malformedScientificNegativeNoDigits() {
        assertThrows(FormulaParseException.class, () -> Lexer.tokenize("-2E-"));
    }

    @Test
    @DisplayName("number in quotes is string, not number")
    void numberInQuotesIsString() {
        List<Token> tokens = Lexer.tokenize("\"42\"");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_STRING, "42", 0);
    }

    // ---- Sign-prefix vs operator disambiguation ----

    @Test
    @DisplayName("minus after variable is operator, not sign prefix: a-5")
    void minusAfterVariable() {
        List<Token> tokens = Lexer.tokenize("a-5");
        assertEquals(3, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "A", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "-", 1);
        assertToken(tokens.get(2), TokenType.CONST_NUMBER, "5", 2);
    }

    @Test
    @DisplayName("minus at start is sign prefix: -5")
    void minusAtStart() {
        List<Token> tokens = Lexer.tokenize("-5");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_NUMBER, "-5", 0);
    }

    @Test
    @DisplayName("plus after variable is operator: a+5")
    void plusAfterVariable() {
        List<Token> tokens = Lexer.tokenize("a+5");
        assertEquals(3, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "A", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "+", 1);
        assertToken(tokens.get(2), TokenType.CONST_NUMBER, "5", 2);
    }

    // ---- Operators ----

    @Test
    @DisplayName("operators: plus minus mul div")
    void arithmeticOperators() {
        List<Token> tokens = Lexer.tokenize("+ - * /");
        assertEquals(4, tokens.size());
        assertToken(tokens.get(0), TokenType.OPERATOR, "+", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "-", 2);
        assertToken(tokens.get(2), TokenType.OPERATOR, "*", 4);
        assertToken(tokens.get(3), TokenType.OPERATOR, "/", 6);
    }

    @Test
    @DisplayName("operators: comparison")
    void comparisonOperators() {
        List<Token> tokens = Lexer.tokenize("= <> >< != > < >= <=");
        assertEquals(8, tokens.size());
        assertToken(tokens.get(0), TokenType.OPERATOR, "=", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "<>", 2);
        assertToken(tokens.get(2), TokenType.OPERATOR, "><", 5);
        assertToken(tokens.get(3), TokenType.OPERATOR, "!=", 8);
        assertToken(tokens.get(4), TokenType.OPERATOR, ">", 11);
        assertToken(tokens.get(5), TokenType.OPERATOR, "<", 13);
        assertToken(tokens.get(6), TokenType.OPERATOR, ">=", 15);
        assertToken(tokens.get(7), TokenType.OPERATOR, "<=", 18);
    }

    @Test
    @DisplayName("operators: logical and assignment")
    void logicalAndAssignmentOperators() {
        List<Token> tokens = Lexer.tokenize("& | ! :=");
        assertEquals(4, tokens.size());
        assertToken(tokens.get(0), TokenType.OPERATOR, "&", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "|", 2);
        assertToken(tokens.get(2), TokenType.OPERATOR, "!", 4);
        assertToken(tokens.get(3), TokenType.OPERATOR, ":=", 6);
    }

    @Test
    @DisplayName("operators: colon list constructor")
    void colonOperator() {
        List<Token> tokens = Lexer.tokenize(":");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.OPERATOR, ":", 0);
    }

    // ---- @Functions ----

    @Test
    @DisplayName("@function without arguments")
    void atFunctionNoArgs() {
        List<Token> tokens = Lexer.tokenize("@Created");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.AT_FUNCTION, "CREATED", 0);
    }

    @Test
    @DisplayName("@function case normalized")
    void atFunctionCaseNormalized() {
        List<Token> tokens = Lexer.tokenize("@trim");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.AT_FUNCTION, "TRIM", 0);
    }

    @Test
    @DisplayName("@function with parentheses")
    void atFunctionWithParens() {
        List<Token> tokens = Lexer.tokenize("@Trim(Subject)");
        assertEquals(4, tokens.size());
        assertToken(tokens.get(0), TokenType.AT_FUNCTION, "TRIM", 0);
        assertToken(tokens.get(1), TokenType.LPAREN, "(", 5);
        assertToken(tokens.get(2), TokenType.VARIABLE, "SUBJECT", 6);
        assertToken(tokens.get(3), TokenType.RPAREN, ")", 13);
    }

    // ---- Keywords ----

    @Test
    @DisplayName("reserved word keyword")
    void reservedWordKeyword() {
        List<Token> tokens = Lexer.tokenize("SELECT");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "SELECT", 0);
    }

    @Test
    @DisplayName("reserved word case normalized")
    void keywordCaseNormalized() {
        List<Token> tokens = Lexer.tokenize("select");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "SELECT", 0);
    }

    @Test
    @DisplayName("FIELD keyword")
    void fieldKeyword() {
        List<Token> tokens = Lexer.tokenize("FIELD");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "FIELD", 0);
    }

    @Test
    @DisplayName("DEFAULT keyword")
    void defaultKeyword() {
        List<Token> tokens = Lexer.tokenize("DEFAULT");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "DEFAULT", 0);
    }

    @Test
    @DisplayName("ENVIRONMENT keyword")
    void environmentKeyword() {
        List<Token> tokens = Lexer.tokenize("ENVIRONMENT");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "ENVIRONMENT", 0);
    }

    @Test
    @DisplayName("REM keyword alone produces COMMENT token")
    void remKeyword() {
        List<Token> tokens = Lexer.tokenize("REM");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.COMMENT, "", 0);
    }

    @Test
    @DisplayName("REM1 is an identifier, not a comment")
    void rem1IsIdentifier() {
        List<Token> tokens = Lexer.tokenize("REM1");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "REM1", 0);
    }

    @Test
    @DisplayName("REM_ is an identifier, not a comment")
    void remUnderscoreIsIdentifier() {
        List<Token> tokens = Lexer.tokenize("REM_");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "REM_", 0);
    }

    @Test
    @DisplayName("REM followed by space+string is a comment")
    void remSpaceStringIsComment() {
        List<Token> tokens = Lexer.tokenize("REM \"comment\"");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.COMMENT, "comment", 0);
    }

    @Test
    @DisplayName("bracket keyword [OK]")
    void bracketKeyword() {
        List<Token> tokens = Lexer.tokenize("[OK]");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "OK", 0);
    }

    @Test
    @DisplayName("bracket keyword [CANCEL]")
    void bracketKeywordCancel() {
        List<Token> tokens = Lexer.tokenize("[CANCEL]");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "CANCEL", 0);
    }

    // ---- Time-Date Constants ----

    @Test
    @DisplayName("time constant 24-hour")
    void timeConstant24h() {
        List<Token> tokens = Lexer.tokenize("[5:30]");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_DATETIME, "5:30", 0);
    }

    @Test
    @DisplayName("time constant 12-hour with AM/PM")
    void timeConstant12h() {
        List<Token> tokens = Lexer.tokenize("[5:30 PM]");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_DATETIME, "5:30 PM", 0);
    }

    @Test
    @DisplayName("date constant")
    void dateConstant() {
        List<Token> tokens = Lexer.tokenize("[6/15]");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_DATETIME, "6/15", 0);
    }

    @Test
    @DisplayName("date-time combined")
    void dateTimeCombined() {
        List<Token> tokens = Lexer.tokenize("[6/15 5:30 PM]");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.CONST_DATETIME, "6/15 5:30 PM", 0);
    }

    @Test
    @DisplayName("unknown bracket content is keyword if single word")
    void unknownBracketIsDatetime() {
        List<Token> tokens = Lexer.tokenize("[FooBar]");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "FOOBAR", 0);
    }

    // ---- Combined / Complex ----

    @Test
    @DisplayName("multiple tokens with spaces")
    void multipleTokensWithSpaces() {
        List<Token> tokens = Lexer.tokenize("LastName + \", \" + FirstName");
        assertEquals(5, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "LASTNAME", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "+", 9);
        assertToken(tokens.get(2), TokenType.CONST_STRING, ", ", 11);
        assertToken(tokens.get(3), TokenType.OPERATOR, "+", 16);
        assertToken(tokens.get(4), TokenType.VARIABLE, "FIRSTNAME", 18);
    }

    @Test
    @DisplayName("no spaces around operators")
    void noSpacesAroundOperators() {
        List<Token> tokens = Lexer.tokenize("LastName+\", \"+FirstName");
        assertEquals(5, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "LASTNAME", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "+", 8);
        assertToken(tokens.get(2), TokenType.CONST_STRING, ", ", 9);
        assertToken(tokens.get(3), TokenType.OPERATOR, "+", 13);
        assertToken(tokens.get(4), TokenType.VARIABLE, "FIRSTNAME", 14);
    }

    @Test
    @DisplayName("formula with assignment")
    void formulaWithAssignment() {
        List<Token> tokens = Lexer.tokenize("n := 1");
        assertEquals(3, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "N", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, ":=", 2);
        assertToken(tokens.get(2), TokenType.CONST_NUMBER, "1", 5);
    }

    @Test
    @DisplayName("formula with @If and semicolons")
    void formulaWithIf() {
        List<Token> tokens = Lexer.tokenize("@If(Condition; \"Yes\"; \"No\")");
        assertEquals(8, tokens.size());
        assertToken(tokens.get(0), TokenType.AT_FUNCTION, "IF", 0);
        assertToken(tokens.get(1), TokenType.LPAREN, "(", 3);
        assertToken(tokens.get(2), TokenType.VARIABLE, "CONDITION", 4);
        assertToken(tokens.get(3), TokenType.SEMICOLON, ";", 13);
        assertToken(tokens.get(4), TokenType.CONST_STRING, "Yes", 15);
        assertToken(tokens.get(5), TokenType.SEMICOLON, ";", 20);
        assertToken(tokens.get(6), TokenType.CONST_STRING, "No", 22);
        assertToken(tokens.get(7), TokenType.RPAREN, ")", 26);
    }

    // ---- Edge Cases ----

    @Test
    @DisplayName("empty string returns empty list")
    void emptyString() {
        List<Token> tokens = Lexer.tokenize("");
        assertTrue(tokens.isEmpty());
    }

    @Test
    @DisplayName("whitespace only returns empty list")
    void whitespaceOnly() {
        List<Token> tokens = Lexer.tokenize("   \t  ");
        assertTrue(tokens.isEmpty());
    }

    @Test
    @DisplayName("null input throws")
    void nullInputThrows() {
        assertThrows(NullPointerException.class, () -> Lexer.tokenize(null));
    }

    @Test
    @DisplayName("unclosed string throws")
    void unclosedStringThrows() {
        FormulaParseException ex = assertThrows(FormulaParseException.class,
                () -> Lexer.tokenize("\"unclosed"));
        assertTrue(ex.getMessage().contains("Unclosed"));
        assertEquals(4501, ex.id);
    }

    @Test
    @DisplayName("unclosed brace throws")
    void unclosedBraceThrows() {
        FormulaParseException ex = assertThrows(FormulaParseException.class,
                () -> Lexer.tokenize("{unclosed"));
        assertTrue(ex.getMessage().contains("Unclosed"));
        assertEquals(4501, ex.id);
    }

    // ---- Subscript ----

    @Test
    @DisplayName("subscript operator is two tokens")
    void subscriptOperator() {
        List<Token> tokens = Lexer.tokenize("Categories[1]");
        assertEquals(4, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "CATEGORIES", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, "[", 10);
        assertToken(tokens.get(2), TokenType.CONST_NUMBER, "1", 11);
        assertToken(tokens.get(3), TokenType.OPERATOR, "]", 12);
    }

    // ---- Comment ----

    @Test
    @DisplayName("REM with string comment")
    void remWithStringComment() {
        List<Token> tokens = Lexer.tokenize("REM \"a comment\"");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.COMMENT, "a comment", 0);
    }

    @Test
    @DisplayName("REM with brace comment")
    void remWithBraceComment() {
        List<Token> tokens = Lexer.tokenize("REM {a comment}");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.COMMENT, "a comment", 0);
    }

    @Test
    @DisplayName("REM empty")
    void remEmpty() {
        List<Token> tokens = Lexer.tokenize("REM");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.COMMENT, "", 0);
    }

    @Test
    @DisplayName("SELECT @All")
    void selectAll() {
        List<Token> tokens = Lexer.tokenize("SELECT @All");
        assertEquals(2, tokens.size());
        assertToken(tokens.get(0), TokenType.KEYWORD, "SELECT", 0);
        assertToken(tokens.get(1), TokenType.AT_FUNCTION, "ALL", 7);
    }

    @Test
    @DisplayName("SEMICOLON as statement separator followed by expression")
    void semicolonSeparatorThenExpression() {
        List<Token> tokens = Lexer.tokenize("x := 1; @UpperCase(x)");
        assertEquals(8, tokens.size());
        assertToken(tokens.get(0), TokenType.VARIABLE, "X", 0);
        assertToken(tokens.get(1), TokenType.OPERATOR, ":=", 2);
        assertToken(tokens.get(2), TokenType.CONST_NUMBER, "1", 5);
        assertToken(tokens.get(3), TokenType.SEMICOLON, ";", 6);
        assertToken(tokens.get(4), TokenType.AT_FUNCTION, "UPPERCASE", 8);
        assertToken(tokens.get(5), TokenType.LPAREN, "(", 18);
        assertToken(tokens.get(6), TokenType.VARIABLE, "X", 19);
        assertToken(tokens.get(7), TokenType.RPAREN, ")", 20);
    }

    // ---- Helper ----

    private void assertToken(Token token, TokenType expectedType, String expectedLexeme, int expectedPosition) {
        assertEquals(expectedType, token.type(), "token type");
        assertEquals(expectedLexeme, token.lexeme(), "token lexeme");
        assertEquals(expectedPosition, token.position(), "token position");
    }
}
