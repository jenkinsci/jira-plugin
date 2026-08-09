package hudson.plugins.jira.wiremock;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.util.Collections;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Runs {@link AbstractJiraRestServiceContractTest} against a real Jira Cloud instance (e.g. the
 * test instance referenced in {@code CONTRIBUTING.md}), for validating or refreshing
 * {@link JiraRestServiceWireMockTest}'s stub shapes when the Jira API contract changes.
 * <p>
 * Disabled by default. Running it creates real issues and versions in the target project (this
 * plugin has no delete-issue/delete-version API to clean them up with), so point it at a
 * disposable project, not a production one. To run it locally:
 * <pre>
 * JIRA_LIVE_TEST=true \
 * JIRA_LIVE_URL=https://your-instance.atlassian.net/ \
 * JIRA_LIVE_USER=you@example.com \
 * JIRA_LIVE_TOKEN=your-api-token \
 * JIRA_LIVE_PROJECT_KEY=SANDBOX \
 * mvn test -Dtest=LiveJiraCloudE2ETest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "JIRA_LIVE_TEST", matches = "true")
class LiveJiraCloudE2ETest extends AbstractJiraRestServiceContractTest {

    @Override
    protected JiraSite site(JenkinsRule j) throws Exception {
        return buildJiraSite(
                requireEnv("JIRA_LIVE_URL"),
                "live-jira-cloud-cred",
                requireEnv("JIRA_LIVE_USER"),
                requireEnv("JIRA_LIVE_TOKEN"));
    }

    @Override
    protected String projectKey() {
        return requireEnv("JIRA_LIVE_PROJECT_KEY");
    }

    @Override
    protected String givenIssue(String summary) throws Exception {
        // Project-scoped, not session.getIssueTypes(): that returns every issue type defined
        // in the whole Jira instance, which can include types this project's issue type scheme
        // doesn't actually accept (400 "Specify a valid issue type").
        IssueType issueType = session.service.getIssueTypes(projectKey()).stream()
                .filter(type -> !type.isSubtask())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Target project has no non-subtask issue types"));
        Issue created = session.createIssue(
                projectKey(),
                "Created by jira-plugin's LiveJiraCloudE2ETest, safe to delete.",
                null,
                Collections.emptyList(),
                summary,
                issueType.getId(),
                null);
        return created.getKey();
    }

    @Override
    protected int givenTransition(String issueKey) throws Exception {
        return session.service.getAvailableActions(issueKey).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Issue " + issueKey + " has no available transitions"))
                .getId();
    }

    @Override
    protected void prepareAddVersion(String versionName) {
        // Nothing to prepare: the real API creates the version for us.
    }

    @Override
    protected void prepareReleaseVersion(ExtendedVersion version) {
        // Nothing to prepare: the real API releases the version for us.
    }

    @Override
    protected void assertCommentWasSent(String issueKey, String commentBody) {
        Issue issue = session.service.getIssue(issueKey);
        boolean found = false;
        for (Comment comment : issue.getComments()) {
            if (commentBody.equals(comment.getBody())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Comment not found on re-fetched issue " + issueKey);
    }

    @Override
    protected void assertVersionReleased(ExtendedVersion version) {
        boolean released = session.service.getVersions(projectKey()).stream()
                .filter(v -> version.getName().equals(v.getName()))
                .findFirst()
                .map(ExtendedVersion::isReleased)
                .orElse(false);
        assertTrue(released, "Version " + version.getName() + " should be released after releaseVersion()");
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "JIRA_LIVE_TEST=true requires " + name + " to be set. See this class's Javadoc.");
        }
        return value;
    }
}
