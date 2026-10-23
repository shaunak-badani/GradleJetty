package com.myapp.cassandra;

/** One row: the partition key (source), the clustering key (createdAt), and the payload (message). */
public class EventRecord {

    private String source;
    private String message;
    private String createdAt;

    public EventRecord() {
    }

    public EventRecord(String source, String message, String createdAt) {
        this.source = source;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
