package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class ExtendedAsynchronousJiraRestClient extends AsynchronousJiraRestClient implements ExtendedJiraRestClient {
    private final ExtendedVersionRestClient extendedVersionRestClient;

    public ExtendedAsynchronousJiraRestClient(URI serverUri, DisposableHttpClient httpClient) {
        super(serverUri, httpClient);
        final URI baseUri = UriBuilder.fromUri(serverUri).path("/rest/api/latest").build();
        extendedVersionRestClient = new AsynchronousExtendedVersionRestClient(baseUri, httpClient);
    }

    @Override
    public ExtendedVersionRestClient getExtendedVersionRestClient() {
        return extendedVersionRestClient;
    }
}