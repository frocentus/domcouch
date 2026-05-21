package com.domcouch.demo;

import com.domcouch.api.*;
import com.domcouch.impl.CouchbaseRichTextItem;
import com.domcouch.impl.CouchbaseSession;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RichTextItem API.
 *
 * Requires: docker compose up -d
 * Run: mvn test -pl springboot-demo -Dtest=RichTextTest
 */
class RichTextTest {

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

    @Test @DisplayName("Create RichText item with text and style")
    void createRichTextItem() {
        var rt = new CouchbaseRichTextItem("Body");
        rt.appendText("Hello ");
        rt.getNotesFont("Courier", RichTextStyle.BOLD);
        rt.appendText("World");
        rt.addNewLine(1);
        rt.appendText("Second paragraph");

        assertEquals(3, rt.getSegmentCount());
        String plain = rt.getPlainText();
        assertTrue(plain.contains("Hello World"));
        assertTrue(plain.contains("Second paragraph"));
    }

    @Test @DisplayName("Sections")
    void sections() {
        var rt = new CouchbaseRichTextItem("Body");
        rt.beginSection("Introduction");
        rt.appendText("This is the intro.");
        rt.endSection();
        rt.appendText("After section.");

        assertEquals(4, rt.getSegmentCount()); // section + text + sectionend + text
        String plain = rt.getPlainText();
        assertTrue(plain.contains("[Introduction]"));
    }

    @Test @DisplayName("Doc links")
    void docLinks() throws Exception {
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "TestDoc");
        doc.save();

        var rt = new CouchbaseRichTextItem("Ref");
        rt.appendDocLink(doc, "See test document");
        rt.appendText(" - end");

        assertEquals(4, rt.getSegmentCount()); // doclink + text
        String plain = rt.getPlainText();
        assertTrue(plain.contains("See test document"));
        assertTrue(plain.contains("[→"));

        doc.remove();
    }

    @Test @DisplayName("Insert position")
    void insertPosition() {
        var rt = new CouchbaseRichTextItem("Body");
        rt.appendText("AAA");
        rt.appendText("CCC");

        // Insert between AAA and CCC
        rt.beginInsert();
        rt.appendText("BBB");
        rt.endInsert();

        // Back to appending
        rt.appendText("DDD");

        assertEquals(8, rt.getSegmentCount()); // AAA + BBB + CCC + DDD = 4 text + 4 newlines? No: each appendText is ONE segment
        // Actually: AAA(1), BBB(1 at pos 1), CCC(1), DDD(1) = 4 segments
        assertEquals(4, rt.getSegmentCount());
        String plain = rt.getPlainText();
        assertTrue(plain.contains("AAABBBCCCDDD"));
    }

    @Test @DisplayName("Save and reload RichText round-trip")
    void saveAndReloadRichText() throws Exception {
        Document doc = db.createDocument();
        doc.replaceItemValue("Form", "RTDoc");

        // Create rich text manually
        var rt = new CouchbaseRichTextItem("Body");
        rt.getNotesFont("Helvetica", RichTextStyle.BOLD | RichTextStyle.ITALIC);
        rt.appendText("Title text");
        rt.addNewLine(2);
        rt.appendText("Body text");

        // Store on document
        ((com.domcouch.impl.CouchbaseDocument) doc).replaceItemValue("Body", rt);
        doc.save();

        Thread.sleep(500);
        Document reloaded = db.getDocumentByUNID(doc.getUniversalID());
        if (reloaded != null) {
            Item body = reloaded.getFirstItem("Body");
            assertNotNull(body);
            assertTrue(body instanceof RichTextItem);
            RichTextItem rtReloaded = (RichTextItem) body;
            assertEquals(3, rtReloaded.getSegmentCount());
            String plain = rtReloaded.getPlainText();
            assertTrue(plain.contains("Title text"));
            assertTrue(plain.contains("Body text"));
        }

        doc.remove();
    }

    @Test @DisplayName("Style with() builder")
    void styleBuilder() {
        RichTextStyle base = RichTextStyle.create();
        RichTextStyle styled = base.with("font-name:Courier; size:14; bold; italic; color:FF0000");

        assertEquals("Courier", styled.getFontName());
        assertEquals(14, styled.getFontSize());
        assertTrue(styled.isBold());
        assertTrue(styled.isItalic());
        assertEquals("FF0000", styled.getColor());
    }

    @Test @DisplayName("Compact removes empty text segments")
    void compact() {
        var rt = new CouchbaseRichTextItem("Body");
        rt.appendText("Keep");
        rt.appendText(""); // empty — should be removed by compact
        rt.appendText("Also keep");

        assertEquals(3, rt.getSegmentCount());
        rt.compact();
        assertEquals(2, rt.getSegmentCount());
    }

    @Test @DisplayName("getMimeType and isMimeType")
    void mimeType() {
        var rt = new CouchbaseRichTextItem("Body");
        assertEquals("text/plain", rt.getMimeType());
        assertTrue(rt.isMimeType("text/plain"));
        assertFalse(rt.isMimeType("text/html"));
    }

    @Test @DisplayName("getValues returns plain text")
    void getValuesReturnsPlainText() {
        var rt = new CouchbaseRichTextItem("Body");
        rt.appendText("First");
        rt.addNewLine(1);
        rt.appendText("Second");

        assertEquals("First\nSecond", rt.getValueString());
        assertEquals("First\nSecond", rt.getValues().get(0));
    }
}
