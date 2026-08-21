package hudson.plugins.jira.extension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousHttpClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * jira-rest-java-client's compiled classes call {@code jakarta.ws.rs.core.UriBuilder} (the JAX-RS
 * 3.x / Jakarta EE 9+ package) starting with its 7.x line. Jenkins' {@code jersey2-api} plugin
 * only ships {@code jakarta.ws.rs-api:2.1.6}, which despite its "jakarta" groupId still exposes
 * the pre-EE-9 {@code javax.ws.rs.*} package names, not {@code jakarta.ws.rs.*}. Without
 * {@code jersey3-api} on the classpath, constructing the real Atlassian client throws
 * {@link NoClassDefFoundError} for {@code UriBuilder} the moment a real REST call is attempted.
 *
 * <p>{@code @WithJenkins} is needed even though this test never touches {@code JiraSite}: this
 * plugin's vendored {@code ApacheAsyncHttpClient} (see ADR 0006) looks up {@link
 * jenkins.model.Jenkins#get()} for proxy configuration on construction, and {@link
 * AsynchronousHttpClientFactory#createClient} builds one under the hood.
 */
@WithJenkins
class JiraRestClientClasspathTest {

    @Test
    void realClientConstructionDoesNotThrowNoClassDefFoundError(JenkinsRule r) throws Exception {
        URI serverUri = URI.create("https://example.atlassian.net/");
        DisposableHttpClient httpClient =
                new AsynchronousHttpClientFactory().createClient(serverUri, new AnonymousAuthenticationHandler());

        assertDoesNotThrow(() -> new AsynchronousJiraRestClient(serverUri, httpClient));
    }
}
