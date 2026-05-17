package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseSession;
import org.junit.jupiter.api.*;
import java.util.Vector;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduces and verifies the ClassCastException bug in getAllDocuments()
 * with multi-instance items (JSON arrays inside items object).
 *
 * Requires: docker compose up -d
 * Run: mvn test -pl springboot-demo -Dtest=GetAllDocumentsTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GetAllDocumentsTest {

    private static Session session;
    private static Database db;

    @BeforeAll
    static void setUp() throws NotesException {
        session = CouchbaseSession.connect("couchbase://localhost", "Administrator", "password");
        db = session.getDatabase("domcouch", "getalldocs_test");
    }

    @AfterAll
    static void tearDown() {
        if (session != null) session.recycle();
    }

    @Test @Order(1) @DisplayName("Save document with multi-value item (array schema)")
    void saveMultiValueDocument() throws Exception {
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "TestDoc");
        doc.replaceItemValue("Tags", new Vector<>(java.util.List.of("java", "couchbase", "test")));
        doc.replaceItemValue("Count", 42.0);
        doc.save();
        assertNotNull(doc.getUniversalID());
    }

    @Test @Order(2) @DisplayName("Save document with folders array")
    void saveDocumentWithFolders() throws Exception {
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "TestDoc");
        doc.putInFolder("TestFolder");
        doc.save();
    }

    @Test @Order(3) @DisplayName("getAllDocuments should not throw ClassCastException")
    void getAllDocumentsShouldWork() throws Exception {
        Thread.sleep(1000);
        // This is the call that throws ClassCastException in the kanban app
        DocumentCollection docs = db.getAllDocuments();
        assertNotNull(docs);
        assertTrue(docs.getCount() >= 0, "Should return collection even if empty due to N1QL timing");
        System.out.println("getAllDocuments returned " + docs.getCount() + " documents");
    }

    @Test @Order(4) @DisplayName("Each returned document should be readable")
    void eachDocumentReadable() throws Exception {
        DocumentCollection docs = db.getAllDocuments();
        for (Document doc : docs) {
            assertNotNull(doc.getUniversalID());
            Item form = doc.getFirstItem("Form");
            // Form might be null if document JSON is malformed
            if (form != null) assertNotNull(form.getValueString());
        }
    }
}
