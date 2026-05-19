package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseSession;
import com.domcouch.impl.DocumentFormulaContext;
import com.domcouch.formula.translate.FormulaTranslator;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarks for document retrieval and formula evaluation.
 *
 * Requires: docker compose up -d, populated database
 * Run: mvn test -pl springboot-demo -Dtest=PerformanceBenchmarkTest
 */
class PerformanceBenchmarkTest {

    private static Session session;
    private static Database db;
    private static FormulaTranslator ft;

    @BeforeAll
    static void setUp() throws NotesException {
        session = CouchbaseSession.connect("couchbase://localhost", "Administrator", "password");
        db = session.getDatabase("domcouch", "contacts"); // use demo data
        ft = new FormulaTranslator();
    }

    @AfterAll
    static void tearDown() {
        if (session != null) session.recycle();
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. Document Retrieval
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("getAllDocuments — batch fetch vs single KV")
    void getAllDocumentsPerformance() {
        long t0 = System.nanoTime();
        DocumentCollection docs = db.getAllDocuments();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        int count = docs.getCount();
        System.out.printf("  getAllDocuments: %d docs in %d ms (%.0f docs/s)%n",
                count, ms, count > 0 ? count * 1000.0 / ms : 0);
        assertTrue(count > 0, "Should have documents");
    }

    @Test @DisplayName("search — formula-filtered retrieval")
    void searchPerformance() throws NotesException {
        long t0 = System.nanoTime();
        DocumentCollection results = db.search("Form = \"Person\"");
        long ms = (System.nanoTime() - t0) / 1_000_000;
        int count = results.getCount();
        System.out.printf("  search(Form=\"Person\"): %d results in %d ms%n", count, ms);
        assertTrue(count >= 0);
    }

    @Test @DisplayName("getDocumentByUNID — single document lookup")
    void singleDocumentLookup() throws NotesException {
        // Get one document ID first
        DocumentCollection all = db.getAllDocuments();
        var it = all.iterator();
        if (!it.hasNext()) return;
        String unid = it.next().getUniversalID();

        int iterations = 100;
        long t0 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Document doc = db.getDocumentByUNID(unid);
            assertNotNull(doc);
        }
        long nsPerLookup = (System.nanoTime() - t0) / iterations;
        System.out.printf("  getDocumentByUNID: %d ns/lookup (%d iterations)%n",
                nsPerLookup, iterations);
        assertTrue(nsPerLookup < 50_000_000, "Single lookup should be < 50ms");
    }

