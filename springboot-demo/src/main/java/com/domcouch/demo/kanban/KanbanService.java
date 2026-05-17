package com.domcouch.demo.kanban;

import com.domcouch.api.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service layer for Kanban board operations using domcouch Database API.
 */
@Service
public class KanbanService {

    private final Database db;

    public KanbanService(Database db) { this.db = db; }

    // ---- Board ----

    public KanbanBoard createBoard(String title) throws NotesException {
        return KanbanBoard.create(db, title);
    }

    public List<KanbanBoard> getBoards() throws NotesException {
        List<KanbanBoard> boards = new ArrayList<>();
        // db.search() uses N1QL IDs + KV fetch, avoids scanning all docs
        DocumentCollection results = db.search("Form = \"KanbanBoard\"");
        for (Document d : results) {
            boards.add(new KanbanBoard(d));
        }
        return boards;
    }

    public KanbanBoard getBoard(String unid) throws NotesException {
        // Retry KV read — Couchbase may need a moment after write
        for (int attempt = 0; attempt < 5; attempt++) {
            Document doc = db.getDocumentByUNID(unid);
            if (doc != null) return new KanbanBoard(doc);
            if (attempt < 4) try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        // KV failed — fallback to N1QL scan
        for (KanbanBoard b : getBoards()) {
            if (unid.equals(b.getUnid())) return b;
        }
        return null;
    }

    public void deleteBoard(String unid) throws NotesException {
        KanbanBoard b = getBoard(unid);
        if (b == null) return;
        for (KanbanLane lane : b.getLanes(db)) {
            for (KanbanTask task : lane.getTasks(db)) task.delete();
            lane.delete();
        }
        b.delete();
    }

    // ---- Lane ----

    public KanbanLane addLane(String boardUnid, String title, int order) throws NotesException {
        KanbanBoard board = getBoard(boardUnid);
        if (board == null) throw new NotesException(4000, "Board not found: " + boardUnid);
        return KanbanLane.create(db, board, title, order);
    }

    // ---- Task ----

    public KanbanTask addTask(String laneUnid, String title, String priority, String assignee) throws NotesException {
        Document laneDoc = db.getDocumentByUNID(laneUnid);
        if (laneDoc == null) throw new NotesException(4000, "Lane not found: " + laneUnid);
        return KanbanTask.create(db, new KanbanLane(laneDoc), title, priority, assignee);
    }

    public void moveTask(String taskUnid, String targetLaneUnid) throws NotesException {
        Document taskDoc = db.getDocumentByUNID(taskUnid);
        Document laneDoc = db.getDocumentByUNID(targetLaneUnid);
        if (taskDoc == null || laneDoc == null) throw new NotesException(4000, "Not found");
        new KanbanTask(taskDoc).moveTo(new KanbanLane(laneDoc));
    }

    public void deleteTask(String taskUnid) throws NotesException {
        Document doc = db.getDocumentByUNID(taskUnid);
        if (doc != null) new KanbanTask(doc).delete();
    }

    /** Get all lanes with tasks for a board as a flat list. */
    public Map<String, Object> getBoardState(String boardUnid) throws NotesException {
        KanbanBoard board = getBoard(boardUnid);
        if (board == null) return Map.of();

        List<Map<String, Object>> lanes = new ArrayList<>();
        for (KanbanLane lane : board.getLanes(db)) {
            List<Map<String, Object>> tasks = new ArrayList<>();
            for (KanbanTask task : lane.getTasks(db)) {
                tasks.add(Map.of(
                    "unid", task.getUnid(),
                    "title", task.getTitle(),
                    "priority", task.getPriority(),
                    "assignee", task.getAssignee(),
                    "status", task.getStatus()
                ));
            }
            lanes.add(Map.of(
                "unid", lane.getUnid(),
                "title", lane.getTitle(),
                "order", lane.getOrder(),
                "tasks", tasks
            ));
        }
        return Map.of("unid", board.getUnid(), "title", board.getTitle(), "lanes", lanes);
    }
}
