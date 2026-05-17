package com.domcouch.demo.kanban;

import com.domcouch.api.*;
import java.time.Instant;
import java.util.*;

/**
 * Kanban board backed by a domcouch Document (Form = "KanbanBoard").
 */
public class KanbanBoard {
    private final Document doc;

    public KanbanBoard(Document doc) { this.doc = doc; }

    static KanbanBoard create(Database db, String title) throws NotesException {
        Document d = db.createDocument();
        d.replaceItemValue("Form", "KanbanBoard");
        d.replaceItemValue("Title", title);
        d.replaceItemValue("Created", Instant.now().toString());
        d.save();
        return new KanbanBoard(d);
    }

    public String getUnid() { return doc.getUniversalID(); }
    public String getTitle() {
        String t = itemStr("Title");
        System.out.println("KanbanBoard.getTitle: '" + t + "' items=" + doc.getItems().size());
        for (Item it : doc.getItems()) {
            System.out.println("  item: " + ((com.domcouch.impl.CouchbaseItem)it).getName() + "=" + it.getValueString());
        }
        return t;
    }
    public String getCreated() { return itemStr("Created"); }
    public Document getDocument() { return doc; }

    public List<KanbanLane> getLanes(Database db) throws NotesException {
        List<KanbanLane> lanes = new ArrayList<>();
        DocumentCollection responses = doc.getResponses();
        for (Document d : responses) {
            Item f = d.getFirstItem("Form");
            if (f != null && "KanbanLane".equals(f.getValueString())) {
                lanes.add(new KanbanLane(d));
            }
        }
        lanes.sort(Comparator.comparingDouble(KanbanLane::getOrder));
        return lanes;
    }

    void delete() throws NotesException { doc.remove(); }

    private String itemStr(String name) {
        Item item = doc.getFirstItem(name);
        return item != null ? item.getValueString() : "";
    }
}
