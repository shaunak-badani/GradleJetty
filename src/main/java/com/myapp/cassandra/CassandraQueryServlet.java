package com.myapp.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /cassandra/events?source=checkout-service
 *
 * Fetches every row for one partition (source), already sorted newest-first by the clustering
 * column. "source" is required on purpose: Cassandra can only serve this kind of query
 * efficiently by partition key - there's no general "filter by any field" the way SQL has.
 */
@WebServlet("/cassandra/events/search")
public class CassandraQueryServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PreparedStatement SELECT = CassandraSessionProvider.getSession().prepare(
            "SELECT source, created_at, message FROM " + CassandraConfig.TABLE + " WHERE source = ? LIMIT 50");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String source = request.getParameter("source");
        if (source == null || source.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Missing required query parameter 'source' (Cassandra needs the partition key to query)");
            return;
        }

        CqlSession session = CassandraSessionProvider.getSession();
        ResultSet rows = session.execute(SELECT.bind(source));

        List<EventRecord> results = new ArrayList<>();
        for (Row row : rows) {
            results.add(new EventRecord(row.getString("source"), row.getString("message"),
                    row.getInstant("created_at").toString()));
        }

        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(results));
    }
}
