package hudson.plugins.jira.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.oai.validator.model.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Runs {@link AbstractJiraRestServiceContractTest} against a local WireMock server instead of a
 * mocked {@link hudson.plugins.jira.extension.ExtendedJiraRestClient}, so the real HTTP
 * request/response wire format is what's actually validated. Runs offline as part of the
 * normal {@code mvn test} — WireMock only binds loopback.
 * <p>
 * Response bodies below are derived from Atlassian's official Jira Cloud platform OpenAPI spec
 * (<a href="https://developer.atlassian.com/cloud/jira/platform/swagger-v3.v3.json">swagger-v3.v3.json</a>),
 * trimmed to the fields {@code jira-rest-java-client-core} 6.0.2 actually parses. Deliberate
 * adaptations from the raw spec examples:
 * <ul>
 *   <li>{@code description}/{@code comment.body} are dropped: the spec (v3) represents them as
 *   Atlassian Document Format objects, but this plugin talks to {@code /rest/api/2}/
 *   {@code /rest/api/latest}, whose wire format predates ADF. Both fields are optional.</li>
 *   <li>Issue fixtures include empty top-level {@code "names": {}} and {@code "schema": {}}
 *   fields even though the spec examples omit them: {@code IssueJsonParser.parseFields} calls
 *   {@code JSONObject.keys()} on both unconditionally with no null-check, so a real Jira
 *   response's normally-present {@code names}/{@code schema} sections quietly avoid a bug that a
 *   hand-trimmed fixture without them would otherwise trip over.</li>
 * </ul>
 * Fixture bodies are additionally checked against the spec at test time via
 * {@link OpenApiSpecConformance}, so "derived from the OpenAPI spec" is enforced, not just
 * asserted in a comment — a fixture that drifts from the spec fails the test that defines it.
 * See {@code CONTRIBUTING.md} for how to refresh these against the real Jira Cloud test instance.
 */
