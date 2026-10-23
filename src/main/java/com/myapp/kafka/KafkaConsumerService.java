package com.myapp.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Runs on its own thread (started by KafkaLifecycleListener) and keeps the
 * last MAX_BUFFERED records consumed from KafkaConfig.TOPIC in memory for the
 * console page to display. This class never hears from KafkaProduceServlet
 * directly - both only ever talk to the broker, which is the thing being
 * demonstrated.
 */
public class KafkaConsumerService implements Runnable {

    private static final int MAX_BUFFERED = 50;
    private static final Deque<EventMessage> BUFFER = new ConcurrentLinkedDeque<>();

    private volatile boolean running = true;
    private KafkaConsumer<String, String> consumer;

    @Override
    public void run() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "financebudge-console");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        consumer = new KafkaConsumer<>(props);
        try {
            consumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC));
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    EventMessage message = new EventMessage(record.key(), record.value());
                    message.setPartition(record.partition());
                    message.setOffset(record.offset());
                    BUFFER.addFirst(message);
                    while (BUFFER.size() > MAX_BUFFERED) {
                        BUFFER.removeLast();
                    }
                }
            }
        } catch (WakeupException e) {
            // expected when shutdown() is called during contextDestroyed
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
    }

    public static List<EventMessage> recent() {
        return BUFFER.stream().collect(Collectors.toList());
    }
}
