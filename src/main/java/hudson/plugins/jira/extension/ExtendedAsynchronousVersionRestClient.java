package hudson.plugins.jira.extension;

import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousVersionRestClient;
import com.atlassian.util.concurrent.Promise;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class ExtendedAsynchronousVersionRestClient extends AsynchronousVersionRestClient implements ExtendedVersionRestClient {
    private final URI versionRootUri;

	ExtendedAsynchronousVersionRestClient(URI baseUri, HttpClient client) {
        super(baseUri, client);
        versionRootUri = UriBuilder.fromUri(baseUri).path("version").build();
    }

    @Override
    public Promise<ExtendedVersion> getExtendedVersion(URI versionUri) {
        return getAndParse(versionUri, new ExtendedVersionJsonParser());
    }

    @Override
    public Promise<ExtendedVersion> createExtendedVersion(ExtendedVersionInput versionInput) {
        return postAndParse(versionRootUri, versionInput, new ExtendedVersionInputJsonGenerator(), new ExtendedVersionJsonParser());
    }

    @Override
    public Promise<ExtendedVersion> updateExtendedVersion(URI versionUri, ExtendedVersionInput versionInput) {
        return putAndParse(versionUri, versionInput, new ExtendedVersionInputJsonGenerator(), new ExtendedVersionJsonParser());
    }
}