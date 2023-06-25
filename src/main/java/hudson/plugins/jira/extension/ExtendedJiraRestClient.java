package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.JiraRestClient;

public interface ExtendedJiraRestClient extends JiraRestClient {
    ExtendedVersionRestClient getExtendedVersionRestClient();

    ExtendedMyPermissionsRestClient getExtendedMyPermissionsRestClient();
}
