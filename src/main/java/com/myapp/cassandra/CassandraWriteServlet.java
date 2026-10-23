package com.myapp.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

/**
 * POST /cassandra/events
 * Body: {"source": "checkout-service", "message": "order placed"}
 *
 * Inserts one row keyed by (source, created_at). "source" is the partition key - Cassandra uses
 * it to decide which node(s) own this row - and "created_at" is a clustering column, so every row
 * for the same source ends up sorted together on disk, newest first.
 */
@WebServlet("/cassandra/events")
public class CassandraWriteServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PreparedStatement INSERT = CassandraSessionProvider.getSession().prepare(
            "INSERT INTO " + CassandraConfig.TABLE + " (source, created_at, message) VALUES (?, ?, ?)");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EventRecord incoming = MAPPER.readValue(request.getInputStream(), EventRecord.class);

        Instant createdAt = Instant.now();
        CqlSession session = CassandraSessionProvider.getSession();
        session.execute(INSERT.bind(incoming.getSource(), createdAt, incoming.getMessage()));

        EventRecord result = new EventRecord(incoming.getSource(), incoming.getMessage(), createdAt.toString());
        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(result));
    }
}
