package hudson.plugins.jira.extension;

import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.internal.async.AbstractAsynchronousRestClient;
import com.atlassian.jira.rest.client.internal.json.ComponentJsonParser;
import com.atlassian.jira.rest.client.internal.json.GenericJsonArrayParser;
import io.atlassian.util.concurrent.Promise;
import java.net.URI;

final class ExtendedAsynchronousProjectRestClient extends AbstractAsynchronousRestClient {

    private final GenericJsonArrayParser<ExtendedVersion> versionsJsonParser =
            GenericJsonArrayParser.create(new ExtendedVersionJsonParser());
    private final GenericJsonArrayParser<Component> componentsJsonParser =
            GenericJsonArrayParser.create(new ComponentJsonParser());

    ExtendedAsynchronousProjectRestClient(HttpClient client) {
        super(client);
    }

    Promise<Iterable<ExtendedVersion>> getVersionsForProject(URI uri) {
        return getAndParse(uri, versionsJsonParser);
    }

    Promise<Iterable<Component>> getComponentsForProject(URI uri) {
        return getAndParse(uri, componentsJsonParser);
    }
}
