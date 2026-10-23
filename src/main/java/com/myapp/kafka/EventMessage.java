package com.myapp.kafka;

import java.time.Instant;

/**
 * One Kafka record, in both directions: what a producer sends (key/value)
 * and what a consumer sees back (same fields, plus the partition/offset the
 * broker assigned it).
 */
public class EventMessage {

    private String key;
    private String value;
    private String timestamp;
    private Integer partition;
    private Long offset;

    public EventMessage() {
    }

    public EventMessage(String key, String value) {
        this.key = key;
        this.value = value;
        this.timestamp = Instant.now().toString();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }
}
