package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.domain.Permissions;
import io.atlassian.util.concurrent.Promise;

public interface ExtendedMyPermissionsRestClient {

  Promise<Permissions> getMyPermissions();
}
