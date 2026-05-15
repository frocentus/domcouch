package com.domcouch.formula;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for @DbLookup and @DbColumn formula evaluation.
 * Uses a mock FormulaContext — does not require Couchbase.
 */
@DisplayName("@DbLookup / @DbColumn")
class DbLookupTest extends BaseFormulaTest {

    @Test @DisplayName("@DbLookup delegates to ctx.dbLookup")
    void dbLookupDelegates() {
        var ctx = new MockContext(Map.of(), List.of("Alice", "Bob"));
        assertEquals(List.of("Alice", "Bob"),
                evaluator.evalExpr("@DbLookup(\"\"; \"\"; \"MyView\"; \"key\"; 2)", ctx));
    }

    @Test @DisplayName("@DbLookup with empty result returns empty")
    void dbLookupEmpty() {
        var ctx = new MockContext(Map.of(), List.of());
        assertEquals(List.of(), evaluator.evalExpr("@DbLookup(\"\"; \"\"; \"V\"; \"k\"; 1)", ctx));
    }

    @Test @DisplayName("@DbColumn delegates to ctx.dbColumn")
    void dbColumnDelegates() {
        var ctx = new MockContext(Map.of(), List.of("x", "y", "z"));
        assertEquals(List.of("x", "y", "z"),
                evaluator.evalExpr("@DbColumn(\"\"; \"\"; \"MyView\"; 3)", ctx));
    }

    @Test @DisplayName("@DbLookup without context support returns empty")
    void dbLookupNoContext() {
        assertEquals("", eval("@DbLookup(\"\"; \"\"; \"V\"; \"k\"; 1)"));
    }

    @Test @DisplayName("@DbColumn without context support returns empty")
    void dbColumnNoContext() {
        assertEquals("", eval("@DbColumn(\"\"; \"\"; \"V\"; 2)"));
    }

    /** Mock FormulaContext that implements dbLookup/dbColumn with test data. */
    static class MockContext implements FormulaContext {
        private final Map<String, Object> fields;
        private final List<Object> lookupResult;

        MockContext(Map<String, Object> fields, List<Object> lookupResult) {
            this.fields = fields;
            this.lookupResult = lookupResult;
        }

        @Override public Object resolve(String name) { return fields.get(name); }

        @Override
        public List<Object> dbLookup(String s, String d, String v, Object k, int c) {
            return lookupResult;
        }

        @Override
        public List<Object> dbColumn(String s, String d, String v, int c) {
            return lookupResult;
        }
    }
}
