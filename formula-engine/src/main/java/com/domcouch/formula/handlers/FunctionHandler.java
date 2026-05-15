package com.domcouch.formula.handlers;

import com.domcouch.formula.Evaluator;
import com.domcouch.formula.Expr;
import com.domcouch.formula.FormulaContext;

import java.util.List;

/**
 * Handler for a single @Function implementation.
 */
@FunctionalInterface
public interface FunctionHandler {
    /**
     * Evaluate the function call.
     *
     * @param evaluator the evaluator (for evaluating sub-expressions)
     * @param args      the unevaluated argument expressions
     * @param ctx       the formula context
     * @return the function's return value
     */
    Object call(Evaluator evaluator, List<Expr> args, FormulaContext ctx);
}
