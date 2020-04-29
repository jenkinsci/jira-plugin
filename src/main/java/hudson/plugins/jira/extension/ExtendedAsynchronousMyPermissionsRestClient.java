package hudson.plugins.jira.extension;

import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.jira.rest.client.api.domain.Permissions;
import com.atlassian.jira.rest.client.internal.async.AsynchronousMyPermissionsRestClient;
import com.atlassian.jira.rest.client.internal.json.PermissionsJsonParser;
import io.atlassian.util.concurrent.Promise;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;

public class ExtendedAsynchronousMyPermissionsRestClient extends AsynchronousMyPermissionsRestClient
    implements ExtendedMyPermissionsRestClient {

  private static final String URI_PREFIX = "mypermissions";
  private final URI baseUri;
  private final PermissionsJsonParser permissionsJsonParser = new PermissionsJsonParser();

  ExtendedAsynchronousMyPermissionsRestClient(final URI baseUri, final HttpClient client) {
    super(baseUri, client);
    this.baseUri = baseUri;
  }

  @Override
  public Promise<Permissions> getMyPermissions() {
    final UriBuilder uriBuilder = UriBuilder.fromUri(baseUri).path(URI_PREFIX);
    uriBuilder.queryParam("permissions", "BROWSE_PROJECTS");
    return getAndParse(uriBuilder.build(), permissionsJsonParser);
  }
}
