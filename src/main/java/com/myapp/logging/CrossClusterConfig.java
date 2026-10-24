package com.myapp.logging;

/**
 * Shared config for the cross-cluster search demo: the alias "cluster-b" is registered as a
 * remote on the local OpenSearch cluster (see README "Cross-cluster search" section) via
 * cluster.remote.cluster-b.seeds. That registration is only good for *search* - CCS has no write
 * path - so writing directly into cluster-b needs its own client pointed at its own HTTP port,
 * which is what CCS_REMOTE_HOST/PORT are for.
 */
public class CrossClusterConfig {

    public static final String ALIAS = System.getenv().getOrDefault("CCS_REMOTE_CLUSTER_ALIAS", "cluster-b");

    public static String remoteHost() {
        return System.getenv().getOrDefault("CCS_REMOTE_HOST", "localhost");
    }

    public static int remotePort() {
        return Integer.parseInt(System.getenv().getOrDefault("CCS_REMOTE_PORT", "9207"));
    }

    public static String remoteScheme() {
        return System.getenv().getOrDefault("CCS_REMOTE_SCHEME", "http");
    }
}
