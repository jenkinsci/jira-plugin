package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import io.atlassian.util.concurrent.Promise;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ws.rs.core.UriBuilder;

public class ExtendedAsynchronousJiraRestClient extends AsynchronousJiraRestClient implements ExtendedJiraRestClient {

    private static final Logger LOGGER = Logger.getLogger(ExtendedAsynchronousJiraRestClient.class.getName());

    // jira-rest-java-client's own Cloud auto-detection (UriUtil.isURICloud) doesn't know about
    // api.atlassian.com, the API-gateway URL form Atlassian's own docs recommend, so it routes
    // JQL searches on such a site to the removed /search endpoint and gets a 410. This mirrors
    // that heuristic with the missing domain added, and is passed explicitly to the superclass
    // instead of relying on its own detection.
    private static final List<String> CLOUD_DOMAINS = List.of("atlassian.net", "jira.com", "api.atlassian.com");
    private static final List<String> DATA_CENTER_DOMAINS = List.of("localhost");

    private final ExtendedVersionRestClient extendedVersionRestClient;
    private final ExtendedMyPermissionsRestClient extendedMyPermissionsRestClient;
    private final ExtendedAsynchronousProjectRestClient extendedProjectRestClient;

    public ExtendedAsynchronousJiraRestClient(URI serverUri, DisposableHttpClient httpClient) {
        this(serverUri, httpClient, isCloudUri(serverUri));
    }

    public ExtendedAsynchronousJiraRestClient(URI serverUri, DisposableHttpClient httpClient, boolean cloud) {
        super(serverUri, httpClient, cloud);
        LOGGER.fine(() -> "Jira site " + serverUri + " classified as " + (cloud ? "Cloud" : "Server/Data Center")
                + ", JQL search will use " + (cloud ? "/search/jql" : "/search"));
        final URI baseUri =
                UriBuilder.fromUri(serverUri).path("/rest/api/latest").build();
        extendedVersionRestClient = new ExtendedAsynchronousVersionRestClient(baseUri, httpClient);
        extendedMyPermissionsRestClient = new ExtendedAsynchronousMyPermissionsRestClient(baseUri, httpClient);
        extendedProjectRestClient = new ExtendedAsynchronousProjectRestClient(httpClient);
    }

    static boolean isCloudUri(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return CLOUD_DOMAINS.stream().anyMatch(host::contains)
                && DATA_CENTER_DOMAINS.stream().noneMatch(host::contains);
    }

    @Override
    public ExtendedVersionRestClient getExtendedVersionRestClient() {
        return extendedVersionRestClient;
    }

    @Override
    public ExtendedMyPermissionsRestClient getExtendedMyPermissionsRestClient() {
        return extendedMyPermissionsRestClient;
    }

    @Override
    public Promise<Iterable<ExtendedVersion>> getVersionsForProject(URI uri) {
        return extendedProjectRestClient.getVersionsForProject(uri);
    }

    @Override
    public Promise<Iterable<Component>> getComponentsForProject(URI uri) {
        return extendedProjectRestClient.getComponentsForProject(uri);
    }
}
