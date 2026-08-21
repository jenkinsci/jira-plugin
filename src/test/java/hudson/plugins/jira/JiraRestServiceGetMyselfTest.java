package hudson.plugins.jira;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Covers rantoniuk/jira-plugin#202's follow-up: some Jira sites silently fall back to an
 * anonymous session on failed Basic authentication instead of returning 401 (Seraph's
 * {@code X-Seraph-Loginreason: AUTHENTICATED_FAILED}), which made {@code doValidate}'s previous
 * {@code getMyPermissions()}-based check report "Success" with rejected credentials.
 * {@link JiraRestService#getMyself()} hits {@code /myself} instead, which has no such fallback.
 */
@Tag("wiremock")
@WithJenkins
class JiraRestServiceGetMyselfTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void succeedsWhenCredentialsAreAccepted(JenkinsRule r) {
        wireMock.stubFor(get(urlPathEqualTo("/rest/api/2/myself"))
                .willReturn(okJson("{\"accountId\":\"1\",\"displayName\":\"Test User\"}")));

        service().getMyself();
    }

    @Test
    void throwsWhenCredentialsAreRejected(JenkinsRule r) {
        wireMock.stubFor(
                get(urlPathEqualTo("/rest/api/2/myself")).willReturn(aResponse().withStatus(401)));

        JiraRestService service = service();
        assertThrows(RestClientException.class, service::getMyself);
    }

    private JiraRestService service() {
        URI serverUri = URI.create(wireMock.baseUrl());
        return new JiraRestService(serverUri, null, "user", "pass", JiraSite.DEFAULT_TIMEOUT);
    }
}
