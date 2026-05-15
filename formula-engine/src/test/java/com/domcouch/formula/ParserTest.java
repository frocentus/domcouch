package com.domcouch.formula;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parser")
class ParserTest {

    // ---- Simple expressions ----

    @Test
    @DisplayName("single variable")
    void singleVariable() {
        List<Expr> stmts = parse("FirstName");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.Variable("FIRSTNAME"), stmts.get(0));
    }

    @Test
    @DisplayName("string constant")
    void stringConstant() {
        List<Expr> stmts = parse("\"Hello\"");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.StringConst("Hello"), stmts.get(0));
    }

    @Test
    @DisplayName("numeric constant")
    void numericConstant() {
        List<Expr> stmts = parse("42");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.NumberConst(42.0), stmts.get(0));
    }

    @Test
    @DisplayName("datetime constant")
    void datetimeConstant() {
        List<Expr> stmts = parse("[5:30 PM]");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.DateTimeConst("5:30 PM"), stmts.get(0));
    }

    @Test
    @DisplayName("keyword expression [OK]")
    void keywordExpression() {
        List<Expr> stmts = parse("[OK]");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.KeywordExpr("OK"), stmts.get(0));
    }

    // ---- Arithmetic ----

    @Test
    @DisplayName("simple addition")
    void simpleAddition() {
        List<Expr> stmts = parse("2 + 3");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.BinaryOp(new Expr.NumberConst(2.0), "+", new Expr.NumberConst(3.0)),
                stmts.get(0));
    }

    @Test
    @DisplayName("multiplication has higher precedence than addition")
    void multiplicationPrecedence() {
        List<Expr> stmts = parse("5 - 3 * 6 - 4");
        assertEquals(1, stmts.size());
        // Should parse as: 5 - (3 * 6) - 4
        Expr expr = stmts.get(0);
        assertTrue(expr instanceof Expr.BinaryOp);
        Expr.BinaryOp outer = (Expr.BinaryOp) expr;
        assertEquals("-", outer.op());
        assertEquals(new Expr.NumberConst(4.0), outer.right());
        // left should be: (5 - (3 * 6))
        assertTrue(outer.left() instanceof Expr.BinaryOp);
        Expr.BinaryOp inner = (Expr.BinaryOp) outer.left();
        assertEquals("-", inner.op());
        assertEquals(new Expr.NumberConst(5.0), inner.left());
        assertEquals(
                new Expr.BinaryOp(new Expr.NumberConst(3.0), "*", new Expr.NumberConst(6.0)),
                inner.right());
    }

    @Test
    @DisplayName("parentheses override precedence")
    void parenthesesOverride() {
        List<Expr> stmts = parse("(5 - 3) * (6 - 4)");
        assertEquals(1, stmts.size());
        Expr.BinaryOp expr = (Expr.BinaryOp) stmts.get(0);
        assertEquals("*", expr.op());
        assertEquals(
                new Expr.BinaryOp(new Expr.NumberConst(5.0), "-", new Expr.NumberConst(3.0)),
                expr.left());
        assertEquals(
                new Expr.BinaryOp(new Expr.NumberConst(6.0), "-", new Expr.NumberConst(4.0)),
                expr.right());
    }

    // ---- String concatenation ----

    @Test
    @DisplayName("string concatenation")
    void stringConcatenation() {
        List<Expr> stmts = parse("CompanyName + \", Inc.\"");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.BinaryOp(new Expr.Variable("COMPANYNAME"), "+", new Expr.StringConst(", Inc.")),
                stmts.get(0));
    }

    @Test
    @DisplayName("multi-element concatenation")
    void multiConcatenation() {
        List<Expr> stmts = parse("LastName + \", \" + FirstName");
        assertEquals(1, stmts.size());
        Expr.BinaryOp expr = (Expr.BinaryOp) stmts.get(0);
        assertEquals("+", expr.op());
        assertEquals(new Expr.Variable("FIRSTNAME"), expr.right());
        assertEquals(
                new Expr.BinaryOp(new Expr.Variable("LASTNAME"), "+", new Expr.StringConst(", ")),
                expr.left());
    }

    // ---- Comparison ----

    @Test
    @DisplayName("comparison operators")
    void comparisonOperators() {
        List<Expr> stmts = parse("Salary > 100000");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.BinaryOp(new Expr.Variable("SALARY"), ">", new Expr.NumberConst(100000.0)),
                stmts.get(0));
    }

    @Test
    @DisplayName("not equal operator")
    void notEqualOperator() {
        List<Expr> stmts = parse("Status != \"Active\"");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.BinaryOp(new Expr.Variable("STATUS"), "!=", new Expr.StringConst("Active")),
                stmts.get(0));
    }

    // ---- Logical operators ----

    @Test
    @DisplayName("logical AND between comparisons")
    void logicalAnd() {
        List<Expr> stmts = parse("4 = 2 + 2 & 5 = 3 + 2");
        assertEquals(1, stmts.size());
        Expr.BinaryOp expr = (Expr.BinaryOp) stmts.get(0);
        assertEquals("&", expr.op());
    }

    @Test
    @DisplayName("logical NOT has lowest precedence")
    void logicalNotPrecedence() {
        List<Expr> stmts = parse("! 5 = 2 + 2");
        assertEquals(1, stmts.size());
        // ! has lowest precedence → !(5 = 2+2)
        Expr.BinaryOp expr = (Expr.BinaryOp) stmts.get(0);
        assertEquals("!", expr.op());
        // right side should be the comparison, left is null/absent for unary
        assertNull(expr.left());
        assertEquals(
                new Expr.BinaryOp(new Expr.NumberConst(5.0), "=",
                        new Expr.BinaryOp(new Expr.NumberConst(2.0), "+", new Expr.NumberConst(2.0))),
                expr.right());
    }

    // ---- Assignment ----

    @Test
    @DisplayName("simple assignment")
    void simpleAssignment() {
        List<Expr> stmts = parse("n := 1");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.Assignment(new Expr.Variable("N"), new Expr.NumberConst(1.0)),
                stmts.get(0));
    }

    @Test
    @DisplayName("assignment with expression")
    void assignmentWithExpression() {
        List<Expr> stmts = parse("n := n + 1");
        assertEquals(1, stmts.size());
        Expr.Assignment assign = (Expr.Assignment) stmts.get(0);
        assertEquals(new Expr.Variable("N"), assign.target());
        assertEquals(
                new Expr.BinaryOp(new Expr.Variable("N"), "+", new Expr.NumberConst(1.0)),
                assign.value());
    }

    @Test
    @DisplayName("nested assignment")
    void nestedAssignment() {
        List<Expr> stmts = parse("city1Upper := @UpperCase(city1 := \"London\")");
        assertEquals(1, stmts.size());
        Expr.Assignment outer = (Expr.Assignment) stmts.get(0);
        assertEquals(new Expr.Variable("CITY1UPPER"), outer.target());
        // value should be @UpperCase with a nested Assignment inside
        Expr.FunctionCall call = (Expr.FunctionCall) outer.value();
        assertEquals("UPPERCASE", call.name());
        assertEquals(1, call.args().size());
        assertEquals(
                new Expr.Assignment(new Expr.Variable("CITY1"), new Expr.StringConst("London")),
                call.args().get(0));
    }

    // ---- FIELD / DEFAULT / ENVIRONMENT ----

    @Test
    @DisplayName("FIELD assignment")
    void fieldAssignment() {
        List<Expr> stmts = parse("FIELD Subject := \"No Subject\"");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.FieldAssign(new Expr.Variable("SUBJECT"), new Expr.StringConst("No Subject")),
                stmts.get(0));
    }

    @Test
    @DisplayName("DEFAULT assignment")
    void defaultAssignment() {
        List<Expr> stmts = parse("DEFAULT KeyThought := Topic");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.DefaultAssign(new Expr.Variable("KEYTHOUGHT"), new Expr.Variable("TOPIC")),
                stmts.get(0));
    }

    @Test
    @DisplayName("ENVIRONMENT assignment")
    void environmentAssignment() {
        List<Expr> stmts = parse("ENVIRONMENT OrderNumber := @Text(NewOrderNumber)");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.EnvironmentAssign(new Expr.Variable("ORDERNUMBER"),
                        new Expr.FunctionCall("TEXT", List.of(new Expr.Variable("NEWORDERNUMBER")))),
                stmts.get(0));
    }

    // ---- @Functions ----

    @Test
    @DisplayName("@function without arguments")
    void atFunctionNoArgs() {
        List<Expr> stmts = parse("@Created");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.FunctionCall("CREATED", List.of()), stmts.get(0));
    }

    @Test
    @DisplayName("@function with one argument")
    void atFunctionOneArg() {
        List<Expr> stmts = parse("@Trim(Subject)");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.FunctionCall("TRIM", List.of(new Expr.Variable("SUBJECT"))),
                stmts.get(0));
    }

    @Test
    @DisplayName("@function with multiple arguments")
    void atFunctionMultipleArgs() {
        List<Expr> stmts = parse("@If(Condition; \"Yes\"; \"No\")");
        assertEquals(1, stmts.size());
        Expr.FunctionCall call = (Expr.FunctionCall) stmts.get(0);
        assertEquals("IF", call.name());
        assertEquals(3, call.args().size());
        assertEquals(new Expr.Variable("CONDITION"), call.args().get(0));
        assertEquals(new Expr.StringConst("Yes"), call.args().get(1));
        assertEquals(new Expr.StringConst("No"), call.args().get(2));
    }

    @Test
    @DisplayName("@function with keyword argument")
    void atFunctionWithKeywordArg() {
        List<Expr> stmts = parse("@Prompt([OK]; \"Title\"; @DbTitle)");
        assertEquals(1, stmts.size());
        Expr.FunctionCall call = (Expr.FunctionCall) stmts.get(0);
        assertEquals("PROMPT", call.name());
        assertEquals(3, call.args().size());
        assertEquals(new Expr.KeywordExpr("OK"), call.args().get(0));
    }

    // ---- List constructor ----

    @Test
    @DisplayName("list constructor with colon")
    void listConstructor() {
        List<Expr> stmts = parse("\"London\" : \"New York\" : \"Tokyo\"");
        assertEquals(1, stmts.size());
        // right-associative: "London" : ("New York" : "Tokyo")
        Expr.BinaryOp expr = (Expr.BinaryOp) stmts.get(0);
        assertEquals(":", expr.op());
        assertEquals(new Expr.StringConst("London"), expr.left());
        assertEquals(
                new Expr.BinaryOp(new Expr.StringConst("New York"), ":", new Expr.StringConst("Tokyo")),
                expr.right());
    }

    @Test
    @DisplayName("unary minus on number before colon — Lexer consumes -3 as single token")
    void unaryMinusOnList() {
        List<Expr> stmts = parse("-3:4");
        assertEquals(1, stmts.size());
        // Lexer produces NumberConst(-3), so : creates list (-3, 4)
        Expr.BinaryOp expr = (Expr.BinaryOp) stmts.get(0);
        assertEquals(":", expr.op());
        assertEquals(new Expr.NumberConst(-3.0), expr.left());
        assertEquals(new Expr.NumberConst(4.0), expr.right());
    }

    // ---- Subscript ----

    @Test
    @DisplayName("subscript operator")
    void subscriptOperator() {
        List<Expr> stmts = parse("Categories[2]");
        assertEquals(1, stmts.size());
        // subscript is BinaryOp with op="[]"
        Expr.BinaryOp sub = (Expr.BinaryOp) stmts.get(0);
        assertEquals("[]", sub.op());
        assertEquals(new Expr.Variable("CATEGORIES"), sub.left());
        assertEquals(new Expr.NumberConst(2.0), sub.right());
    }

    @Test
    @DisplayName("subscript with expression index")
    void subscriptWithExpression() {
        List<Expr> stmts = parse("Categories[n + 1]");
        assertEquals(1, stmts.size());
        Expr.BinaryOp sub = (Expr.BinaryOp) stmts.get(0);
        assertEquals("[]", sub.op());
        assertEquals(new Expr.Variable("CATEGORIES"), sub.left());
        assertEquals(
                new Expr.BinaryOp(new Expr.Variable("N"), "+", new Expr.NumberConst(1.0)),
                sub.right());
    }

    // ---- Multiple statements ----

    @Test
    @DisplayName("semicolon-separated statements")
    void semicolonSeparatedStatements() {
        List<Expr> stmts = parse("x := 1; @UpperCase(x)");
        assertEquals(2, stmts.size());
        assertEquals(
                new Expr.Assignment(new Expr.Variable("X"), new Expr.NumberConst(1.0)),
                stmts.get(0));
        assertEquals(
                new Expr.FunctionCall("UPPERCASE", List.of(new Expr.Variable("X"))),
                stmts.get(1));
    }

    @Test
    @DisplayName("FIELD statement followed by expression")
    void fieldThenExpression() {
        List<Expr> stmts = parse("FIELD MonthName := @Replace(month; nMonths; months); @All");
        assertEquals(2, stmts.size());
        assertTrue(stmts.get(0) instanceof Expr.FieldAssign);
        assertEquals(new Expr.FunctionCall("ALL", List.of()), stmts.get(1));
    }

    // ---- SELECT keyword ----

    @Test
    @DisplayName("SELECT @All")
    void selectAll() {
        List<Expr> stmts = parse("SELECT @All");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.KeywordStatement("SELECT", new Expr.FunctionCall("ALL", List.of())),
                stmts.get(0));
    }

    @Test
    @DisplayName("SELECT with comparison")
    void selectWithComparison() {
        List<Expr> stmts = parse("SELECT Form = \"Person\"");
        assertEquals(1, stmts.size());
        assertEquals(
                new Expr.KeywordStatement("SELECT",
                        new Expr.BinaryOp(new Expr.Variable("FORM"), "=", new Expr.StringConst("Person"))),
                stmts.get(0));
    }

    // ---- REM comments ----

    @Test
    @DisplayName("REM comment")
    void remComment() {
        List<Expr> stmts = parse("REM \"a comment\"");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.Comment("a comment"), stmts.get(0));
    }

    @Test
    @DisplayName("REM comment before code")
    void remBeforeCode() {
        List<Expr> stmts = parse("REM \"header\"; x := 1");
        assertEquals(2, stmts.size());
        assertEquals(new Expr.Comment("header"), stmts.get(0));
        assertTrue(stmts.get(1) instanceof Expr.Assignment);
    }

    // ---- Error cases ----

    @Test
    @DisplayName("unclosed parenthesis throws")
    void unclosedParenThrows() {
        FormulaParseException ex = assertThrows(FormulaParseException.class,
                () -> parse("@If(x; \"a\""));
        assertTrue(ex.getMessage().contains("parenthesis"));
    }

    @Test
    @DisplayName("empty input returns empty list")
    void emptyInput() {
        List<Expr> stmts = parse("");
        assertTrue(stmts.isEmpty());
    }

    // ---- Edge cases ----

    @Test
    @DisplayName("unary minus on number")
    void unaryMinusOnNumber() {
        // Note: Lexer consumes -123 as single CONST_NUMBER token
        List<Expr> stmts = parse("-123");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.NumberConst(-123.0), stmts.get(0));
    }

    @Test
    @DisplayName("unary minus on variable")
    void unaryMinusOnVariable() {
        List<Expr> stmts = parse("-x");
        assertEquals(1, stmts.size());
        Expr.BinaryOp expr = (Expr.BinaryOp) stmts.get(0);
        assertEquals("-", expr.op());
        assertNull(expr.left());
        assertEquals(new Expr.Variable("X"), expr.right());
    }

    @Test
    @DisplayName("parenthesized keyword")
    void parenthesizedKeyword() {
        List<Expr> stmts = parse("([OK])");
        assertEquals(1, stmts.size());
        assertEquals(new Expr.KeywordExpr("OK"), stmts.get(0));
    }

    // ---- Adjacent term rejection ----

    @Test
    @DisplayName("adjacent variables rejected: LastName FirstName")
    void adjacentVariablesRejected() {
        FormulaParseException ex = assertThrows(FormulaParseException.class,
                () -> parse("LastName FirstName"));
        assertTrue(ex.getMessage().contains("adjacent") || ex.getMessage().contains("Unexpected"));
    }

    @Test
    @DisplayName("adjacent constants rejected: 1 2")
    void adjacentNumbersRejected() {
        assertThrows(FormulaParseException.class, () -> parse("1 2"));
    }

    @Test
    @DisplayName("variable+operator+variable accepted: LastName+FirstName")
    void operatorBetweenVarsAccepted() {
        List<Expr> stmts = parse("LastName+FirstName");
        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof Expr.BinaryOp);
    }

    @Test
    @DisplayName("multi-statement with semicolons accepted: x:=1;y:=2")
    void multiStatementSemicolons() {
        List<Expr> stmts = parse("x:=1;y:=2");
        assertEquals(2, stmts.size());
    }

    // ---- Helper ----

    private static List<Expr> parse(String input) {
        return new Parser(Lexer.tokenize(input)).parse();
    }
}
