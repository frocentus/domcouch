package com.domcouch.demo.kanban;

import com.domcouch.api.*;
import java.util.*;

/**
 * A single lane on a Kanban board, backed by a domcouch Document (Form = "KanbanLane").
 */
public class KanbanLane {
    private final Document doc;

    public KanbanLane(Document doc) { this.doc = doc; }

    static KanbanLane create(Database db, KanbanBoard board, String title, int order) throws NotesException {
        Document d = db.createDocument();
        d.replaceItemValue("Form", "KanbanLane");
        d.replaceItemValue("Title", title);
        d.replaceItemValue("Order", (double) order);
        d.replaceItemValue("WIPLimit", 10);
        d.makeResponse(board.getDocument());
        d.save();
        System.out.println("KanbanLane.create: save() unid=" + d.getUniversalID() + " parent=" + (board != null ? board.getUnid() : "null"));
        return new KanbanLane(d);
    }

    public String getUnid() { return doc.getUniversalID(); }
    public String getTitle() { return itemStr("Title"); }
    public double getOrder() {
        Item item = doc.getFirstItem("Order");
        return item != null ? item.getValueDouble() : 0;
    }
    public Document getDocument() { return doc; }

    public List<KanbanTask> getTasks(Database db) throws NotesException {
        List<KanbanTask> tasks = new ArrayList<>();
        DocumentCollection responses = doc.getResponses();
        for (Document d : responses) {
            Item f = d.getFirstItem("Form");
            if (f != null && "KanbanTask".equals(f.getValueString())) {
                tasks.add(new KanbanTask(d));
            }
        }
        return tasks;
    }

    void delete() throws NotesException { doc.remove(); }

    private String itemStr(String name) {
        Item item = doc.getFirstItem(name);
        return item != null ? item.getValueString() : "";
    }
}
