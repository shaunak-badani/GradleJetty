package com.myapp.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /kafka/messages
 *
 * Returns what the background consumer has read off KafkaConfig.TOPIC so
 * far (most recent first). Polled by the console page to show the consumer
 * side of the flow.
 */
@WebServlet("/kafka/messages")
public class KafkaMessagesServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.getWriter().println(MAPPER.writeValueAsString(KafkaConsumerService.recent()));
    }
}
