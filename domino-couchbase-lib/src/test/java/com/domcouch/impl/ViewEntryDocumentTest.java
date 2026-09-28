package com.domcouch.impl;

import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.domcouch.api.Database;
import com.domcouch.api.Document;
import com.domcouch.api.Item;
import com.domcouch.api.View;
import com.domcouch.api.ViewEntry;
import com.domcouch.api.ViewEntryCollection;
import com.domcouch.formula.translate.FormulaTranslator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ViewEntry.getDocument() must return the complete document, as lotus.domino does,
 * even when the view query selected only unid + column aliases. No Couchbase needed.
 */
class ViewEntryDocumentTest {

    private static JsonObject itemsOf(String name, String value) {
        return JsonObject.create().put(name.toUpperCase(), JsonArray.from(
                JsonObject.create().put("type", 0).put("values", JsonArray.from(value))));
    }

    @Test
    @DisplayName("explicit-column row (no items) → document is loaded by UNID")
    void partialRowLoadsFullDocument() {
        CouchbaseDatabase db = mock(CouchbaseDatabase.class);
        CouchbaseView view = mock(CouchbaseView.class);
        when(view.getDatabase()).thenReturn(db);
        Document full = mock(Document.class);
        when(db.getDocumentByUNID("A1")).thenReturn(full);

        JsonObject row = JsonObject.create().put("unid", "A1").put("Kurzbez", "SPÖ");
        ViewEntry entry = new CouchbaseViewEntry(view, "A1", List.of("SPÖ"), 1, row);

        assertSame(full, entry.getDocument());
        verify(db).getDocumentByUNID("A1");
    }

    @Test
    @DisplayName("doc.* row (with items) → document built from the row, no extra read")
    void fullRowIsReused() {
        CouchbaseDatabase db = mock(CouchbaseDatabase.class);
        CouchbaseView view = mock(CouchbaseView.class);
        when(view.getDatabase()).thenReturn(db);

        JsonObject row = JsonObject.create().put("unid", "A1")
                .put("items", itemsOf("Partei_Name", "Sozialdemokratische Partei"));
        ViewEntry entry = new CouchbaseViewEntry(view, "A1", List.of(), 1, row);

        Document doc = entry.getDocument();
        assertEquals("Sozialdemokratische Partei", doc.getFirstItem("Partei_Name").getValues().get(0));
        verify(db, never()).getDocumentByUNID(anyString());
    }

    @Test
    @DisplayName("@DbLookup with a field name reads the item from each matching document")
    void dbLookupByFieldName() {
        Item item = mock(Item.class);
        when(item.getValues()).thenReturn(new Vector<>(List.of("Sozialdemokratische Partei")));
        Document match = mock(Document.class);
        when(match.getFirstItem("Partei_Name")).thenReturn(item);
        ViewEntry entry = mock(ViewEntry.class);
        when(entry.getDocument()).thenReturn(match);
        ViewEntryCollection entries = mock(ViewEntryCollection.class);
        when(entries.iterator()).thenAnswer(inv -> List.of(entry).iterator());
        View view = mock(View.class);
        when(view.getAllEntriesByKey("P42")).thenReturn(entries);
        Database db = mock(Database.class);
        when(db.getView("Parteien_ID")).thenReturn(view);

        var ctx = new DocumentFormulaContext(mock(Document.class)).withDatabase(db);
        Object result = new FormulaTranslator().evaluate(
                "@DbLookup(\"Notes\":\"ReCache\"; \"\"; \"Parteien_ID\"; \"P42\"; \"Partei_Name\"; [FailSilent])", ctx);

        assertEquals(List.of("Sozialdemokratische Partei"), result);
    }
}
