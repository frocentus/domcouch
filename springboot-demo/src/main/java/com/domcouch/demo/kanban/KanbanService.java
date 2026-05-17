package com.domcouch.demo.kanban;

import com.domcouch.api.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class KanbanService {

    private final Database db;

    public KanbanService(@Qualifier("kanbanDatabase") Database db) { this.db = db; }

    // ---- Board ----

    public KanbanBoard createBoard(String title) throws NotesException {
        return KanbanBoard.create(db, title);
    }

    /** Create board with default lanes (avoids KV read-after-write issue). */
    public KanbanBoard createBoardWithLanes(String title) throws NotesException {
        KanbanBoard board = KanbanBoard.create(db, title);
        String[] lanes = {"Backlog", "Development", "Testing", "Deployment", "Finished"};
        for (int i = 0; i < lanes.length; i++) {
            KanbanLane.create(db, board, lanes[i], i);
        }
        return board;
    }

    public List<KanbanBoard> getBoards() throws NotesException {
        List<KanbanBoard> boards = new ArrayList<>();
        DocumentCollection results = db.search("Form = \"KanbanBoard\"");
        for (Document d : results) boards.add(new KanbanBoard(d));
        return boards;
    }

    public KanbanBoard getBoard(String unid) throws NotesException {
        Document doc = db.getDocumentByUNID(unid);
        return doc != null ? new KanbanBoard(doc) : null;
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

    public Map<String, Object> getBoardState(String boardUnid) throws NotesException {
        KanbanBoard board = getBoard(boardUnid);
        if (board == null) return Map.of();

        List<Map<String, Object>> lanes = new ArrayList<>();
        for (KanbanLane lane : board.getLanes(db)) {
            List<Map<String, Object>> tasks = new ArrayList<>();
            for (KanbanTask task : lane.getTasks(db)) {
                tasks.add(Map.of(
                    "unid", task.getUnid(), "title", task.getTitle(),
                    "priority", task.getPriority(), "assignee", task.getAssignee(),
                    "status", task.getStatus()
                ));
            }
            lanes.add(Map.of("unid", lane.getUnid(), "title", lane.getTitle(),
                    "order", lane.getOrder(), "tasks", tasks));
        }
        return Map.of("unid", board.getUnid(), "title", board.getTitle(), "lanes", lanes);
    }
}