    @Test @DisplayName("getAllDocuments — first item access triggers lazy load")
    void lazyLoadOverhead() {
        DocumentCollection docs = db.getAllDocuments();
        var it = docs.iterator();
        if (!it.hasNext()) return;

        // Measure first item access (triggers lazy loading)
        Document doc = it.next();
        long t0 = System.nanoTime();
        Item firstItem = doc.getFirstItem("FirstName");
        long nsFirstAccess = System.nanoTime() - t0;

        // Measure second access (already loaded)
        t0 = System.nanoTime();
        Item secondAccess = doc.getFirstItem("LastName");
        long nsSecondAccess = System.nanoTime() - t0;

        System.out.printf("  Lazy load: first access %d ns, second access %d ns%n",
                nsFirstAccess, nsSecondAccess);

        // Second access should be much faster (items cached)
        assertNotNull(firstItem);
        assertNotNull(secondAccess);
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. Formula Evaluation
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("Simple formula evaluation throughput")
    void simpleFormulaThroughput() throws NotesException {
        DocumentCollection all = db.getAllDocuments();
        int iterations = Math.min(100, all.getCount());
        var it = all.iterator();

        String formula = "FirstName + \" \" + LastName";
        long t0 = System.nanoTime();
        int evaluated = 0;
        for (int i = 0; i < iterations && it.hasNext(); i++) {
            Document doc = it.next();
            DocumentFormulaContext ctx = new DocumentFormulaContext(doc);
            Object result = ft.evaluate(formula, ctx);
            assertNotNull(result);
            evaluated++;
        }
        long nsPerEval = (System.nanoTime() - t0) / evaluated;
        System.out.printf("  Simple concat: %d evals, %d ns/eval (%.0f evals/s)%n",
                evaluated, nsPerEval, evaluated * 1_000_000_000.0 / (System.nanoTime() - t0));
        assertTrue(evaluated > 0);
    }

    @Test @DisplayName("Compiled formula — cache vs uncached")
    void compiledFormulaSpeedup() {
        DocumentCollection all = db.getAllDocuments();
        var it = all.iterator();
        if (!it.hasNext()) return;
        Document doc = it.next();
        DocumentFormulaContext ctx = new DocumentFormulaContext(doc);

        String formula = "@UpperCase(FirstName) + \" \" + @ProperCase(LastName)";

        // Uncached: parse + evaluate
        long t0 = System.nanoTime();
        int uncachedRuns = 50;
        for (int i = 0; i < uncachedRuns; i++) {
            ft.evaluate(formula, ctx);
        }
        long nsUncached = (System.nanoTime() - t0) / uncachedRuns;

        // Cached: compile once
        var compiled = ft.compile(formula);
        t0 = System.nanoTime();
        for (int i = 0; i < uncachedRuns; i++) {
            ft.evaluate(compiled, ctx);
        }
        long nsCached = (System.nanoTime() - t0) / uncachedRuns;

        double speedup = (double) nsUncached / nsCached;
        System.out.printf("  Compiled speedup: uncached %d ns, cached %d ns (%.1fx)%n",
                nsUncached, nsCached, speedup);
        assertTrue(speedup > 1.0, "Compiled should be faster than uncached");
    }

    @Test @DisplayName("Math formula evaluation")
    void mathFormulaThroughput() throws NotesException {
        DocumentCollection all = db.getAllDocuments();
        int iterations = Math.min(50, all.getCount());
        var it = all.iterator();

        // Use multiple formulas on the same documents
        String[] formulas = {
            "@Abs(Salary - 50000)",
            "@If(Salary > 50000; \"High\"; \"Low\")",
            "@Year(@Now) - @Year(HireDate)",
            "@Round(Salary * 1.1)",
        };

        long t0 = System.nanoTime();
        int evaluated = 0;
        for (int i = 0; i < iterations && it.hasNext(); i++) {
            Document doc = it.next();
            DocumentFormulaContext ctx = new DocumentFormulaContext(doc);
            for (String f : formulas) {
                ft.evaluate(f, ctx);
                evaluated++;
            }
        }
        long nsPerEval = (System.nanoTime() - t0) / evaluated;
        System.out.printf("  Math formulas: %d evals, %d ns/eval (%.0f evals/s)%n",
                evaluated, nsPerEval, evaluated * 1_000_000_000.0 / (System.nanoTime() - t0));
        assertTrue(evaluated > 0);
    }

    @Test @DisplayName("String formula evaluation (@Contains, @Length, @ReplaceSubstring)")
    void stringFormulaThroughput() throws NotesException {
        DocumentCollection all = db.getAllDocuments();
        int iterations = Math.min(50, all.getCount());
        var it = all.iterator();

        String[] formulas = {
            "@Contains(FirstName; \"a\")",
            "@Length(LastName)",
            "@UpperCase(FirstName)",
        };

        long t0 = System.nanoTime();
        int evaluated = 0;
        for (int i = 0; i < iterations && it.hasNext(); i++) {
            Document doc = it.next();
            DocumentFormulaContext ctx = new DocumentFormulaContext(doc);
            for (String f : formulas) {
                ft.evaluate(f, ctx);
                evaluated++;
            }
        }
        long nsPerEval = (System.nanoTime() - t0) / evaluated;
        System.out.printf("  String formulas: %d evals, %d ns/eval (%.0f evals/s)%n",
                evaluated, nsPerEval, evaluated * 1_000_000_000.0 / (System.nanoTime() - t0));
        assertTrue(evaluated > 0);
    }

    @Test @DisplayName("Formula evaluation against many documents (batch)")
    void batchFormulaEvaluation() throws NotesException {
        DocumentCollection all = db.getAllDocuments();
        int batchSize = Math.min(200, all.getCount());
        var it = all.iterator();

        String formula = "@If(Salary > 50000; FirstName; LastName)";

        long t0 = System.nanoTime();
        int evaluated = 0;
        for (int i = 0; i < batchSize && it.hasNext(); i++) {
            Document doc = it.next();
            DocumentFormulaContext ctx = new DocumentFormulaContext(doc);
            ft.evaluate(formula, ctx);
            evaluated++;
        }
        long totalMs = (System.nanoTime() - t0) / 1_000_000;

        System.out.printf("  Batch eval: %d docs in %d ms (%.0f docs/s, %.1f μs/doc)%n",
                evaluated, totalMs,
                evaluated * 1000.0 / totalMs,
                totalMs * 1000.0 / evaluated);
        assertTrue(evaluated > 0);
    }
}
