package com.domcouch.formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent (Pratt) parser for Domino formulas.
 *
 * <h3>Operator precedence (higher number = tighter binding)</h3>
 * <table>
 *   <tr><td>1</td><td>{@code :=}</td><td>assignment (lowest)</td></tr>
 *   <tr><td>2</td><td>{@code & | !} (logical)</td><td></td></tr>
 *   <tr><td>3</td><td>{@code = <> != > < >= <=}</td><td>comparison</td></tr>
 *   <tr><td>4</td><td>{@code + -} (binary)</td><td>addition / concatenation</td></tr>
 *   <tr><td>5</td><td>{@code * /}</td><td>multiplication</td></tr>
 *   <tr><td>6</td><td>{@code + - !} (unary)</td><td>sign / logical NOT</td></tr>
 *   <tr><td>7</td><td>{@code :}</td><td>list constructor</td></tr>
 *   <tr><td>8</td><td>{@code []}</td><td>subscript (highest)</td></tr>
 * </table>
 */
public class Parser {

    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public List<Expr> parse() {
        List<Expr> statements = new ArrayList<>();
        while (!isAtEnd()) {
            while (match(TokenType.SEMICOLON)) advance();
            if (isAtEnd()) break;
            Expr stmt = parseStatement();
            if (stmt != null) {
                statements.add(stmt);
                // Consume optional semicolon after statement
                if (match(TokenType.SEMICOLON)) advance();
                // Reject adjacent bare values: LastName FirstName without operator
                else if (!isAtEnd() && isBareValue(stmt) && isExpressionStart(peek()))
                    throw error("Unexpected adjacent term: '" + peek().lexeme()
                            + "'. Values must be separated by an operator or ';'.");
            }
        }
        return statements;
    }

    /** True if the expression is a single bare value (variable, constant) with no operator. */
    private static boolean isBareValue(Expr expr) {
        return expr instanceof Expr.Variable
                || expr instanceof Expr.NumberConst
                || expr instanceof Expr.StringConst
                || expr instanceof Expr.DateTimeConst
                || expr instanceof Expr.KeywordExpr;
    }

    /** True if the token type can start a new expression. */
    private static boolean isExpressionStart(Token t) {
        return switch (t.type()) {
            case VARIABLE, CONST_NUMBER, CONST_STRING, CONST_DATETIME, AT_FUNCTION -> true;
            default -> false;
        };
    }

    // ---- Statements ----

    private Expr parseStatement() {
        if (match(TokenType.COMMENT)) return new Expr.Comment(advance().lexeme());

        if (match(TokenType.KEYWORD)) {
            String kw = peek().lexeme();
            return switch (kw) {
                case "SELECT" -> parseKeywordStatement();
                case "FIELD" -> parseKeywordAssign(KeywordKind.FIELD);
                case "DEFAULT" -> parseKeywordAssign(KeywordKind.DEFAULT);
                case "ENVIRONMENT" -> parseKeywordAssign(KeywordKind.ENVIRONMENT);
                default -> expression(0);
            };
        }
        return expression(0);
    }

    private enum KeywordKind { FIELD, DEFAULT, ENVIRONMENT }

    private Expr parseKeywordStatement() {
        advance(); // SELECT
        return new Expr.KeywordStatement("SELECT", expression(0));
    }

    private Expr parseKeywordAssign(KeywordKind kind) {
        advance(); // FIELD/DEFAULT/ENVIRONMENT
        if (!match(TokenType.VARIABLE)) throw error("Expected variable name after keyword");
        Expr target = new Expr.Variable(advance().lexeme());
        if (!(match(TokenType.OPERATOR) && peek().lexeme().equals(":=")))
            throw error("Expected ':=' after target variable");
        advance();
        Expr value = expression(0);
        return switch (kind) {
            case FIELD -> new Expr.FieldAssign(target, value);
            case DEFAULT -> new Expr.DefaultAssign(target, value);
            case ENVIRONMENT -> new Expr.EnvironmentAssign(target, value);
        };
    }

    // ---- Pratt expression parser (higher prec = tighter binding) ----

    /** Parse expression until hitting operator with precedence < minPrec. */
    private Expr expression(int minPrec) {
        Expr left = prefix();

        while (!isAtEnd() && !match(TokenType.SEMICOLON) && !match(TokenType.RPAREN)
                && match(TokenType.OPERATOR)) {
            Token op = peek();
            if (!isInfixOp(op.lexeme())) break;
            int prec = infixPrecedence(op.lexeme());
            if (prec < minPrec) break;

            if (op.lexeme().equals(":=")) {
                advance();
                left = new Expr.Assignment(left, expression(prec)); // right-assoc
                continue;
            }

            if (op.lexeme().equals("[")) {
                // Subscript: numeric and single-letter variable indices supported.
                // Multi-letter: use a temp variable (e.g., idx := n; items[idx]).
                advance();
                Expr index = expression(0);
                if (match(TokenType.OPERATOR) && peek().lexeme().equals("]")) advance();
                else throw error("Expected ']' after subscript");
                left = new Expr.BinaryOp(left, "[]", index);
                continue;
            }

            advance();
            left = new Expr.BinaryOp(left, op.lexeme(),
                    expression(isLeftAssoc(op.lexeme()) ? prec + 1 : prec));
        }
        return left;
    }

