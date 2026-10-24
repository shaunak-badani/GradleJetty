package com.myapp.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.io.IOException;

/**
 * POST /logs
 * Body: {"level": "INFO", "message": "something happened", "source": "checkout-service", "cluster": "local"}
 *
 * "cluster" picks where this entry ends up:
 *  - "local" (default) - writes to logs/application.log only. Logstash tails that file and ships
 *    new lines into the local "app-logs" index (see logstash/app-logs.conf). This mirrors a real
 *    deployment: the app only ever produces its log file, something else ingests it.
 *  - CrossClusterConfig.ALIAS (e.g. "cluster-b") - skips the file entirely and indexes the
 *    document straight into that cluster's own "app-logs" index. There's no Logstash shipping
 *    logs to cluster-b in this demo, and CCS itself has no write path (remote cluster aliases are
 *    search-only), so this is the only way to get a document there.
 */
@WebServlet("/logs")
public class LogIngestServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LogEntry incoming = MAPPER.readValue(request.getInputStream(), LogEntry.class);
        String targetCluster = incoming.getCluster();

        if (targetCluster == null || targetCluster.isBlank() || targetCluster.equals("local")) {
            LogEntry entry = new LogEntry(incoming.getLevel(), incoming.getMessage(), incoming.getSource());
            LogFileWriter.append(entry);
            entry.setCluster("local");
            respond(response, entry);
            return;
        }

        if (!targetCluster.equals(CrossClusterConfig.ALIAS)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Unknown cluster '" + targetCluster + "' - use 'local' or '" + CrossClusterConfig.ALIAS + "'");
            return;
        }

        LogEntry entry = new LogEntry(incoming.getLevel(), incoming.getMessage(), incoming.getSource());
        OpenSearchClient remoteClient = RemoteOpenSearchClientProvider.getClient();
        remoteClient.index(i -> i.index("app-logs").document(entry));
        entry.setCluster(targetCluster);
        respond(response, entry);
    }

    private void respond(HttpServletResponse response, LogEntry entry) throws IOException {
        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(entry));
    }
}
