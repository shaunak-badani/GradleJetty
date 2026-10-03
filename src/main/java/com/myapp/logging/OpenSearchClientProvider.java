package com.myapp.logging;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

/**
 * One shared OpenSearchClient, pointed at a locally running OpenSearch node.
 *
 * Assumes OpenSearch is reachable with no auth (the usual setup for a local,
 * single-node, security-disabled dev instance). Override host/port/scheme
 * with the OPENSEARCH_HOST / OPENSEARCH_PORT / OPENSEARCH_SCHEME env vars.
 * If your instance has the security plugin on, you'll need to add
 * credentials/TLS handling to the transport builder below.
 */
public class OpenSearchClientProvider {

    private static final OpenSearchClient CLIENT = buildClient();

    public static OpenSearchClient getClient() {
        return CLIENT;
    }

    private static OpenSearchClient buildClient() {
        String host = System.getenv().getOrDefault("OPENSEARCH_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("OPENSEARCH_PORT", "9200"));
        String scheme = System.getenv().getOrDefault("OPENSEARCH_SCHEME", "http");

        HttpHost httpHost = new HttpHost(scheme, host, port);
        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder.builder(httpHost).build();
        return new OpenSearchClient(transport);
    }
}
