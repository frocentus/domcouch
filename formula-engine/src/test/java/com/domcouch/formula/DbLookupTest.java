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

    @Nested @DisplayName("Notes argument syntax")
    class NotesSyntax {

        @Test @DisplayName("class:cache is not passed as server")
        void classCacheIgnored() {
            var ctx = new MockContext(Map.of("PARTEI_ID", "P42"), List.of("SPÖ"));
            evaluator.evalExpr("@DbLookup(\"Notes\":\"ReCache\"; \"\"; \"Parteien_ID\"; Partei_ID; 5)", ctx);
            assertEquals(List.of("", "", "Parteien_ID", "P42", "column:5"), ctx.lastCall);
        }

        @Test @DisplayName("server:database list is split")
        void serverDatabaseSplit() {
            var ctx = new MockContext(Map.of(), List.of("x"));
            evaluator.evalExpr("@DbLookup(\"\"; \"CN=Srv/O=Org\":\"names.nsf\"; \"People\"; \"k\"; 2)", ctx);
            assertEquals(List.of("CN=Srv/O=Org", "names.nsf", "People", "k", "column:2"), ctx.lastCall);
        }

        @Test @DisplayName("single database value is a path/replica ID on the current server")
        void singleDatabaseValue() {
            var ctx = new MockContext(Map.of(), List.of("x"));
            evaluator.evalExpr("@DbColumn(\"\"; \"85255B6E004A6D12\"; \"People\"; 1)", ctx);
            assertEquals(List.of("", "85255B6E004A6D12", "People", "column:1"), ctx.lastCall);
        }

        @Test @DisplayName("field name as fifth argument uses the fieldName overload")
        void fieldNameLookup() {
            var ctx = new MockContext(Map.of(), List.of("Sozialdemokratische Partei"));
            assertEquals(List.of("Sozialdemokratische Partei"), evaluator.evalExpr(
                    "@DbLookup(\"Notes\":\"ReCache\"; \"\"; \"Parteien_ID\"; \"P42\"; \"Partei_Name\"; [FailSilent])", ctx));
            assertEquals(List.of("", "", "Parteien_ID", "P42", "field:Partei_Name"), ctx.lastCall);
        }

        @Test @DisplayName("[FailSilent] returns \"\" when nothing is found")
        void failSilentEmpty() {
            var ctx = new MockContext(Map.of(), List.of());
            assertEquals("", evaluator.evalExpr("@DbLookup(\"\"; \"\"; \"V\"; \"k\"; 1; [FailSilent])", ctx));
        }
    }

    /** Mock FormulaContext that implements dbLookup/dbColumn with test data. */
    static class MockContext implements FormulaContext {
        private final Map<String, Object> fields;
        private final List<Object> lookupResult;
        List<Object> lastCall;

        MockContext(Map<String, Object> fields, List<Object> lookupResult) {
            this.fields = fields;
            this.lookupResult = lookupResult;
        }

        @Override public Object resolve(String name) { return fields.get(name); }

        @Override
        public List<Object> dbLookup(String s, String d, String v, Object k, int c) {
            lastCall = List.of(s, d, v, k, "column:" + c);
            return lookupResult;
        }

        @Override
        public List<Object> dbLookup(String s, String d, String v, Object k, String f) {
            lastCall = List.of(s, d, v, k, "field:" + f);
            return lookupResult;
        }

        @Override
        public List<Object> dbColumn(String s, String d, String v, int c) {
            lastCall = List.of(s, d, v, "column:" + c);
            return lookupResult;
        }
    }
}
