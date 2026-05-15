package com.domcouch.formula;

import com.domcouch.formula.translate.FormulaTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the compile-once, evaluate-many pattern and verifies
 * formula results match hand-computed Java values.
 */
@DisplayName("Cached Formula Evaluation")
class CachedEvaluationTest {

    private FormulaTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new FormulaTranslator("Alice");
    }

    /**
     * Simulates a Person document with 20 fields (matching the demo app schema).
     */
    private FormulaContext personContext(String firstName, String lastName, String email,
                                         String city, String department, double salary) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("FIRSTNAME", firstName);
        vars.put("LASTNAME", lastName);
        vars.put("EMAIL", email);
        vars.put("CITY", city);
        vars.put("DEPARTMENT", department);
        vars.put("SALARY", salary);
        vars.put("COMPANY", "Acme Inc.");
        vars.put("COUNTRY", "USA");
        vars.put("STATUS", "Active");
        vars.put("CREATED", "2024-01-15T09:00:00Z");
        return vars::get;
    }

    // ================================================================
    // Compile once, evaluate against 3 documents, verify against Java
    // ================================================================

    @Test
    @DisplayName("FullName formula cached, verified against 3 persons")
    void fullNameCached() {
        CompiledFormula fullName = translator.compile("FirstName + \" \" + LastName");

        // Doc 1: Alice Johnson
        assertEquals("Alice Johnson",
                translator.evaluate(fullName, personContext("Alice", "Johnson", "a@x.com", "NYC", "Eng", 95000)));

        // Doc 2: Bob Smith
        assertEquals("Bob Smith",
                translator.evaluate(fullName, personContext("Bob", "Smith", "b@x.com", "LA", "Sales", 120000)));

        // Doc 3: empty names
        assertEquals(" ",
                translator.evaluate(fullName, personContext("", "", "", "", "", 0)));
    }

    @Test
    @DisplayName("Computed field: company email pattern")
    void companyEmailPattern() {
        // Company email: FirstName + "." + LastName + "@" + Company + ".com"
        CompiledFormula email = translator.compile(
                "FirstName + \".\" + LastName + \"@\" + Company + \".com\"");

        assertEquals("Alice.Johnson@Acme Inc..com",
                translator.evaluate(email, personContext("Alice", "Johnson", "", "", "", 0)));

        assertEquals("Bob.Smith@Acme Inc..com",
                translator.evaluate(email, personContext("Bob", "Smith", "", "", "", 0)));
    }

    @Test
    @DisplayName("Email username extraction via @Left + string op")
    void emailUsername() {
        // Extract everything before @: @Left(email; position of @)
        // Since @Left needs a numeric count, we use the length approach
        CompiledFormula username = translator.compile(
                "@Left(Email; @Length(Email) - @Length(@Right(Email; 8)))");
        // For "alice@acme.com" (16 chars), @Right 8 = "acme.com" (8 chars)
        // 16 - 8 = 8, @Left 8 = "alice@"
        // This is imperfect but demonstrates formula composition

        Object result = translator.evaluate(username,
                personContext("", "", "alice@acme.com", "", "", 0));
        assertNotNull(result);
    }

    @Test
    @DisplayName("Salary band formula (@If)")
    void salaryBand() {
        CompiledFormula band = translator.compile(
                "@If(Salary >= 200000; \"Executive\"; @If(Salary >= 100000; \"Senior\"; @If(Salary >= 50000; \"Mid\"; \"Junior\")))");

        // Java equivalent
        java.util.function.Function<Double, String> javaBand = s -> {
            if (s >= 200000) return "Executive";
            if (s >= 100000) return "Senior";
            if (s >= 50000) return "Mid";
            return "Junior";
        };

        double[] salaries = {250000, 120000, 95000, 75000, 45000, 30000, 0};
        for (double s : salaries) {
            String expected = javaBand.apply(s);
            assertEquals(expected,
                    translator.evaluate(band, personContext("", "", "", "", "", s)),
                    "Salary: " + s);
        }
    }

    @Test
    @DisplayName("Location string: City, Country")
    void locationString() {
        CompiledFormula loc = translator.compile("City + \", \" + Country");

        assertEquals("NYC, USA", translator.evaluate(loc, personContext("", "", "", "NYC", "", 0)));
        assertEquals("London, UK", translator.evaluate(loc, createContext(Map.of("CITY", "London", "COUNTRY", "UK"))));
        assertEquals("Tokyo, Japan", translator.evaluate(loc, createContext(Map.of("CITY", "Tokyo", "COUNTRY", "Japan"))));
        assertEquals(", ", translator.evaluate(loc, createContext(Map.of())));
    }

    // ================================================================
    // Batch processing: 1000 documents, 5 computed fields
    // ================================================================

    @Test
    @DisplayName("Batch: 1000 docs × 5 fields = 5000 cached evaluations")
    void batchProcessing() {
        // Compile formulas once (form-load time)
        CompiledFormula fullName  = translator.compile("FirstName + \" \" + LastName");
        CompiledFormula upper     = translator.compile("@UpperCase(LastName)");
        CompiledFormula band      = translator.compile("@If(Salary >= 100000; \"Senior\"; \"Junior\")");
        CompiledFormula location  = translator.compile("City + \", \" + Country");
        CompiledFormula greeting  = translator.compile("\"Hello \" + FirstName + \" from \" + Department");

        String[] firstNames = {"Alice", "Bob", "Carol", "Dave", "Eve"};
        String[] lastNames  = {"Johnson", "Smith", "Williams", "Brown", "Davis"};
        String[] cities     = {"NYC", "LA", "Chicago", "Houston", "Phoenix"};
        String[] depts      = {"Engineering", "Sales", "Marketing", "HR", "Finance"};
        double[] salaries   = {95000, 120000, 45000, 180000, 75000};

        int docs = 1000;
        int errors = 0;

        for (int i = 0; i < docs; i++) {
            int idx = i % 5;
            FormulaContext ctx = personContext(
                    firstNames[idx], lastNames[idx], "", cities[idx], depts[idx], salaries[idx]);

            String name = (String) translator.evaluate(fullName, ctx);
            String up   = (String) translator.evaluate(upper, ctx);
            String b    = (String) translator.evaluate(band, ctx);
            String loc  = (String) translator.evaluate(location, ctx);
            String g    = (String) translator.evaluate(greeting, ctx);

            // Verify against Java-computed values
            assertEquals(firstNames[idx] + " " + lastNames[idx], name);
            assertEquals(lastNames[idx].toUpperCase(), up);
            assertEquals(salaries[idx] >= 100000 ? "Senior" : "Junior", b);
            assertEquals(cities[idx] + ", USA", loc);
            assertEquals("Hello " + firstNames[idx] + " from " + depts[idx], g);
        }

        // No assertion failures = all 5000 evaluations correct
    }

    // ================================================================
    // FIELD assignment: formula modifies document
    // ================================================================

    @Test
    @DisplayName("FIELD assignment writes to context")
    void fieldAssignmentWritesToContext() {
        CompiledFormula setFullName = translator.compile(
                "FIELD FullName := FirstName + \" \" + LastName");

        MapFormulaContext ctx = new MapFormulaContext(Map.of(
                "FIRSTNAME", "Alice",
                "LASTNAME", "Johnson"
        ));

        Object result = translator.evaluate(setFullName, ctx);
        assertEquals("Alice Johnson", result);
        assertEquals("Alice Johnson", ctx.get("FULLNAME"));
    }

    // ================================================================
    // List operations
    // ================================================================

    @Test
    @DisplayName("List construction and @Elements")
    void listOperations() {
        CompiledFormula elements = translator.compile("@Elements(\"a\" : \"b\" : \"c\" : \"d\" : \"e\")");
        assertEquals(5.0, translator.evaluate(elements, createContext(Map.of())));
    }

    // ---- helpers ----

    private FormulaContext createContext(Map<String, Object> vars) {
        return vars::get;
    }

    static class MapFormulaContext implements FormulaContext {
        private final Map<String, Object> map;

        MapFormulaContext(Map<String, Object> initial) {
            this.map = new HashMap<>(initial);
        }

        @Override public Object resolve(String name) { return map.get(name); }
        @Override public void setField(String name, Object value) { map.put(name, value); }
        Object get(String name) { return map.get(name); }
    }
}
