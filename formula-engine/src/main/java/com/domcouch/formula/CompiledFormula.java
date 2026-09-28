package com.domcouch.formula;

import com.domcouch.formula.translate.FormulaTranslator;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A pre-parsed formula that can be evaluated against multiple contexts
 * without re-parsing. Created by {@link FormulaTranslator#compile(String)}.
 */
public final class CompiledFormula {

    private final List<Expr> statements;
    private final String source;
    private final Set<String> referencedFields;
    private final Set<String> referencedFunctions;

    public CompiledFormula(List<Expr> statements, String source) {
        this.statements = List.copyOf(statements);
        this.source = source;

        Set<String> variables = new LinkedHashSet<>();
        Set<String> tempVariables = new LinkedHashSet<>();
        Set<String> functions = new LinkedHashSet<>();
        for (Expr stmt : this.statements) collect(stmt, variables, tempVariables, functions);
        variables.removeAll(tempVariables);
        this.referencedFields = Collections.unmodifiableSet(variables);
        this.referencedFunctions = Collections.unmodifiableSet(functions);
    }

    /** @return the original formula source text */
    public String source() { return source; }

    /** @return the number of statements in this formula */
    public int statementCount() { return statements.size(); }

    /** Internal: the parsed AST. */
    public List<Expr> statements() { return statements; }

    /**
     * @return the document fields this formula reads or writes (uppercase),
     *         excluding temporary variables assigned with {@code :=}.
     *         Useful to re-evaluate a hide-when only when one of these fields changes.
     */
    public Set<String> referencedFields() { return referencedFields; }

    /** @return the @Functions this formula calls (uppercase, without {@code @}) */
    public Set<String> referencedFunctions() { return referencedFunctions; }

    private static void collect(Expr expr, Set<String> vars, Set<String> temps, Set<String> fns) {
        if (expr == null) return; // unary operators are BinaryOp(null, op, operand)
        switch (expr) {
            case Expr.Variable v -> vars.add(v.name());
            case Expr.FunctionCall fc -> {
                fns.add(fc.name());
                for (Expr arg : fc.args()) collect(arg, vars, temps, fns);
            }
            case Expr.BinaryOp bo -> {
                collect(bo.left(), vars, temps, fns);
                collect(bo.right(), vars, temps, fns);
            }
            case Expr.Assignment a -> {
                if (a.target() instanceof Expr.Variable v) temps.add(v.name());
                collect(a.value(), vars, temps, fns);
            }
            case Expr.FieldAssign fa -> {
                collect(fa.target(), vars, temps, fns);
                collect(fa.value(), vars, temps, fns);
            }
            case Expr.DefaultAssign da -> {
                collect(da.target(), vars, temps, fns);
                collect(da.value(), vars, temps, fns);
            }
            case Expr.EnvironmentAssign ea -> collect(ea.value(), vars, temps, fns);
            case Expr.KeywordStatement ks -> collect(ks.body(), vars, temps, fns);
            case Expr.DeleteField df -> collect(df.target(), vars, temps, fns);
            case Expr.StringConst s -> { }
            case Expr.NumberConst n -> { }
            case Expr.DateTimeConst d -> { }
            case Expr.KeywordExpr kw -> { }
            case Expr.Comment c -> { }
        }
    }

    @Override
    public String toString() {
        return "CompiledFormula(" + statements.size() + " stmts: " +
                (source.length() > 50 ? source.substring(0, 47) + "..." : source) + ")";
    }
}
