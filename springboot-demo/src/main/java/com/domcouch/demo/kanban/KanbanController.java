package com.domcouch.demo.kanban;

import com.domcouch.api.NotesException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/kanban")
public class KanbanController {

    private final KanbanService service;

    public KanbanController(KanbanService service) { this.service = service; }

    // ---- Board ----

    @GetMapping
    public List<Map<String, String>> listBoards() throws NotesException {
        return service.getBoards().stream()
                .map(b -> Map.of("unid", b.getUnid(), "title", b.getTitle()))
                .toList();
    }

    @PostMapping
    public Map<String, String> createBoard(@RequestBody Map<String, String> body) throws NotesException {
        KanbanBoard board = service.createBoard(body.getOrDefault("title", "New Board"));
        return Map.of("unid", board.getUnid(), "title", board.getTitle());
    }

    @GetMapping("/{boardUnid}")
    public Map<String, Object> getBoard(@PathVariable String boardUnid) throws NotesException {
        return service.getBoardState(boardUnid);
    }

    @DeleteMapping("/{boardUnid}")
    public ResponseEntity<Void> deleteBoard(@PathVariable String boardUnid) throws NotesException {
        service.deleteBoard(boardUnid);
        return ResponseEntity.ok().build();
    }

    // ---- Task ----

    @PostMapping("/{boardUnid}/tasks")
    public Map<String, String> createTask(@PathVariable String boardUnid,
                                          @RequestBody Map<String, String> body) throws NotesException {
        KanbanTask task = service.addTask(
                body.get("laneUnid"), body.get("title"),
                body.getOrDefault("priority", "Medium"),
                body.getOrDefault("assignee", ""));
        return Map.of("unid", task.getUnid(), "title", task.getTitle());
    }

    @PutMapping("/tasks/{taskUnid}/move")
    public ResponseEntity<Void> moveTask(@PathVariable String taskUnid,
                                         @RequestBody Map<String, String> body) throws NotesException {
        service.moveTask(taskUnid, body.get("laneUnid"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tasks/{taskUnid}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskUnid) throws NotesException {
        service.deleteTask(taskUnid);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(NotesException.class)
    public ResponseEntity<Map<String, String>> handleError(NotesException e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
