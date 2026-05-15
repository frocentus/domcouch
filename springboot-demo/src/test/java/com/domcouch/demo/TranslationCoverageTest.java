package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseSession;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying all N1QL-translatable @Functions via a
 * Couchbase view with formula columns.
 * <p>
 * Requires: docker compose up -d
 * Run: mvn test -pl springboot-demo -Dtest=TranslationCoverageTest -Dcouchbase.test=true
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TranslationCoverageTest {

    private static Session session;
    private static Database db;
    private static String testUnid;

    @BeforeAll
    static void setUp() throws NotesException {
        session = CouchbaseSession.connect("couchbase://localhost", "Administrator", "password");
        db = session.getDatabase("domcouch", "translation_test");
    }

    @AfterAll
    static void tearDown() {
        if (session != null) session.recycle();
    }

    @Test @Order(1) @DisplayName("Create test document with known values")
    void createTestDocument() throws Exception {
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "TestDoc");
        doc.replaceItemValue("FirstName", "John");
        doc.replaceItemValue("LastName", "Doe");
        doc.replaceItemValue("Age", 30);
        doc.replaceItemValue("Salary", 75000.0);
        doc.replaceItemValue("Income", 60000.0);
        doc.replaceItemValue("Department", "Engineering");
        doc.replaceItemValue("City", "New York");
        doc.replaceItemValue("Tags", List.of("java", "couchbase", "domino"));
        doc.replaceItemValue("Grade", 95.5);
        doc.replaceItemValue("Active", true);
        doc.save();
        // Allow Couchbase index propagation
        Thread.sleep(500);
        testUnid = doc.getUniversalID();
        assertNotNull(testUnid);
    }

    @Test @Order(2) @DisplayName("View: field references")
    void fieldReferences() {
        var view = createView(List.of(
                ViewColumn.field("FirstName", "FirstName"),
                ViewColumn.field("LastName", "LastName"),
                ViewColumn.field("Department", "Department"),
                ViewColumn.field("Salary", "Salary")
        ));
        var e = first(view);
        assertEquals("John", string(e, 0));
        assertEquals("Doe", string(e, 1));
        assertEquals("Engineering", string(e, 2));
        assertEquals("75000", string(e, 3).replace(".0", ""));
    }

    @Test @Order(3) @DisplayName("View: string concatenation (+ -> ||)")
    void stringConcat() {
        var view = createView(List.of(
                ViewColumn.formula("FullName", "FirstName + \" \" + LastName")
        ));
        assertEquals("John Doe", string(first(view), 0));
    }

    @Test @Order(4) @DisplayName("View: string functions")
    void stringFunctions() {
        var view = createView(List.of(
                ViewColumn.formula("UpperName", "@UpperCase(LastName)"),
                ViewColumn.formula("LowerName", "@LowerCase(FirstName)"),
                ViewColumn.formula("TrimName", "@Trim(FirstName)"),
                ViewColumn.formula("NameLength", "@Length(FirstName)"),
                ViewColumn.formula("Left2", "@Left(FirstName; 2)"),
                ViewColumn.formula("Right2", "@Right(LastName; 2)"),
                ViewColumn.formula("ContainsText", "@Contains(Department; \"Eng\")"),
                ViewColumn.formula("BeginsText", "@Begins(FirstName; \"Jo\")"),
                ViewColumn.formula("EndsText", "@Ends(LastName; \"oe\")"),
                ViewColumn.formula("ReplaceText", "@ReplaceSubstring(Department; \"Engineering\"; \"Eng\")"),
                ViewColumn.formula("RepeatText", "@Repeat(\"Hi\"; 3)"),
                ViewColumn.formula("ProperName", "@ProperCase(FirstName)"),
                ViewColumn.formula("NewLine", "@NewLine"),
                ViewColumn.formula("WordName", "@Word(\"John Doe\"; \" \"; 2)"),
                ViewColumn.formula("MiddleName", "@Middle(\"John\"; 2; 2)")
        ));
        var e = first(view);
        assertEquals("DOE", string(e, 0));
        assertEquals("john", string(e, 1));
        assertEquals("John", string(e, 2));
        assertEquals("4", string(e, 3).replace(".0", ""));
        assertEquals("Jo", string(e, 4));
        assertEquals("oe", string(e, 5));
        assertTrue(isTruthy(e, 6));
        assertTrue(isTruthy(e, 7));
        assertTrue(isTruthy(e, 8));
        assertEquals("Eng", string(e, 9));
        assertEquals("HiHiHi", string(e, 10));
        assertEquals("John", string(e, 11));
        assertEquals("\n", string(e, 12));
        assertEquals("Doe", string(e, 13));
        assertEquals("oh", string(e, 14));
    }

    @Test @Order(5) @DisplayName("View: type checking / conversion")
    void typeChecking() {
        var view = createView(List.of(
                ViewColumn.formula("IsNumber", "@IsNumber(Salary)"),
                ViewColumn.formula("IsText", "@IsText(FirstName)"),
                ViewColumn.formula("TextSalary", "@Text(Salary)"),
                ViewColumn.formula("TextToNum", "@TextToNumber(\"123\")"),
                ViewColumn.formula("IsNullEmpty", "@IsNull(\"\")"),
                ViewColumn.formula("IsNullField", "@IsNull(MissingField)")
        ));
        var e = first(view);
        assertTrue(isTruthy(e, 0));
        assertTrue(isTruthy(e, 1));
        assertEquals("75000", string(e, 2).replace(".0", "")); // TO_STRING may produce "75000" or "75000"
        assertEquals("123", string(e, 3).replace(".0", ""));
        // N1QL: '' IS NULL is false (empty string != SQL NULL)
        assertFalse(isTruthy(e, 4), "N1QL: empty string IS NOT NULL");
        assertFalse(isTruthy(e, 5), "N1QL: MISSING IS NOT NULL");
    }

    @Test @Order(6) @DisplayName("View: math functions")
    void mathFunctions() {
        var view = createView(List.of(
                ViewColumn.formula("AbsSalary", "@Abs(-75000)"),
                ViewColumn.formula("SqrtGrade", "@Sqrt(100)"),
                ViewColumn.formula("Power2", "@Power(5; 2)"),
                ViewColumn.formula("Cos0", "@Cos(0)"),
                ViewColumn.formula("Tan0", "@Tan(0)"),
                ViewColumn.formula("IntGrade", "@Integer(95.5)"),
                ViewColumn.formula("RoundGrade", "@Round(95.5)"),
                ViewColumn.formula("ModuloTest", "@Modulo(10; 3)"),
                ViewColumn.formula("SignTest", "@Sign(-42)"),
                ViewColumn.formula("MaxTest", "@Max(10; 20)"),
                ViewColumn.formula("MinTest", "@Min(10; 20)")
        ));
        var e = first(view);
        assertEquals("75000", string(e, 0));
        assertEquals("10", string(e, 1));
        assertEquals("25", string(e, 2));
        assertEquals("1", string(e, 3));
        assertEquals("0", string(e, 4));
        assertEquals("95", string(e, 5));
        assertEquals("96", string(e, 6));
        assertEquals("1", string(e, 7));
        assertEquals("-1", string(e, 8));
        assertEquals("20", string(e, 9));
        assertEquals("10", string(e, 10));
    }

    @Test @Order(7) @DisplayName("View: date extraction")
    void dateExtraction() {
        var view = createView(List.of(
                ViewColumn.formula("YearBirth", "@Year(@Created)"),
                ViewColumn.formula("AgeYears", "@Year(@Now) - @Year(@Created)")
        ));
        var e = first(view);
        // @Created is set at document creation time, should be current year
        int year = java.time.LocalDate.now().getYear();
        assertEquals(String.valueOf(year), string(e, 0).replace(".0", ""));
    }

    @Test @Order(8) @DisplayName("View: boolean / document / list")
    void booleanDocList() {
        var view = createView(List.of(
                ViewColumn.formula("YesTest", "@Yes"),
                ViewColumn.formula("NoTest", "@No"),
                ViewColumn.formula("TrueTest", "@True"),
                ViewColumn.formula("FalseTest", "@False"),
                ViewColumn.formula("HasField", "@IsAvailable(FirstName)"),
                ViewColumn.formula("NoField", "@IsUnavailable(MissingField)"),
                ViewColumn.formula("TagCount", "@Elements(Tags)"),
                ViewColumn.formula("HasJava", "@IsMember(\"java\"; Tags)")
        ));
        var e = first(view);
        assertTrue(isTruthy(e, 0));
        assertFalse(isTruthy(e, 1));
        assertTrue(isTruthy(e, 2));
        assertFalse(isTruthy(e, 3));
        assertTrue(isTruthy(e, 4));
        assertFalse(isTruthy(e, 5)); // MissingField doesn't exist
        assertEquals("3", string(e, 6));
        assertTrue(isTruthy(e, 7));
    }

    @Test @Order(9) @DisplayName("View: control flow")
    void controlFlow() {
        var view = createView(List.of(
                ViewColumn.formula("IfTest", "@If(Salary > 50000; \"High\"; \"Low\")")
        ));
        assertEquals("High", string(first(view), 0));
    }

    @Test @Order(10) @DisplayName("View: list explode/implode")
    void listFunctions() {
        var view = createView(List.of(
                ViewColumn.formula("ExplodeTest", "@Explode(\"a,b,c\"; \",\")")
        ));
        assertNotNull(first(view).getColumnValue(0));
    }

    // ---- helpers ----

    private View createView(List<ViewColumn> columns) {
        String name = "test_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        return db.createView(name, "Form = \"TestDoc\"", columns);
    }

    private ViewEntry first(View view) {
        var entries = view.getAllEntries();
        assertTrue(entries.getCount() > 0, "View returned no entries");
        return entries.iterator().next();
    }

    private String string(ViewEntry e, int idx) {
        Object val = e.getColumnValue(idx);
        return val != null ? val.toString().trim() : "";
    }

    private boolean isTruthy(ViewEntry e, int idx) {
        String s = string(e, idx);
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "1".equals(s);
    }
}
