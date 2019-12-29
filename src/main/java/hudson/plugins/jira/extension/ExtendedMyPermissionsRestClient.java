package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.domain.Permissions;
import com.atlassian.util.concurrent.Promise;

public interface ExtendedMyPermissionsRestClient {

    Promise<Permissions> getMyPermissions();
}
