package com.domcouch.formula;

import java.util.List;

/**
 * AST node types for Domino formulas.
 * <p>
 * All variable, function, and keyword names are stored in UPPERCASE
 * (the Lexer normalizes case).
 */
public sealed interface Expr {

    /** A variable reference (field name or temp variable). */
    record Variable(String name) implements Expr {}

    /** A string constant (already unescaped). */
    record StringConst(String value) implements Expr {}

    /** A numeric constant. */
    record NumberConst(double value) implements Expr {}

    /** A time-date constant (raw bracket content, parsed at eval time). */
    record DateTimeConst(String raw) implements Expr {}

    /** A keyword value like [OK], [CANCEL]. */
    record KeywordExpr(String value) implements Expr {}

    /** An @Function call: @Name(arg1; arg2; ...). 0-arg functions have empty args list. */
    record FunctionCall(String name, List<Expr> args) implements Expr {}

    /** A binary operator: left OP right. OP is one of + - * / = <> != > < >= <= & | ! : := */
    record BinaryOp(Expr left, String op, Expr right) implements Expr {}

    /** A temporary variable assignment: x := expr. Returns the value. */
    record Assignment(Expr target, Expr value) implements Expr {}

    /** FIELD var := expr — writes to the document. Returns the value. */
    record FieldAssign(Expr target, Expr value) implements Expr {}

    /** DEFAULT var := expr — read with fallback. Returns the value. */
    record DefaultAssign(Expr target, Expr value) implements Expr {}

    /** ENVIRONMENT var := expr — environment variable. Phase 1: no-op, returns value. */
    record EnvironmentAssign(Expr target, Expr value) implements Expr {}

    /** SELECT expression — keyword statement. */
    record KeywordStatement(String keyword, Expr body) implements Expr {}

    /** @DeleteField — deletes a field from the document. */
    record DeleteField(Expr target) implements Expr {}

    /** REM comment — no value, documentation only. */
    record Comment(String text) implements Expr {}
}
