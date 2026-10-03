package com.myapp.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /logs/search?q=checkout
 *
 * Runs a "match" query against the "message" field of the "app-logs" index
 * and returns the matching log entries as JSON. This is the query half of
 * the demo - everything else exists to put documents here for this to find.
 */
@WebServlet("/logs/search")
public class LogSearchServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = request.getParameter("q");
        if (query == null || query.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Missing required query parameter 'q'");
            return;
        }

        OpenSearchClient client = OpenSearchClientProvider.getClient();
        SearchResponse<LogEntry> searchResponse = client.search(s -> s
                        .index("app-logs")
                        .query(q -> q
                                .match(m -> m
                                        .field("message")
                                        .query(FieldValue.of(query)))),
                LogEntry.class);

        List<LogEntry> hits = searchResponse.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(hits));
    }
}
