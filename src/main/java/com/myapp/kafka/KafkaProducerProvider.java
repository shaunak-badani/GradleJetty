package com.myapp.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/** One shared KafkaProducer, same sharing pattern as OpenSearchClientProvider. */
public class KafkaProducerProvider {

    private static final KafkaProducer<String, String> PRODUCER = buildProducer();

    public static KafkaProducer<String, String> getProducer() {
        return PRODUCER;
    }

    private static KafkaProducer<String, String> buildProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
}