@Tag("wiremock")
class JiraRestServiceWireMockTest extends AbstractJiraRestServiceContractTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // Derived from: GET /rest/api/3/serverInfo response example.
    // Needed because AsynchronousIssueRestClient looks up the server build number
    // before it can send a comment or a transition.
    private static final String SERVER_INFO_JSON = """
            {
              "baseUrl": "https://your-domain.atlassian.net",
              "version": "1001.0.0-SNAPSHOT",
              "buildNumber": 100000,
              "buildDate": "2024-01-01T00:00:00.000+00:00",
              "serverTime": "2024-01-01T00:00:00.000+00:00",
              "scmInfo": "unknown",
              "serverTitle": "Jira"
            }
            """;

    private static final String ISSUE_KEY = "TEST-1";

    @Override
    protected JiraSite site(JenkinsRule j) throws Exception {
        return buildJiraSite(wireMock.baseUrl(), "wiremock-cred", "bob", "secret");
    }

    @Override
    protected String projectKey() {
        return "TEST";
    }

    @Override
    protected String givenIssue(String summary) {
        // Derived from: GET /rest/api/3/issue/{issueIdOrKey} response example, trimmed of
        // attachments/watchers/subtasks/description (all optional). "self" always points back at
        // this WireMock server (not a fixed placeholder) because progressWorkflowAction() derives
        // the transitions URI from it.
        String self = wireMock.baseUrl() + "/rest/api/latest/issue/" + ISSUE_KEY;
        String issueJson = """
                {
                  "expand": "",
                  "id": "10001",
                  "self": "%s",
                  "key": "%s",
                  "fields": {
                    "summary": "%s",
                    "issuetype": { "self": "%s/type", "id": 1, "name": "Bug", "subtask": false },
                    "status": { "self": "%s/status", "name": "Open", "description": "", "iconUrl": "%s/icon", "id": 1 },
                    "project": { "self": "%s/project", "id": 10000, "key": "TEST", "name": "Example" },
                    "created": "2024-01-01T10:00:00.000+00:00",
                    "updated": "2024-01-02T11:30:00.000+00:00"
                  },
                  "names": {},
                  "schema": {}
                }
                """.formatted(self, ISSUE_KEY, summary, self, self, self, self);
        OpenApiSpecConformance.assertConformsToSpec(
                "/rest/api/3/issue/{issueIdOrKey}", Request.Method.GET, 200, issueJson);
        wireMock.stubFor(
                get(urlPathEqualTo("/rest/api/latest/issue/" + ISSUE_KEY)).willReturn(okJson(issueJson)));

        OpenApiSpecConformance.assertConformsToSpec(
                "/rest/api/3/serverInfo", Request.Method.GET, 200, SERVER_INFO_JSON);
        wireMock.stubFor(get(urlPathEqualTo("/rest/api/latest/serverInfo")).willReturn(okJson(SERVER_INFO_JSON)));

        // Derived from: POST /rest/api/3/issue/{issueIdOrKey}/comment response example (201, body unused by the
        // caller — no conformance check since there's no body to check).
        wireMock.stubFor(post(urlPathEqualTo("/rest/api/2/issue/" + ISSUE_KEY + "/comment"))
                .willReturn(aResponse().withStatus(201)));
        return ISSUE_KEY;
    }

    @Override
    protected int givenTransition(String issueKey) {
        // Derived from: POST /rest/api/3/issue/{issueIdOrKey}/transitions response example (204, no body).
        wireMock.stubFor(post(urlPathEqualTo("/rest/api/latest/issue/" + issueKey + "/transitions"))
                .willReturn(aResponse().withStatus(204)));
        return 21;
    }

    @Override
    protected void prepareAddVersion(String versionName) {
        // Derived from: POST /rest/api/3/version response example.
        String createdVersionJson = """
                {
                  "self": "https://your-domain.atlassian.net/rest/api/2/version/10000",
                  "id": "10000",
                  "name": "%s",
                  "description": "An excellent version",
                  "archived": false,
                  "released": false
                }
                """.formatted(versionName);
        OpenApiSpecConformance.assertConformsToSpec(
                "/rest/api/3/version", Request.Method.POST, 201, createdVersionJson);
        wireMock.stubFor(post(urlPathEqualTo("/rest/api/latest/version"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createdVersionJson)));

        // WireMock has no real state, so getVersions() needs its own stub reflecting what the
        // addVersion() call above "created" — same id, so a later releaseVersion() PUT targets
        // the same /rest/api/2/version/10000 URL. Derived from:
        // GET /rest/api/3/project/{projectIdOrKey}/versions response example.
        String versionsListJson = """
                [
                  {
                    "self": "https://your-domain.atlassian.net/rest/api/2/version/10000",
                    "id": "10000",
                    "name": "%s",
                    "description": "An excellent version",
                    "archived": false,
                    "released": false,
                    "startDate": "2024-01-01"
                  }
                ]
                """.formatted(versionName);
        OpenApiSpecConformance.assertConformsToSpec(
                "/rest/api/3/project/{projectIdOrKey}/versions", Request.Method.GET, 200, versionsListJson);
        wireMock.stubFor(
                get(urlPathEqualTo("/rest/api/2/project/TEST/versions")).willReturn(okJson(versionsListJson)));
    }

    @Override
    protected void prepareReleaseVersion(ExtendedVersion version) {
        // Derived from: PUT /rest/api/3/version/{id} response example.
        String releasedVersionJson = """
                {
                  "self": "https://your-domain.atlassian.net/rest/api/2/version/%s",
                  "id": "%s",
                  "name": "%s",
                  "description": "An excellent version",
                  "archived": false,
                  "released": true,
                  "releaseDate": "2010-07-06"
                }
                """.formatted(version.getId(), version.getId(), version.getName());
        OpenApiSpecConformance.assertConformsToSpec(
                "/rest/api/3/version/{id}", Request.Method.PUT, 200, releasedVersionJson);
        wireMock.stubFor(put(urlPathEqualTo("/rest/api/2/version/" + version.getId()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(releasedVersionJson)));
    }

    @Override
    protected void prepareGetComponents() {
        // Derived from: GET /rest/api/3/project/{projectIdOrKey}/components response example.
        String componentsJson = """
                [
                  {
                    "self": "%s/rest/api/2/component/10000",
                    "id": "10000",
                    "name": "Backend",
                    "description": "Backend services"
                  }
                ]
                """.formatted(wireMock.baseUrl());
        OpenApiSpecConformance.assertConformsToSpec(
                "/rest/api/3/project/{projectIdOrKey}/components", Request.Method.GET, 200, componentsJson);
        wireMock.stubFor(
                get(urlPathEqualTo("/rest/api/2/project/TEST/components")).willReturn(okJson(componentsJson)));
    }

    @Override
    protected void assertCommentWasSent(String issueKey, String commentBody) {
        wireMock.verify(postRequestedFor(urlPathEqualTo("/rest/api/2/issue/" + issueKey + "/comment")));
    }

    @Override
    protected void assertVersionReleased(ExtendedVersion version) {
        // Checks the request body, not just that a PUT happened: WireMock's stub returns a
        // canned "released": true response no matter what was sent, so without this the test
        // can't tell a real release request apart from one that forgot to set released=true.
        wireMock.verify(putRequestedFor(urlPathEqualTo("/rest/api/2/version/" + version.getId()))
                .withRequestBody(matchingJsonPath("$.released", equalTo("true"))));
    }

    @Test
    void getIssueTypesProjectScopedReturnsOnlyThatProjectsTypes() {
        // Derived from: GET /rest/api/3/project/{projectIdOrKey} response example, trimmed to
        // the fields ProjectJsonParser actually reads. lead/versions/components are parsed
        // unconditionally (empty is fine) even though this plugin only cares about issueTypes.
        String projectJson = """
                {
                  "self": "%1$s/rest/api/latest/project/TEST",
                  "key": "TEST",
                  "id": "10000",
                  "name": "Example",
                  "lead": {},
                  "versions": [],
                  "components": [],
                  "issueTypes": [
                    { "self": "%1$s/rest/api/latest/issuetype/1", "id": "1", "name": "Bug", "subtask": false },
                    { "self": "%1$s/rest/api/latest/issuetype/2", "id": "2", "name": "Sub-task", "subtask": true }
                  ]
                }
                """.formatted(wireMock.baseUrl());
        OpenApiSpecConformance.assertConformsToSpec(
                "/rest/api/3/project/{projectIdOrKey}", Request.Method.GET, 200, projectJson);
        wireMock.stubFor(get(urlPathEqualTo("/rest/api/latest/project/TEST")).willReturn(okJson(projectJson)));

        List<IssueType> issueTypes = session.service.getIssueTypes(projectKey());

        assertEquals(2, issueTypes.size());
        assertTrue(issueTypes.stream().anyMatch(type -> "Bug".equals(type.getName()) && !type.isSubtask()));
        assertTrue(issueTypes.stream().anyMatch(type -> "Sub-task".equals(type.getName()) && type.isSubtask()));
    }
}
