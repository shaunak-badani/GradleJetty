package com.myapp.cassandra;

/**
 * Override with the CASSANDRA_HOST / CASSANDRA_PORT / CASSANDRA_DATACENTER env vars.
 * Defaults match the official `cassandra` Docker image's out-of-the-box single-node config
 * (SimpleSnitch, datacenter "datacenter1").
 */
public class CassandraConfig {

    public static final String KEYSPACE = "financebudge";
    public static final String TABLE = "events";

    public static String host() {
        return System.getenv().getOrDefault("CASSANDRA_HOST", "localhost");
    }

    public static int port() {
        return Integer.parseInt(System.getenv().getOrDefault("CASSANDRA_PORT", "9042"));
    }

    public static String datacenter() {
        return System.getenv().getOrDefault("CASSANDRA_DATACENTER", "datacenter1");
    }
}
