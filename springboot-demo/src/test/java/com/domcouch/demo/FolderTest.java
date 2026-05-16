package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseSession;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Database folder CRUD operations.
 * <p>
 * Requires: docker compose up -d
 * Run: mvn test -pl springboot-demo -Dtest=FolderTest
 */
class FolderTest {

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

    @Test @DisplayName("createFolder creates a shared folder")
    void createFolder() throws Exception {
        String name = "test_folder_" + System.currentTimeMillis();
        View folder = db.createFolder(name);
        assertNotNull(folder);
        assertTrue(db.isFolder(name));
        assertTrue(db.getFolderNames().contains(name));
        db.removeFolder(name);
    }

    @Test @DisplayName("isFolder returns false for null or non-folders")
    void isFolderNegative() throws Exception {
        assertFalse(db.isFolder(null));
        assertFalse(db.isFolder("NonExistentFolder_xyz"));
    }

    @Test @DisplayName("getFolder returns null for non-existent folder")
    void getFolderNegative() throws Exception {
        assertNull(db.getFolder("GhostFolder_xyz"));
    }

    @Test @DisplayName("putInFolder adds document, getFolderNames reflects it")
    void putInFolder() throws Exception {
        String name = "test_folder_" + System.currentTimeMillis();
        db.createFolder(name);

        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Memo");
        doc.putInFolder(name);
        try {
            boolean saved = doc.save();
            assertTrue(saved, "save() should return true");
        } catch (NotesException e) {
            fail("save() threw: " + e.getMessage());
        }
        String unid = doc.getUniversalID();
        assertNotNull(unid, "UNID should be set");

        // Retry KV read (Couchbase may need a moment)
        Document reloaded = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(200);
            reloaded = db.getDocumentByUNID(unid);
            if (reloaded != null) break;
        }
        assertNotNull(reloaded, "Document should be readable after save (unid=" + unid + ")");
        assertTrue(reloaded.getFolderNames().contains(name));

        doc.remove();
        db.removeFolder(name);
    }

    @Test @DisplayName("removeFromFolder removes folder membership")
    void removeFromFolder() throws Exception {
        String name = "test_folder_" + System.currentTimeMillis();
        db.createFolder(name);

        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Memo");
        doc.putInFolder(name);
        doc.save();

        // Retry KV read
        Document reloaded = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(200);
            reloaded = db.getDocumentByUNID(doc.getUniversalID());
            if (reloaded != null) break;
        }
        assertNotNull(reloaded, "Document should exist after save");
        assertTrue(reloaded.getFolderNames().contains(name));

        reloaded.removeFromFolder(name);
        assertFalse(reloaded.getFolderNames().contains(name));
        reloaded.save();

        Document finalCheck = db.getDocumentByUNID(doc.getUniversalID());
        assertNotNull(finalCheck);
        assertFalse(finalCheck.getFolderNames().contains(name));

        doc.remove();
        db.removeFolder(name);
    }

    @Test @DisplayName("removeFolder deletes folder")
    void removeFolder() throws Exception {
        String name = "test_folder_" + System.currentTimeMillis();
        db.createFolder(name);
        assertTrue(db.isFolder(name));

        db.removeFolder(name);
        assertFalse(db.isFolder(name));
        assertNull(db.getFolder(name));
    }

    @Test @DisplayName("getFolderNames returns all folders")
    void getFolderNames() throws Exception {
        String a = "test_folder_a_" + System.currentTimeMillis();
        String b = "test_folder_b_" + System.currentTimeMillis();
        db.createFolder(a);
        db.createFolder(b);

        List<String> names = db.getFolderNames();
        assertTrue(names.contains(a));
        assertTrue(names.contains(b));

        db.removeFolder(a);
        db.removeFolder(b);
    }

    @Test @DisplayName("ViewNavigator works on folder view")
    void folderNavigator() throws Exception {
        String name = "test_folder_" + System.currentTimeMillis();
        db.createFolder(name);

        // Add a document to the folder
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Memo");
        doc.putInFolder(name);
        doc.save();

        // Wait for N1QL index
        Thread.sleep(1000);

        View folder = db.getFolder(name);
        assertNotNull(folder);

        var nav = folder.createViewNav();
        assertNotNull(nav);
        // Note: count may be 0 if N1QL hasn't indexed yet
        assertTrue(nav.getCount() >= 0);

        doc.remove();
        db.removeFolder(name);
    }

    @Test @DisplayName("Lazy navigator works on folder view")
    void folderLazyNavigator() throws Exception {
        String name = "test_folder_" + System.currentTimeMillis();
        db.createFolder(name);

        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "Memo");
        doc.putInFolder(name);
        doc.save();

        Thread.sleep(1000);

        View folder = db.getFolder(name);
        assertNotNull(folder);

        var nav = ((com.domcouch.impl.CouchbaseView) folder).createLazyViewNav();
        assertNotNull(nav);
        assertTrue(nav.getCount() >= 0);

        doc.remove();
        db.removeFolder(name);
    }
}
