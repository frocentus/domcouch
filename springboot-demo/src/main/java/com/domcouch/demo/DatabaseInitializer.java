package com.domcouch.demo;

import com.domcouch.demo.service.DominoDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, initializes the database with 10,000 fake persons and creates views.
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DominoDatabaseService service;

    public DatabaseInitializer(DominoDatabaseService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========================================");
        log.info("  DomCouch — Domino + Couchbase Demo");
        log.info("========================================");
        try {
            service.initialize();
            log.info("Database ready: {} documents", service.getDocumentCount());
        } catch (Exception e) {
            log.error("Failed to initialize database: {}", e.getMessage(), e);
            log.warn("Is Couchbase running? Run:  docker compose up -d");
        }
    }
}
