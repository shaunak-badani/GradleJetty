package com.myapp.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * POST /logs
 * Body:  {"level": "INFO", "message": "something happened", "source": "checkout-service"}
 *
 * Only writes to logs/application.log. Getting entries into OpenSearch is no longer
 * this servlet's job - Logstash tails that file and ships new lines into the
 * "app-logs" index (see logstash/app-logs.conf). This mirrors a real deployment:
 * the app only ever produces its log file, something else ingests it.
 */
@WebServlet("/logs")
public class LogIngestServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LogEntry incoming = MAPPER.readValue(request.getInputStream(), LogEntry.class);
        LogEntry entry = new LogEntry(incoming.getLevel(), incoming.getMessage(), incoming.getSource());

        LogFileWriter.append(entry);

        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(entry));
    }
}
