package com.myapp.kafka;

/**
 * Override with the KAFKA_BOOTSTRAP_SERVERS / KAFKA_TOPIC env vars.
 * Assumes a local, single-broker, no-auth Kafka (the usual dev setup) with
 * auto.create.topics.enable left on, so the topic doesn't need to exist
 * ahead of time.
 */
public class KafkaConfig {

    public static final String TOPIC = System.getenv().getOrDefault("KAFKA_TOPIC", "app-events");

    public static String bootstrapServers() {
        return System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    }
}
