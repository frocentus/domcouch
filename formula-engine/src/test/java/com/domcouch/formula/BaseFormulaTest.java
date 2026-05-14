package com.domcouch.formula;

import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared base for formula engine tests. Provides common context setup
 * and eval helper.
 */
abstract class BaseFormulaTest {

    protected Evaluator evaluator;
    protected Map<String, Object> vars;

    @BeforeEach
    void setUp() {
        evaluator = new Evaluator("Alice");
        vars = new HashMap<>();
        vars.put("CREATED", "2024-01-15T09:30:00Z");
        vars.put("COMPANY", "Acme Inc.");
        vars.put("SUBJECT", "Hello World");
        vars.put("BODY", "The quick brown fox jumps over the lazy dog");
        vars.put("CATEGORIES", List.of("A", "B", "C", "D", "E"));
    }

    protected FormulaContext ctx() { return vars::get; }

    protected Object eval(String formula) {
        return evaluator.evalExpr(formula, ctx());
    }
}
