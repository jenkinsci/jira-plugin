package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.net.URI;
import java.util.List;

class JiraCloudRestServiceTest {

    private static final String JIRA_CLOUD_URL = "https://jenkins-jira-plugin.atlassian.net";
    private final String apiUsername;
    private final String apiToken;

    public JiraCloudRestServiceTest() {
        this.apiUsername = Optional.ofNullable(System.getenv("JIRA_API_USERNAME"))
            .orElseThrow(() -> new IllegalStateException("Environment variable JIRA_API_USERNAME is not set"));
        this.apiToken = Optional.ofNullable(System.getenv("JIRA_API_TOKEN"))
            .orElseThrow(() -> new IllegalStateException("Environment variable JIRA_API_TOKEN is not set"));
    }

    @Test
    void testConnectionToJiraCloud() {
        JiraRestService service = new JiraRestService(
            URI.create(JIRA_CLOUD_URL),
            null,
            apiUsername, 
            apiToken,
            JiraSite.DEFAULT_TIMEOUT
        );

        assertNotNull(service, "JiraRestService instance should not be null");
        // Add more assertions or integration logic as needed
    }

    @Test
    void testEnhancedSearchJql() throws Exception {
        JiraRestService service = new JiraRestService(
            URI.create(JIRA_CLOUD_URL),
            null, // Replace with actual client if needed
            apiUsername,
            apiToken,
            JiraSite.DEFAULT_TIMEOUT
        );

        String jqlQuery = "key = TESTME-1";
        int maxResults = 1;

        List<Issue> issues = service.getIssuesFromJqlSearch(jqlQuery, maxResults);

        assertNotNull(issues, "Issues list should not be null");
        assertFalse(issues.isEmpty(), "Issues list should not be empty");
        assertEquals("TESTME-1", issues.get(0).getKey(), "Expected issue key to be TESTME-1");
    }
}