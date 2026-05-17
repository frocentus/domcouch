package com.domcouch.demo.kanban;

import com.domcouch.api.*;
import java.time.Instant;

/**
 * A task card on a Kanban lane, backed by a domcouch Document (Form = "KanbanTask").
 */
public class KanbanTask {
    private final Document doc;

    public KanbanTask(Document doc) { this.doc = doc; }

    static KanbanTask create(Database db, KanbanLane lane, String title, String priority, String assignee) throws NotesException {
        Document d = db.createDocument();
        d.replaceItemValue("Form", "KanbanTask");
        d.replaceItemValue("Title", title);
        d.replaceItemValue("Priority", priority);
        d.replaceItemValue("Assignee", assignee);
        d.replaceItemValue("Status", "Open");
        d.replaceItemValue("Created", Instant.now().toString());
        d.makeResponse(lane.getDocument());
        d.save();
        return new KanbanTask(d);
    }

    public String getUnid() { return doc.getUniversalID(); }
    public String getTitle() { return itemStr("Title"); }
    public String getPriority() { return itemStr("Priority"); }
    public String getAssignee() { return itemStr("Assignee"); }
    public String getStatus() { return itemStr("Status"); }
    public Document getDocument() { return doc; }

    /** Move this task to a different lane. */
    public void moveTo(KanbanLane targetLane) throws NotesException {
        doc.makeResponse(targetLane.getDocument());
        doc.save();
    }

    void delete() throws NotesException { doc.remove(); }

    private String itemStr(String name) {
        Item item = doc.getFirstItem(name);
        return item != null ? item.getValueString() : "";
    }
}
