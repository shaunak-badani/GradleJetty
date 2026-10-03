package com.myapp.logging;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Appends one JSON line per log entry to logs/application.log, relative to
 * the Jetty base directory (this project root, since startJetty runs with
 * jetty.base=.). This is the "raw log file on disk" half of the demo -
 * OpenSearch never reads this file directly, the servlet reads it and
 * indexes each entry itself.
 */
public class LogFileWriter {

    private static final Path LOG_FILE = Paths.get("logs", "application.log");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void append(LogEntry entry) throws IOException {
        Files.createDirectories(LOG_FILE.getParent());
        String line = MAPPER.writeValueAsString(entry) + System.lineSeparator();
        Files.write(LOG_FILE, line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
