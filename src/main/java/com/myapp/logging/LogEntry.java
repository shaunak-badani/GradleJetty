package com.myapp.logging;

import java.time.Instant;

/**
 * One structured log record: the unit we write to the log file and the
 * unit we index into OpenSearch. Same shape in both places on purpose -
 * that's what lets a raw log line become a searchable document.
 */
public class LogEntry {

    private String timestamp;
    private String level;
    private String message;
    private String source;

    public LogEntry() {
    }

    public LogEntry(String level, String message, String source) {
        this.timestamp = Instant.now().toString();
        this.level = level;
        this.message = message;
        this.source = source;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
