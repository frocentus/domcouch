package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseSession;
import com.domcouch.impl.DocumentFormulaContext;
import com.domcouch.formula.translate.FormulaTranslator;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct @DbLookup and @DbColumn tests — verifying formula engine
 * lookup resolution WITHOUT going through computeWithForm.
 *
 * Requires: docker compose up -d
 * Run: mvn test -pl springboot-demo -Dtest=DbLookupDirectTest
 */
class DbLookupDirectTest {

    private static Session session;
    private static Database db;

    @BeforeAll
    static void setUp() throws NotesException {
        session = CouchbaseSession.connect("couchbase://localhost", "Administrator", "password");
        db = session.getDatabase("domcouch", "contacts");
    }

    @AfterAll
    static void tearDown() {
        if (session != null) session.recycle();
    }

    // ═══════════════════════════════════════════════════════════════
    // Nickname lookup table
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("getAllEntriesByKey direct — not through formula engine")
    void getAllEntriesByKeyDirect() throws Exception {
        setupNicknameLookupTable();
        View view = db.getView("Nicknames");
        var entries = view.getAllEntriesByKey("Bob");
        System.out.println("  getAllEntriesByKey('Bob'): " + entries.getCount() + " entries");
        assertEquals(1, entries.getCount(), "Should return exactly 1 entry for 'Bob', got " + entries.getCount());
        ViewEntry first = entries.getFirstEntry();
        assertNotNull(first);
        assertEquals("Robert", String.valueOf(first.getColumnValue(1)).trim());
        System.out.println("  direct: Bob → Robert ✅");
    }

    @Test @DisplayName("@DbLookup: nickname → full name (simple)")
    void nicknameLookup() throws Exception {
        setupNicknameLookupTable();

        // Evaluate @DbLookup directly
        Document doc = db.createDocument();
        doc.replaceItemValue("givenName", "Bob");
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        Object result = ft.evaluate("@DbLookup(\"\"; \"\"; \"Nicknames\"; givenName; 2)", ctx);

        assertNotNull(result);
        assertInstanceOf(List.class, result);
        List<?> list = (List<?>) result;
        assertFalse(list.isEmpty(), "Should find at least one match for Bob");
        long matchCount = list.stream().filter(v -> "Robert".equals(String.valueOf(v))).count();
        System.out.println("  @DbLookup Bob → " + list + " (" + matchCount + " matches)");
        assertTrue(matchCount >= 1, "Should find at least one Robert");
    }

    @Test @DisplayName("@DbLookup: unknown name returns empty list")
    void nicknameLookupUnknown() throws Exception {
        setupNicknameLookupTable();

        Document doc = db.createDocument();
        doc.replaceItemValue("givenName", "XYZ_Unknown");
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        Object result = ft.evaluate("@DbLookup(\"\"; \"\"; \"Nicknames\"; givenName; 2)", ctx);

        assertNotNull(result);
        assertInstanceOf(List.class, result);
        assertTrue(((List<?>) result).isEmpty(), "Unknown name should return empty list, got: " + result);

        System.out.println("  XYZ_Unknown → [] ✅");
    }

    @Test @DisplayName("@DbLookup: name with quotes (Bob's)")
    void nicknameLookupWithQuotes() throws Exception {
        setupNicknameLookupTable();

        // Add a quoted name
        Document d = db.createDocument();
        d.replaceItemValue("Form", "Nickname");
        d.replaceItemValue("givenName", "Bob's");
        d.replaceItemValue("fullName", "Robert Special");
        d.save();

        Thread.sleep(500);

        Document doc = db.createDocument();
        doc.replaceItemValue("givenName", "Bob's");
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        Object result = ft.evaluate("@DbLookup(\"\"; \"\"; \"Nicknames\"; givenName; 2)", ctx);

        assertNotNull(result);
        assertFalse(((List<?>) result).isEmpty());
        System.out.println("  Bob's → " + result + " ✅");
    }

    // ═══════════════════════════════════════════════════════════════
    // City-by-zip lookup table
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("@DbLookup: zip code → city name")
    void zipToCityLookup() throws Exception {
        setupZipLookupTable();

        Document doc = db.createDocument();
        doc.replaceItemValue("zipCode", "10001");
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        Object result = ft.evaluate("@DbLookup(\"\"; \"\"; \"ZipCities\"; zipCode; 2)", ctx);

        assertNotNull(result);
        assertFalse(((List<?>) result).isEmpty());
        assertEquals("New York", String.valueOf(((List<?>) result).get(0)));

        System.out.println("  10001 → " + result + " ✅");
    }

    @Test @DisplayName("@DbLookup: zip code with leading zeros")
    void zipToCityLeadingZeros() throws Exception {
        setupZipLookupTable();

        Document doc = db.createDocument();
        doc.replaceItemValue("zipCode", "01234");
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        Object result = ft.evaluate("@DbLookup(\"\"; \"\"; \"ZipCities\"; zipCode; 2)", ctx);

        assertNotNull(result);
        assertFalse(((List<?>) result).isEmpty());
        assertEquals("Smalltown", String.valueOf(((List<?>) result).get(0)));

        System.out.println("  01234 → " + result + " ✅");
    }

