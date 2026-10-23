package com.myapp.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * POST /kafka/produce
 * Body: {"key": "user-42", "value": "signed up"}
 *
 * Publishes one record to KafkaConfig.TOPIC and reports back the
 * partition/offset the broker assigned it. KafkaConsumerService, running on
 * its own thread, is what eventually picks this record up - this servlet has
 * no reference to it at all.
 */
@WebServlet("/kafka/produce")
public class KafkaProduceServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EventMessage incoming = MAPPER.readValue(request.getInputStream(), EventMessage.class);

        KafkaProducer<String, String> producer = KafkaProducerProvider.getProducer();
        ProducerRecord<String, String> record =
                new ProducerRecord<>(KafkaConfig.TOPIC, incoming.getKey(), incoming.getValue());

        try {
            RecordMetadata metadata = producer.send(record).get();
            EventMessage result = new EventMessage(incoming.getKey(), incoming.getValue());
            result.setPartition(metadata.partition());
            result.setOffset(metadata.offset());

            response.setContentType("application/json");
            response.getWriter().println(MAPPER.writeValueAsString(result));
        } catch (InterruptedException | ExecutionException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Failed to publish to Kafka: " + e.getMessage());
        }
    }
}
