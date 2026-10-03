package com.myapp.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POST /logs
 * Body:  {"level": "INFO", "message": "something happened", "source": "checkout-service"}
 *
 * Does two things with every log entry, to show both halves of the pipeline:
 *   1. Appends it as a JSON line to logs/application.log (the "log file on disk").
 *   2. Indexes the same entry into OpenSearch, index "app-logs" (the "searchable copy").
 */
@WebServlet("/logs")
public class LogIngestServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LogEntry incoming = MAPPER.readValue(request.getInputStream(), LogEntry.class);
        LogEntry entry = new LogEntry(incoming.getLevel(), incoming.getMessage(), incoming.getSource());

        LogFileWriter.append(entry);

        OpenSearchClient client = OpenSearchClientProvider.getClient();
        IndexResponse indexResponse = client.index(b -> b
                .index("app-logs")
                .document(entry));

        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", indexResponse.id());
        result.put("result", indexResponse.result().jsonValue());

        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(result));
    }
}