    private Expr prefix() {
        if (isAtEnd()) throw error("Unexpected end of formula");

        // Unary operators: + - at prec 6, ! at prec 2 (logical NOT binds loosely)
        if (match(TokenType.OPERATOR)) {
            String opLex = peek().lexeme();
            if (opLex.equals("+") || opLex.equals("-")) {
                advance();
                return new Expr.BinaryOp(null, opLex, expression(UNARY_PREC));
            }
            if (opLex.equals("!")) {
                advance();
                return new Expr.BinaryOp(null, "!", expression(2)); // logical NOT at prec 2
            }
        }

        Token t = advance();
        return switch (t.type()) {
            case VARIABLE -> new Expr.Variable(t.lexeme());
            case CONST_STRING -> new Expr.StringConst(t.lexeme());
            case CONST_NUMBER -> new Expr.NumberConst(Double.parseDouble(t.lexeme()));
            case CONST_DATETIME -> new Expr.DateTimeConst(t.lexeme());
            case KEYWORD -> {
                // FIELD, DEFAULT, ENVIRONMENT in expression context
                if (t.lexeme().equals("FIELD") || t.lexeme().equals("DEFAULT")
                        || t.lexeme().equals("ENVIRONMENT")) {
                    if (!match(TokenType.VARIABLE)) throw error("Expected variable after " + t.lexeme());
                    Expr target = new Expr.Variable(advance().lexeme());
                    if (!(match(TokenType.OPERATOR) && peek().lexeme().equals(":="))) throw error("Expected ':='");
                    advance();
                    Expr value = expression(0);
                    yield switch (t.lexeme()) {
                        case "FIELD" -> new Expr.FieldAssign(target, value);
                        case "DEFAULT" -> new Expr.DefaultAssign(target, value);
                        default -> new Expr.EnvironmentAssign(target, value);
                    };
                }
                yield new Expr.KeywordExpr(t.lexeme());
            }
            case AT_FUNCTION -> parseFunctionCall(t.lexeme());
            case LPAREN -> {
                Expr inner = expression(0);
                if (match(TokenType.RPAREN)) { advance(); yield inner; }
                throw error("Unclosed parenthesis");
            }
            default -> throw error("Unexpected token: " + t);
        };
    }

    private Expr parseFunctionCall(String name) {
        if (match(TokenType.LPAREN)) {
            advance();
            List<Expr> args = new ArrayList<>();
            if (!match(TokenType.RPAREN)) {
                args.add(expression(0));
                while (match(TokenType.SEMICOLON)) {
                    advance();
                    args.add(expression(0));
                }
            }
            if (match(TokenType.RPAREN)) advance();
            else throw error("Unclosed parenthesis in @" + name);
            return new Expr.FunctionCall(name, args);
        }
        return new Expr.FunctionCall(name, List.of());
    }

    // ---- Helpers ----

    private boolean isAtEnd() { return pos >= tokens.size(); }
    private Token peek() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }
    private boolean match(TokenType type) { return !isAtEnd() && peek().type() == type; }
    private FormulaParseException error(String msg) {
        return new FormulaParseException(4502, msg, isAtEnd() ? -1 : peek().position());
    }

    // ---- Precedence (higher = tighter) ----

    private static final int UNARY_PREC = 6;

    private static int infixPrecedence(String op) {
        return switch (op) {
            case "[" -> 8;                                // subscript
            case ":" -> 7;                                // list constructor
            case "*", "/", "**", "*/" -> 5;              // multiply/divide + permuted
            case "+", "-", "*+", "*-" -> 4;              // add/subtract + permuted
            case "=", "<>", "!=", "><", ">", "<", ">=", "<=" -> 3;  // comparison
            case "*=", "*!=", "*>", "*<", "*>=", "*<=" -> 3;    // permuted comparison
            case "&", "|", "!" -> 2;                      // logical
            case ":=" -> 1;                               // assignment
            default -> -1;
        };
    }

    private static boolean isInfixOp(String op) { return infixPrecedence(op) >= 0; }

    private static boolean isUnaryOp(String op) {
        return op.equals("+") || op.equals("-");
    }

    private static boolean isLeftAssoc(String op) {
        return !op.equals(":=") && !op.equals(":");
    }
}
