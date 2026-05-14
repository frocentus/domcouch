package com.domcouch.formula;

import java.util.List;

/**
 * A pre-parsed formula that can be evaluated against multiple contexts
 * without re-parsing. Created by {@link FormulaTranslator#compile(String)}.
 */
public final class CompiledFormula {

    private final List<Expr> statements;
    private final String source;

    CompiledFormula(List<Expr> statements, String source) {
        this.statements = List.copyOf(statements);
        this.source = source;
    }

    /** @return the original formula source text */
    public String source() { return source; }

    /** @return the number of statements in this formula */
    public int statementCount() { return statements.size(); }

    /** Internal: the parsed AST. */
    List<Expr> statements() { return statements; }

    @Override
    public String toString() {
        return "CompiledFormula(" + statements.size() + " stmts: " +
                (source.length() > 50 ? source.substring(0, 47) + "..." : source) + ")";
    }
}
