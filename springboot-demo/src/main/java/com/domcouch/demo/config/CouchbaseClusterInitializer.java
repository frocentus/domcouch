package com.domcouch.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Auto-configures an uninitialized Couchbase cluster on first startup.
 * Removes the need for manual cluster setup.
 */
@Component
public class CouchbaseClusterInitializer {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseClusterInitializer.class);

    @Value("${domcouch.connection-string:couchbase://localhost}")
    private String connectionString;

    @Value("${domcouch.username:Administrator}")
    private String username;

    @Value("${domcouch.password:password}")
    private String password;

    @Value("${domcouch.bucket:domcouch}")
    private String bucketName;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureClusterReady() {
        // Extract host from connection string (e.g., couchbase://localhost -> localhost)
        String host = connectionString.replace("couchbase://", "");
        int port = 8091;
        String restBase = "http://" + host + ":" + port;
        String auth = java.util.Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());

        try {
            // Check if cluster is already set up
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            var checkRequest = HttpRequest.newBuilder()
                    .uri(URI.create(restBase + "/pools/default"))
                    .header("Authorization", "Basic " + auth)
                    .GET()
                    .build();

            var checkResponse = client.send(checkRequest, HttpResponse.BodyHandlers.ofString());

            if (checkResponse.statusCode() == 200 && !checkResponse.body().contains("unknown pool")) {
                log.info("Couchbase cluster is already initialized.");
            } else {
                log.info("Couchbase cluster not initialized — setting up automatically...");

                // Step 1: Set data paths
                post(client, restBase + "/nodes/self/controller/settings",
                        "path=%2Fopt%2Fcouchbase%2Fvar%2Flib%2Fcouchbase%2Fdata&index_path=%2Fopt%2Fcouchbase%2Fvar%2Flib%2Fcouchbase%2Fdata", auth);

                // Step 2: Set up services
                post(client, restBase + "/node/controller/setupServices",
                        "services=kv%2Cn1ql%2Cindex%2Cfts", auth);

                // Step 3: Set admin credentials
                post(client, restBase + "/settings/web",
                        "username=" + username + "&password=" + password + "&port=8091", auth);

                // Step 4: Set memory quotas
                post(client, restBase + "/pools/default",
                        "memoryQuota=512&indexMemoryQuota=256&ftsMemoryQuota=256", auth);

                // Step 5: Create bucket
                post(client, restBase + "/pools/default/buckets",
                        "name=" + bucketName + "&ramQuotaMB=512&bucketType=couchbase&replicaNumber=0&flushEnabled=1", auth);

                log.info("Cluster initialized! Bucket '{}' created.", bucketName);
            }

            // Step 6: Always ensure GSI indexer storage mode is set (required for indexes)
            post(client, restBase + "/settings/indexes",
                    "storageMode=forestdb", auth);
            log.info("GSI indexer storage mode ensured (forestdb).");

        } catch (Exception e) {
            log.warn("Could not auto-initialize Couchbase cluster via REST API: {}", e.getMessage());
            log.warn("You may need to initialize via http://{}:{}/", host, port);
        }
    }

    private void post(HttpClient client, String url, String body, String auth) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + auth)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.debug("REST call to {} returned {}: {}", url, response.statusCode(), response.body());
        }
    }
}
