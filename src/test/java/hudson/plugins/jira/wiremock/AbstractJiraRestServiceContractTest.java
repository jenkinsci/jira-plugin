package hudson.plugins.jira.wiremock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Version;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.plugins.jira.JiraRestService;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Contract for {@link JiraRestService}'s operations, written once and run against two
 * different backends by two thin subclasses:
 * <ul>
 *   <li>{@link JiraRestServiceWireMockTest} — a local WireMock server, so this runs offline
 *   as part of the normal {@code mvn test}.</li>
 *   <li>{@link LiveJiraCloudE2ETest} — a real Jira Cloud instance, opt-in and env-var-gated,
 *   for validating/refreshing the WireMock stub shapes when the Jira API contract changes.</li>
 * </ul>
 * The test bodies below only call {@code session.service.*} and assert on the result; they
 * don't know or care which backend they're talking to. Each subclass supplies the backend by
 * implementing {@link #site(JenkinsRule)} and the handful of {@code given*}/{@code prepare*}
 * hooks below — WireMock's implementations register stubs, the live instance's create real
 * data — so a change to one of these test methods automatically applies to both backends
 * instead of needing to be duplicated.
 */
@WithJenkins
abstract class AbstractJiraRestServiceContractTest {

    protected JiraSession session;

    @BeforeEach
    void initSession(JenkinsRule j) throws Exception {
        session = site(j).getSession(null);
    }

    /** Builds the {@link JiraSite} this test runs against. */
    protected abstract JiraSite site(JenkinsRule j) throws Exception;

    protected abstract String projectKey();

    /** Ensures an issue with the given summary exists and is independently fetchable; returns its key. */
    protected abstract String givenIssue(String summary) throws Exception;

    /** Returns a workflow-transition id valid for the given issue right now. */
    protected abstract int givenTransition(String issueKey) throws Exception;

    /** Prepares whatever's needed for {@code addVersion(projectKey(), versionName)} to succeed. */
    protected abstract void prepareAddVersion(String versionName) throws Exception;

    /** Prepares whatever's needed for {@code releaseVersion(projectKey(), version)} to succeed. */
    protected abstract void prepareReleaseVersion(ExtendedVersion version) throws Exception;

    /** Verifies the comment sent by {@link #addCommentSendsExpectedRequest()} actually reached the issue. */
    protected abstract void assertCommentWasSent(String issueKey, String commentBody) throws Exception;

    /** Verifies the version released by {@link #releaseVersionMarksVersionReleased()} is now released. */
    protected abstract void assertVersionReleased(ExtendedVersion version) throws Exception;

    protected static String uniqueName(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    protected JiraSite buildJiraSite(String baseUrl, String credentialsId, String username, String password)
            throws Exception {
        UsernamePasswordCredentialsImpl credentials =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, null, username, password);

        SystemCredentialsProvider systemProvider = SystemCredentialsProvider.getInstance();
        systemProvider.getCredentials().add(credentials);
        systemProvider.save();

        JiraSite site = new JiraSite(baseUrl);
        site.setCredentialsId(credentialsId);
        return site;
    }

    @Test
    void getIssueParsesRealResponseShape() throws Exception {
        String issueKey = givenIssue("Main order flow broken");

        Issue issue = session.service.getIssue(issueKey);

        assertEquals(issueKey, issue.getKey());
        assertEquals("Main order flow broken", issue.getSummary());
        assertEquals(projectKey(), issue.getProject().getKey());
        assertNotNull(issue.getStatus());
    }

    @Test
    void addCommentSendsExpectedRequest() throws Exception {
        String issueKey = givenIssue("Issue for addComment contract test");
        String commentBody = "Comment from JiraRestService contract test";

        session.service.addComment(issueKey, commentBody, null, null);

        assertCommentWasSent(issueKey, commentBody);
    }

    @Test
    void progressWorkflowActionTransitionsIssue() throws Exception {
        String issueKey = givenIssue("Issue for transition contract test");
        String statusBeforeTransition =
                session.service.getIssue(issueKey).getStatus().getName();
        int actionId = givenTransition(issueKey);

        Issue issue = session.service.progressWorkflowAction(issueKey, actionId);

        assertEquals(issueKey, issue.getKey());
        // The transitions endpoint returns 204 with no body, so a correct implementation must
        // re-fetch the issue afterwards to report its actual new status rather than the
        // pre-transition snapshot it started from.
        assertNotEquals(statusBeforeTransition, issue.getStatus().getName());
    }

    @Test
    void getVersionsParsesRealResponseShape() throws Exception {
        String versionName = uniqueName("contract-getversions");
        prepareAddVersion(versionName);
        session.service.addVersion(projectKey(), versionName);

        List<ExtendedVersion> versions = session.service.getVersions(projectKey());

        ExtendedVersion found = versions.stream()
                .filter(v -> versionName.equals(v.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Created version not found in getVersions(): " + versionName));
        assertFalse(found.isReleased());
    }

    @Test
    void addVersionCreatesNewVersion() throws Exception {
        String versionName = uniqueName("contract-addversion");
        prepareAddVersion(versionName);

        Version created = session.service.addVersion(projectKey(), versionName);

        assertEquals(versionName, created.getName());
        assertFalse(created.isReleased());
    }

    @Test
    void releaseVersionMarksVersionReleased() throws Exception {
        String versionName = uniqueName("contract-release");
        prepareAddVersion(versionName);
        session.service.addVersion(projectKey(), versionName);
        ExtendedVersion created = session.service.getVersions(projectKey()).stream()
                .filter(v -> versionName.equals(v.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Created version not found in getVersions(): " + versionName));
        prepareReleaseVersion(created);

        // releaseVersion() PUTs whatever isReleased() says on the version it's given - it
        // doesn't flip the flag itself, so the caller must ask for a released copy (mirroring
        // production usage in VersionReleaser).
        ExtendedVersion toRelease = new ExtendedVersion(
                created.getSelf(),
                created.getId(),
                created.getName(),
                created.getDescription(),
                created.isArchived(),
                true,
                created.getStartDate(),
                created.getReleaseDate());

        session.service.releaseVersion(projectKey(), toRelease);

        assertVersionReleased(toRelease);
    }
}
