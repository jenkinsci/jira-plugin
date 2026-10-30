package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Component;
import io.atlassian.util.concurrent.Promise;
import java.net.URI;

public interface ExtendedJiraRestClient extends JiraRestClient {
    ExtendedVersionRestClient getExtendedVersionRestClient();

    ExtendedMyPermissionsRestClient getExtendedMyPermissionsRestClient();

    Promise<Iterable<ExtendedVersion>> getVersionsForProject(URI uri);

    Promise<Iterable<Component>> getComponentsForProject(URI uri);
}
