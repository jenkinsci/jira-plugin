package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.VersionRestClient;
import com.atlassian.util.concurrent.Promise;

import java.net.URI;

public interface ExtendedVersionRestClient extends VersionRestClient {
    Promise<ExtendedVersion> getExtendedVersion(URI versionUri);
    Promise<ExtendedVersion> createExtendedVersion(ExtendedVersionInput versionInput);
    Promise<ExtendedVersion> updateExtendedVersion(URI versionUri, ExtendedVersionInput versionInput);
}