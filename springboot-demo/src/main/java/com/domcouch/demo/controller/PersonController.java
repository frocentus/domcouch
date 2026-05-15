package com.domcouch.demo.controller;

import com.domcouch.api.NotesException;
import com.domcouch.demo.model.Person;
import com.domcouch.demo.service.DominoDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the Domino-backed person database.
 */
@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private static final Logger log = LoggerFactory.getLogger(PersonController.class);
    private final DominoDatabaseService service;

    public PersonController(DominoDatabaseService service) {
        this.service = service;
    }

    /**
     * Health check with database info.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "database", service.getDatabaseTitle(),
                "documentCount", service.getDocumentCount()
        ));
    }

    /**
     * Lookup by view.
     */
    @GetMapping("/view/{viewName}")
    public ResponseEntity<List<Person>> viewLookup(
            @PathVariable String viewName,
            @RequestParam(required = false) String key) throws NotesException {
        return ResponseEntity.ok(service.lookupByView(viewName, key));
    }

    /**
     * Full-text search.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Person>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "500") int maxDocs) throws NotesException {
        return ResponseEntity.ok(service.search(q, maxDocs));
    }

    /**
     * Get a single person by UNID.
     */
    @GetMapping("/{unid}")
    public ResponseEntity<Person> getByUnid(@PathVariable String unid) {
        Person p = service.getPersonByUNID(unid);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    /**
     * Re-initialize the database (clear + regenerate 10,000 persons).
     */
    @PostMapping("/admin/reinitialize")
    public ResponseEntity<Map<String, Object>> reinitialize() throws NotesException {
        long start = System.currentTimeMillis();
        service.reinitialize();
        long elapsed = System.currentTimeMillis() - start;
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Database reinitialized with fresh data",
                "documentCount", service.getDocumentCount(),
                "durationMs", elapsed
        ));
    }

    /**
     * Formula-column view: Income > 50K with computed FullName and Age.
     */
    @GetMapping("/view/income-over-50k")
    public ResponseEntity<List<Map<String, Object>>> incomeOver50K() {
        return ResponseEntity.ok(service.getIncomeOver50KView());
    }
}
