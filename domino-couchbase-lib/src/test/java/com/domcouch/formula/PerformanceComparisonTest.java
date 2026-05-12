package com.domcouch.formula;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance comparison: old regex-based toN1ql() vs new Lexer/Parser pipeline.
 */
@DisplayName("Performance Comparison")
class PerformanceComparisonTest {

    private static final int WARMUP = 500;
    private static final int ITERATIONS = 10_000;
    private static final double NANOS_PER_MS = 1_000_000.0;

    private final FormulaTranslator translator = new FormulaTranslator("Alice");
    private final FormulaContext ctx;

    {
        Map<String, Object> vars = new HashMap<>();
        vars.put("FIRSTNAME", "John");
        vars.put("LASTNAME", "Smith");
        vars.put("COMPANY", "Acme Inc.");
        vars.put("SALARY", 95000.0);
        vars.put("STATUS", "Active");
        vars.put("SUBJECT", "Hello World");
        vars.put("CREATED", "2024-01-01T00:00:00Z");
        ctx = vars::get;
    }

    // ================================================================
    // Simple formulas (short)
    // ================================================================

    @Test @DisplayName("Simple: toN1ql vs evaluate (LastName + \", \" + FirstName)")
    void simpleConcat() {
        String formula = "LastName + \", \" + FirstName";

        // Old regex path (toN1ql)
        long t1 = benchmark(() -> translator.toN1ql(formula));
        String n1qlResult = translator.toN1ql(formula);

        // New Lexer/Parser/Evaluator path (evaluate)
        long t2 = benchmark(() -> translator.evaluate(formula, ctx));
        Object evalResult = translator.evaluate(formula, ctx);

        System.out.printf("%n=== Simple concat: \"%s\"%n", formula);
        System.out.printf("  toN1ql:     %.3f ms  →  %s%n", t1 / NANOS_PER_MS, n1qlResult);
        System.out.printf("  evaluate:   %.3f ms  →  %s%n", t2 / NANOS_PER_MS, evalResult);
        System.out.printf("  ratio:      %.1fx (evaluate/toN1ql)%n", (double) t2 / t1);

        assertEquals("Smith, John", evalResult);
    }

    @Test @DisplayName("Simple: toN1ql vs evaluate (simple field ref)")
    void simpleFieldRef() {
        String formula = "FirstName";

        long t1 = benchmark(() -> translator.toN1ql(formula));
        long t2 = benchmark(() -> translator.evaluate(formula, ctx));

        System.out.printf("%n=== Simple field ref: \"%s\"%n", formula);
        System.out.printf("  toN1ql:     %.3f ms%n", t1 / NANOS_PER_MS);
        System.out.printf("  evaluate:   %.3f ms%n", t2 / NANOS_PER_MS);
        System.out.printf("  ratio:      %.1fx%n", (double) t2 / t1);
    }

    // ================================================================
    // Medium formulas
    // ================================================================

    @Test @DisplayName("Medium: selection formula with @Functions")
    void mediumSelectionFormula() {
        String formula = "Form = \"Person\" & Status = \"Active\" & @Contains(Subject; \"urgent\")";

        long t1 = benchmark(() -> translator.toN1ql(formula));
        long t2 = benchmark(() -> translator.evaluate(formula, ctx));

        System.out.printf("%n=== Medium selection: \"%s\"%n", formula);
        System.out.printf("  toN1ql:     %.3f ms%n", t1 / NANOS_PER_MS);
        System.out.printf("  evaluate:   %.3f ms%n", t2 / NANOS_PER_MS);
        System.out.printf("  ratio:      %.1fx%n", (double) t2 / t1);
    }

    @Test @DisplayName("Medium: computed field formula")
    void mediumComputedFormula() {
        String formula = "\"From: \" + FirstName + \" \" + LastName + \" (\" + @Text(Salary) + \")\"";

        long t2 = benchmark(() -> translator.evaluate(formula, ctx));
        Object result = translator.evaluate(formula, ctx);

        System.out.printf("%n=== Medium computed: \"%s\"%n", formula);
        System.out.printf("  evaluate:   %.3f ms  →  %s%n", t2 / NANOS_PER_MS, result);
    }

    // ================================================================
    // Complex formulas
    // ================================================================

    @Test @DisplayName("Complex: @If with nested expressions")
    void complexIfFormula() {
        String formula = "@If(Salary > 100000; \"High Earner: \" + @Text(Salary); " +
                "@If(Salary > 50000; \"Mid: \" + @Text(Salary); \"Low: \" + @Text(Salary)))";

        long t2 = benchmark(() -> translator.evaluate(formula, ctx));
        Object result = translator.evaluate(formula, ctx);

        System.out.printf("%n=== Complex @If: \"%s\"%n", formula);
        System.out.printf("  evaluate:   %.3f ms  →  %s%n", t2 / NANOS_PER_MS, result);
    }

    // ================================================================
    // Pipeline breakdown (Lexer vs Parser vs Evaluator)
    // ================================================================

