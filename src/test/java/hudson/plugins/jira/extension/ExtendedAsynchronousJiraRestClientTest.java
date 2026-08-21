package hudson.plugins.jira.extension;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousHttpClientFactory;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hudson.plugins.jira.JiraRestService;
import hudson.plugins.jira.JiraSite;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Covers rantoniuk/jira-plugin#202: jira-rest-java-client's own Cloud auto-detection
 * (UriUtil.isURICloud) doesn't recognize api.atlassian.com, so a JQL search against a site
 * configured with that API-gateway URL form is routed to the removed /search endpoint and gets
 * a 410. {@link ExtendedAsynchronousJiraRestClient} computes the Cloud flag itself instead of
 * relying on that detection.
 * <p>
 * {@code @WithJenkins} is needed even though this test never touches {@code JiraSite}: this
 * plugin's vendored {@code ApacheAsyncHttpClient} (see ADR 0006) looks up {@link
 * jenkins.model.Jenkins#get()} for proxy configuration on construction, and {@link
 * AsynchronousHttpClientFactory#createClient} builds one under the hood.
 */
@Tag("wiremock")
@WithJenkins
class ExtendedAsynchronousJiraRestClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String SEARCH_RESULT_JSON = """
            {
              "startAt": 0,
              "maxResults": 50,
              "total": 0,
              "issues": [],
              "names": {},
              "schema": {}
            }
            """;

    @Test
    void cloudSiteSearchesAgainstSearchJqlEndpoint(JenkinsRule r) throws Exception {
        String jql = "project = TEST";
        wireMock.stubFor(get(urlPathEqualTo("/rest/api/latest/search/jql")).willReturn(okJson(SEARCH_RESULT_JSON)));

        List<Issue> issues = search(true, jql);

        assertTrue(issues.isEmpty());
        wireMock.verify(
                getRequestedFor(urlPathEqualTo("/rest/api/latest/search/jql")).withQueryParam("jql", equalTo(jql)));
    }

    @Test
    void dataCenterSiteSearchesAgainstSearchEndpoint(JenkinsRule r) throws Exception {
        String jql = "project = TEST";
        wireMock.stubFor(get(urlPathEqualTo("/rest/api/latest/search")).willReturn(okJson(SEARCH_RESULT_JSON)));

        List<Issue> issues = search(false, jql);

        assertTrue(issues.isEmpty());
        wireMock.verify(
                getRequestedFor(urlPathEqualTo("/rest/api/latest/search")).withQueryParam("jql", equalTo(jql)));
    }

    @Test
    void isCloudUriRecognizesApiAtlassianComAndKnownCloudDomains() {
        assertTrue(ExtendedAsynchronousJiraRestClient.isCloudUri(URI.create("https://mycompany.atlassian.net/")));
        assertTrue(ExtendedAsynchronousJiraRestClient.isCloudUri(
                URI.create("https://api.atlassian.com/ex/jira/cloud-id-123/")));
        assertTrue(ExtendedAsynchronousJiraRestClient.isCloudUri(URI.create("https://mycompany.jira.com/")));
    }

    @Test
    void isCloudUriRejectsDataCenterAndLocalDomains() {
        assertFalse(ExtendedAsynchronousJiraRestClient.isCloudUri(URI.create("https://jira.mycompany.internal/")));
        assertFalse(ExtendedAsynchronousJiraRestClient.isCloudUri(URI.create("http://localhost:8080/")));
    }

    @Test
    void logsCloudClassificationAtFineLevel(JenkinsRule r) throws Exception {
        Logger logger = Logger.getLogger(ExtendedAsynchronousJiraRestClient.class.getName());
        Level originalLevel = logger.getLevel();
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);
        try {
            URI serverUri = URI.create("https://mycompany.atlassian.net/");
            DisposableHttpClient httpClient =
                    new AsynchronousHttpClientFactory().createClient(serverUri, new AnonymousAuthenticationHandler());
            new ExtendedAsynchronousJiraRestClient(serverUri, httpClient, true);
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
        }

        assertTrue(records.stream().anyMatch(record -> record.getMessage().contains("classified as Cloud")));
    }

    private List<Issue> search(boolean cloud, String jql) throws Exception {
        URI serverUri = URI.create(wireMock.baseUrl());
        DisposableHttpClient httpClient =
                new AsynchronousHttpClientFactory().createClient(serverUri, new AnonymousAuthenticationHandler());
        ExtendedAsynchronousJiraRestClient client =
                new ExtendedAsynchronousJiraRestClient(serverUri, httpClient, cloud);
        JiraRestService service = new JiraRestService(serverUri, client, "user", "pass", JiraSite.DEFAULT_TIMEOUT);
        return service.getIssuesFromJqlSearch(jql, 50);
    }
}
