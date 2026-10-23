package com.myapp.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;

import java.net.InetSocketAddress;

/**
 * One shared CqlSession, same sharing pattern as OpenSearchClientProvider / KafkaProducerProvider.
 *
 * Builds the keyspace and table on first use if they don't exist yet - this is a demo app, a real
 * deployment would manage schema separately rather than create-if-missing on startup.
 */
public class CassandraSessionProvider {

    private static final CqlSession SESSION = buildSession();

    public static CqlSession getSession() {
        return SESSION;
    }

    private static CqlSession buildSession() {
        InetSocketAddress contactPoint = new InetSocketAddress(CassandraConfig.host(), CassandraConfig.port());

        // Bootstrap session with no keyspace yet, just to create it if missing.
        try (CqlSession bootstrap = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(CassandraConfig.datacenter())
                .build()) {
            bootstrap.execute("CREATE KEYSPACE IF NOT EXISTS " + CassandraConfig.KEYSPACE +
                    " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
        }

        CqlSession session = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(CassandraConfig.datacenter())
                .withKeyspace(CassandraConfig.KEYSPACE)
                .build();

        // source is the partition key (decides which node owns the row), created_at is a
        // clustering column (sorts rows within a partition, newest first).
        session.execute("CREATE TABLE IF NOT EXISTS " + CassandraConfig.TABLE + " (" +
                "source text, " +
                "created_at timestamp, " +
                "message text, " +
                "PRIMARY KEY (source, created_at)" +
                ") WITH CLUSTERING ORDER BY (created_at DESC)");

        return session;
    }
}