    @Test @DisplayName("@DbColumn: all cities")
    void dbColumnAllCities() throws Exception {
        setupZipLookupTable();

        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Dummy");
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        Object result = ft.evaluate("@DbColumn(\"\"; \"\"; \"ZipCities\"; 2)", ctx);

        assertNotNull(result);
        assertInstanceOf(List.class, result);
        assertTrue(((List<?>) result).size() >= 4, "Should have at least 4 cities");

        System.out.println("  All cities: " + result + " ✅");
    }

    @Test @DisplayName("@DbLookup: lookup by numeric key (salary tier)")
    void numericKeyLookup() throws Exception {
        setupSalaryTierTable();

        Document doc = db.createDocument();
        doc.replaceItemValue("salary", 75000.0);
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        Object result = ft.evaluate("@DbLookup(\"\"; \"\"; \"SalaryTiers\"; salary; 2)", ctx);

        assertNotNull(result);
        assertFalse(((List<?>) result).isEmpty(), "Salary 75000 should match at least one entry");
        System.out.println("  75000 → " + result + " ✅");
    }

    @Test @DisplayName("@DbLookup: multi-column result")
    void multiColumnLookup() throws Exception {
        setupZipLookupTable();

        Document doc = db.createDocument();
        doc.replaceItemValue("zipCode", "10001");
        var ctx = new DocumentFormulaContext(doc).withDatabase(db);
        var ft = new FormulaTranslator();

        // Column 1 = zipCode, column 2 = city, column 3 = state
        Object city = ft.evaluate("@DbLookup(\"\"; \"\"; \"ZipCitiesMulti\"; zipCode; 2)", ctx);
        Object state = ft.evaluate("@DbLookup(\"\"; \"\"; \"ZipCitiesMulti\"; zipCode; 3)", ctx);

        assertEquals("New York", String.valueOf(((List<?>) city).get(0)));
        assertEquals("NY", String.valueOf(((List<?>) state).get(0)));

        System.out.println("  10001 → " + city + " (" + state + ") ✅");
    }

    // ═══════════════════════════════════════════════════════════════
    // Table setup helpers
    // ═══════════════════════════════════════════════════════════════

    private boolean nicknamesSetup;

    private void setupNicknameLookupTable() throws Exception {
        if (nicknamesSetup) return;
        nicknamesSetup = true;
        // Clean up old test documents from previous runs
        cleanupByForm("Nickname");

        Object[][] data = {
            {"Bob", "Robert"}, {"Bill", "William"}, {"Liz", "Elizabeth"},
            {"Mike", "Michael"}, {"Jen", "Jennifer"}, {"Pat", "Patrick"},
        };
        for (Object[] n : data) {
            Document d = db.createDocument();
            d.replaceItemValue("Form", "Nickname");
            d.replaceItemValue("givenName", n[0]);
            d.replaceItemValue("fullName", n[1]);
            d.save();
        }
        db.createView("Nicknames", "Form = \"Nickname\"",
                "givenName",
                List.of(ViewColumn.field("givenName", "givenName"),
                        ViewColumn.field("fullName", "fullName")));

        Thread.sleep(1000);
        System.out.println("  Nickname lookup table: " + data.length + " entries");
    }

    private boolean zipSetup;

    private void setupZipLookupTable() throws Exception {
        if (zipSetup) return;
        zipSetup = true;
        cleanupByForm("ZipCity");

        Object[][] data = {
            {"10001", "New York", "NY"},
            {"01234", "Smalltown", "MA"},
            {"90210", "Beverly Hills", "CA"},
            {"60601", "Chicago", "IL"},
        };
        for (Object[] z : data) {
            Document d = db.createDocument();
            d.replaceItemValue("Form", "ZipCity");
            d.replaceItemValue("zipCode", z[0]);
            d.replaceItemValue("city", z[1]);
            d.replaceItemValue("state", z[2]);
            d.save();
        }
        db.createView("ZipCities", "Form = \"ZipCity\"",
                "zipCode",
                List.of(ViewColumn.field("zipCode", "zipCode"),
                        ViewColumn.field("city", "city")));

        db.createView("ZipCitiesMulti", "Form = \"ZipCity\"",
                "zipCode",
                List.of(ViewColumn.field("zipCode", "zipCode"),
                        ViewColumn.field("city", "city"),
                        ViewColumn.field("state", "state")));

        Thread.sleep(1000);
        System.out.println("  Zip lookup table: " + data.length + " entries");
    }

    private boolean salarySetup;

    private void cleanupByForm(String form) {
        try {
            var docs = db.search("Form = \"" + form + "\"");
            for (Document d : docs) d.remove();
            Thread.sleep(300);
        } catch (Exception ignored) {}
    }

    private void setupSalaryTierTable() throws Exception {
        if (salarySetup) return;
        salarySetup = true;
        cleanupByForm("SalaryTier");

        Object[][] data = {
            {25000.0, 1}, {50000.0, 2}, {75000.0, 3}, {100000.0, 4},
        };
        for (Object[] s : data) {
            Document d = db.createDocument();
            d.replaceItemValue("Form", "SalaryTier");
            d.replaceItemValue("salary", s[0]);
            d.replaceItemValue("tier", s[1]);
            d.save();
        }
        db.createView("SalaryTiers", "Form = \"SalaryTier\"",
                "salary",
                List.of(ViewColumn.field("salary", "salary"),
                        ViewColumn.field("tier", "tier")));

        Thread.sleep(1000);
        System.out.println("  Salary tier table: " + data.length + " entries");
    }
}
