package com.myapp.logging;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

/**
 * A second OpenSearchClient, pointed directly at cluster-b's own HTTP port (CCS_REMOTE_HOST /
 * CCS_REMOTE_PORT). The local cluster's "cluster-b:" remote registration only supports search -
 * indexing a document into a remote cluster needs to talk to that cluster directly.
 */
public class RemoteOpenSearchClientProvider {

    private static final OpenSearchClient CLIENT = buildClient();

    public static OpenSearchClient getClient() {
        return CLIENT;
    }

    private static OpenSearchClient buildClient() {
        HttpHost httpHost = new HttpHost(CrossClusterConfig.remoteScheme(), CrossClusterConfig.remoteHost(), CrossClusterConfig.remotePort());
        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder.builder(httpHost).build();
        return new OpenSearchClient(transport);
    }
}
