package com.domcouch.demo.config;

import com.domcouch.api.Database;
import com.domcouch.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration that establishes a Domino-style Session
 * connected to the Couchbase cluster.
 */
@Configuration
public class DomcouchConfig {

    private static final Logger log = LoggerFactory.getLogger(DomcouchConfig.class);

    @Value("${domcouch.connection-string:couchbase://localhost}")
    private String connectionString;

    @Value("${domcouch.username:Administrator}")
    private String username;

    @Value("${domcouch.password:password}")
    private String password;

    @Value("${domcouch.bucket:domcouch}")
    private String bucketName;

    @Value("${domcouch.database:contacts}")
    private String databaseName;

    @Bean
    public Session domcouchSession() throws Exception {
        log.info("Connecting to Couchbase at {}", connectionString);
        Session session = Session.createSession(connectionString, username, password);
        log.info("Connected as user '{}'", session.getUserName());
        return session;
    }

    @Bean
    @Primary
    public Database contactsDatabase(Session session) throws Exception {
        var db = session.getDatabase(bucketName, databaseName);
        log.info("Opened database '{}' (bucket '{}', scope '{}')",
                db.getTitle(), bucketName, databaseName);
        return db;
    }

    @Bean
    public Database kanbanDatabase(Session session) throws Exception {
        var db = session.getDatabase(bucketName, "kanban");
        log.info("Kanban database opened: {}.kanban", bucketName);
        return db;
    }
}