    @Test @DisplayName("Pipeline stage breakdown")
    void pipelineBreakdown() {
        String formula = "LastName + \", \" + FirstName";

        // Warm up
        for (int i = 0; i < WARMUP; i++) {
            translator.evaluate(formula, ctx);
        }

        // Measure stages separately
        long lexTime = 0, parseTime = 0, evalTime = 0;
        var tokens = Lexer.tokenize(formula);

        for (int i = 0; i < ITERATIONS; i++) {
            long t0 = System.nanoTime();
            tokens = Lexer.tokenize(formula);
            long t1 = System.nanoTime();
            var stmts = new Parser(tokens).parse();
            long t2 = System.nanoTime();
            new Evaluator().evalExpr(formula, ctx);
            long t3 = System.nanoTime();

            lexTime += (t1 - t0);
            parseTime += (t2 - t1);
            evalTime += (t3 - t2);
        }

        double totalMs = (lexTime + parseTime + evalTime) / NANOS_PER_MS;
        System.out.printf("%n=== Pipeline breakdown (%d iterations):%n", ITERATIONS);
        System.out.printf("  Lexer:     %5.1f ms  (%.0f%%)%n",
                lexTime / NANOS_PER_MS, 100.0 * lexTime / (lexTime + parseTime + evalTime));
        System.out.printf("  Parser:    %5.1f ms  (%.0f%%)%n",
                parseTime / NANOS_PER_MS, 100.0 * parseTime / (lexTime + parseTime + evalTime));
        System.out.printf("  Evaluator: %5.1f ms  (%.0f%%)%n",
                evalTime / NANOS_PER_MS, 100.0 * evalTime / (lexTime + parseTime + evalTime));
        System.out.printf("  Total:     %5.1f ms  (%.0f ops/ms)%n",
                totalMs, ITERATIONS / totalMs);

        // Sanity check
        assertEquals("Smith, John", translator.evaluate(formula, ctx));
    }

    @Test @DisplayName("Throughput: evaluate ops/sec")
    void throughputTest() {
        String formula = "LastName + \", \" + FirstName";

        // Warm up
        for (int i = 0; i < WARMUP; i++) {
            translator.evaluate(formula, ctx);
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            translator.evaluate(formula, ctx);
        }
        long elapsed = System.nanoTime() - start;

        double opsPerSec = ITERATIONS / (elapsed / 1_000_000_000.0);
        double usPerOp = (elapsed / 1000.0) / ITERATIONS;

        System.out.printf("%n=== Throughput: %d iterations%n", ITERATIONS);
        System.out.printf("  Elapsed:    %.1f ms%n", elapsed / NANOS_PER_MS);
        System.out.printf("  Ops/sec:    %,.0f%n", opsPerSec);
        System.out.printf("  µs/op:      %.2f%n", usPerOp);

        assertTrue(opsPerSec > 1000, "Should handle at least 1000 ops/sec");
    }

    @Test @DisplayName("Memory: objects created per evaluation")
    void memoryEstimate() {
        String formula = "LastName + \", \" + FirstName";

        // Trigger GC
        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        for (int i = 0; i < 10_000; i++) {
            translator.evaluate(formula, ctx);
        }

        System.gc();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long diff = memAfter - memBefore;

        System.out.printf("%n=== Memory (10k evaluations of \"%s\"):%n", formula);
        System.out.printf("  Heap delta: %d bytes (%.1f KB)%n", diff, diff / 1024.0);
        System.out.printf("  Per eval:   ~%d bytes%n", Math.abs(diff) / 10_000);
    }

    // ================================================================
    // Cached vs uncached (10k documents, 3 computed fields each)
    // ================================================================

    @Test @DisplayName("Cached: 30k evaluations on pre-compiled formulas")
    void cachedVsUncached() {
        String[] formulas = {
                "LastName + \", \" + FirstName",
                "@UpperCase(Company)",
                "@If(Salary > 100000; \"High\"; \"Standard\")"
        };

        // Compile once (simulates form design loading)
        CompiledFormula[] compiled = new CompiledFormula[formulas.length];
        for (int i = 0; i < formulas.length; i++) {
            compiled[i] = translator.compile(formulas[i]);
        }

        int docs = 10_000;

        // Uncached: parse + eval for every doc × field
        long t1 = System.nanoTime();
        for (int d = 0; d < docs; d++) {
            for (String f : formulas) {
                translator.evaluate(f, ctx);
            }
        }
        long uncached = System.nanoTime() - t1;

        // Cached: eval only (AST pre-compiled)
        long t2 = System.nanoTime();
        for (int d = 0; d < docs; d++) {
            for (CompiledFormula cf : compiled) {
                translator.evaluate(cf, ctx);
            }
        }
        long cached = System.nanoTime() - t2;

        double uncachedMs = uncached / NANOS_PER_MS;
        double cachedMs = cached / NANOS_PER_MS;

        System.out.printf("%n=== Cached vs Uncached (%d docs × %d fields = %,d evaluations):%n",
                docs, formulas.length, docs * formulas.length);
        System.out.printf("  Uncached:  %8.1f ms  (%.0f evals/ms)%n",
                uncachedMs, (docs * formulas.length) / uncachedMs);
        System.out.printf("  Cached:    %8.1f ms  (%.0f evals/ms)%n",
                cachedMs, (docs * formulas.length) / cachedMs);
        System.out.printf("  Speedup:   %.1fx%n", uncachedMs / cachedMs);

        assertTrue(cached < uncached, "Cached should be faster than uncached");
    }

    // ---- helper ----

    private long benchmark(Runnable task) {
        // Warm up
        for (int i = 0; i < WARMUP; i++) task.run();
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) task.run();
        return (System.nanoTime() - start) / ITERATIONS; // avg nanos per call
    }
}
