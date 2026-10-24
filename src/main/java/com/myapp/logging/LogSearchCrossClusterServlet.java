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
 * GET /logs/search/ccs?q=checkout
 *
 * Same match query as LogSearchServlet, but the index pattern includes a remote cluster alias
 * ("cluster-b:app-logs") alongside the local one ("app-logs"). This isn't a different API - it's
 * the same _search call, just with an index name OpenSearch recognizes as "ask a remote cluster
 * for this". OpenSearchClientProvider's node must have the remote registered first
 * (cluster.remote.cluster-b.seeds via _cluster/settings) or every hit for that alias fails.
 */
@WebServlet("/logs/search/ccs")
public class LogSearchCrossClusterServlet extends HttpServlet {

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
                        .index("app-logs", CrossClusterConfig.ALIAS + ":app-logs")
                        .query(q -> q
                                .match(m -> m
                                        .field("message")
                                        .query(FieldValue.of(query)))),
                LogEntry.class);

        List<LogEntry> hits = searchResponse.hits().hits().stream()
                .map(LogSearchCrossClusterServlet::withClusterOrigin)
                .collect(Collectors.toList());

        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(hits));
    }

    /** hit.index() is "app-logs" for a local shard or "cluster-b:app-logs" for a remote one - that prefix is the whole CCS result. */
    private static LogEntry withClusterOrigin(Hit<LogEntry> hit) {
        LogEntry entry = hit.source();
        String index = hit.index();
        int colon = index.indexOf(':');
        entry.setCluster(colon == -1 ? "local" : index.substring(0, colon));
        return entry;
    }
}
