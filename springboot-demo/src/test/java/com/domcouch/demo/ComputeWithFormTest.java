package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseRichTextStyle;
import com.domcouch.impl.CouchbaseSession;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Form-based computed fields (computeWithForm).
 *
 * Scenario: Person form with computed FullName, birthYear, and Age fields.
 *
 * Requires: docker compose up -d
 * Run: mvn test -pl springboot-demo -Dtest=ComputeWithFormTest
 */
class ComputeWithFormTest {

    private static Session session;
    private static Database db;
    private static Form personForm;

    @BeforeAll
    static void setUp() throws NotesException {
        session = CouchbaseSession.connect("couchbase://localhost", "Administrator", "password");
        db = session.getDatabase("domcouch", "contacts");

        // Create the Person form definition once
        personForm = db.createForm("Person", List.of(
            // Input fields (not computed)
            field("givenName",  Item.TEXT).build(),
            field("lastName",   Item.TEXT).build(),
            field("birthdate",  Item.TEXT).build(),

            // Computed fields
            field("FullName",   Item.TEXT)
                .computed(true)
                .formula("givenName + \" \" + lastName")
                .build(),

            field("birthYear",  Item.NUMBERS)
                .computed(true)
                .formula("@Year(birthdate)")
                .build(),

            field("Age",        Item.NUMBERS)
                .computed(true)
                .formula("@Year(@Now) - @Year(birthdate)")
                .build()
        ));

        System.out.println("Form 'Person' created with " + personForm.getFields().size() + " fields");
    }

    @AfterAll
    static void tearDown() {
        if (session != null) session.recycle();
    }

    // ═══════════════════════════════════════════════════════════════
    // Form Definition Helpers
    // ═══════════════════════════════════════════════════════════════

    private static FieldBuilder field(String name, int type) {
        return new FieldBuilder(name, type);
    }

    static class FieldBuilder {
        private final String name;
        private final int type;
        private boolean computed;
        private String formula;

        FieldBuilder(String name, int type) { this.name = name; this.type = type; }

        FieldBuilder computed(boolean v) { this.computed = v; return this; }
        FieldBuilder formula(String f) { this.formula = f; return this; }

        FormDef build() { return new FormDef(name, type, computed, formula); }
    }

    /** Minimal FieldDefinition impl for test. */
    record FormDef(String name, int type, boolean computed, String formula)
            implements Form.FieldDefinition {
        @Override public String getName() { return name; }
        @Override public int getType() { return type; }
        @Override public boolean isComputed() { return computed; }
        @Override public boolean isComputedWhenComposed() { return false; }
        @Override public boolean isComputedForDisplay() { return false; }
        @Override public String getFormula() { return formula; }
        @Override public String getDefaultFormula() { return null; }
        @Override public String getValidationFormula() { return null; }
        @Override public String getValidationMessage() { return null; }
        @Override public boolean isMultiValue() { return false; }
        @Override public boolean isRichText() { return false; }
        @Override public String getNumberFormat() { return null; }
        @Override public String getDateFormat() { return null; }
    }

    // ═══════════════════════════════════════════════════════════════
    // Test Cases
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("Create document and compute FullName + birthYear + Age")
    void computePersonFields() throws Exception {
        // 1. Create document with input fields
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Person");
        doc.replaceItemValue("givenName", "Alice");
        doc.replaceItemValue("lastName", "Smith");
        doc.replaceItemValue("birthdate", "1990-03-15");
        doc.save();

        System.out.println("Document created: " + doc.getUniversalID().substring(0, 8));

        // 2. Compute with form
        doc.computeWithForm(personForm, true, false);

        // 3. Verify computed fields
        Item fullName = doc.getFirstItem("FullName");
        assertNotNull(fullName, "FullName should be computed");
        assertEquals("Alice Smith", fullName.getValueString());
        System.out.println("  FullName: '" + fullName.getValueString() + "' ✅");

        Item birthYear = doc.getFirstItem("birthYear");
        assertNotNull(birthYear, "birthYear should be computed");
        assertEquals(1990, birthYear.getValueInt());
        System.out.println("  birthYear: " + birthYear.getValueInt() + " ✅");

        Item age = doc.getFirstItem("Age");
        assertNotNull(age, "Age should be computed");
        int expectedAge = LocalDate.now().getYear() - 1990;
        assertEquals(expectedAge, age.getValueInt());
        System.out.println("  Age: " + age.getValueInt() + " ✅");

        // 4. Save and reload to verify persistence
        doc.save();
        Thread.sleep(500);
        Document reloaded = db.getDocumentByUNID(doc.getUniversalID());
        if (reloaded != null) {
            assertEquals("Alice Smith", reloaded.getFirstItem("FullName").getValueString());
            assertEquals(expectedAge, reloaded.getFirstItem("Age").getValueInt());
            System.out.println("  Reload persistence ✅");
        }

        doc.remove();
    }

    @Test @DisplayName("Second person with different birth year")
    void secondPerson() throws Exception {
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Person");
        doc.replaceItemValue("givenName", "Bob");
        doc.replaceItemValue("lastName", "Jones");
        doc.replaceItemValue("birthdate", "1985-11-20");
        doc.save();

        doc.computeWithForm(personForm, true, false);

        assertEquals("Bob Jones", doc.getFirstItem("FullName").getValueString());
        assertEquals(1985, doc.getFirstItem("birthYear").getValueInt());
        assertEquals(LocalDate.now().getYear() - 1985, doc.getFirstItem("Age").getValueInt());

        System.out.println("  Bob Jones: FullName='Bob Jones', birthYear=1985, Age=" + (LocalDate.now().getYear() - 1985) + " ✅");
        doc.remove();
    }

    @Test @DisplayName("Missing input field → formula evaluates to empty")
    void missingInputField() throws Exception {
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Person");
        doc.replaceItemValue("givenName", "Charlie");
        // No lastName — formula should still work (empty string)
        doc.save();

        doc.computeWithForm(personForm, true, false);

        Item fullName = doc.getFirstItem("FullName");
        assertNotNull(fullName);
        // Domino: missing field = "" → "Charlie "
        assertTrue(fullName.getValueString().contains("Charlie"));
        System.out.println("  Missing lastName: '" + fullName.getValueString() + "' ✅");
        doc.remove();
    }
}
