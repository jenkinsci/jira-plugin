package hudson.plugins.jira;

import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.plugins.jira.JiraSite.ExtendedAsynchronousJiraRestClientFactory;
import hudson.plugins.jira.auth.BearerHttpAuthenticationHandler;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import java.net.URI;

/**
 * Jira Session factory implementation
 *
 * @author Elia Bracci
 */
public class JiraSessionFactory {

    /**
     * This method takes as parameters the JiraSite class, the jira URI and
     * credentials and returns a JiraSession with Basic authentication if
     * useBearerAuth is set to false, otherwise it returns a JiraSession with Bearer
     * authentication if useBearerAuth is set to true.
     *
     * @param jiraSite    jiraSite class
     * @param uri         jira uri
     * @param credentials Jenkins credentials
     * @return JiraSession instance
     */
    public static JiraSession create(JiraSite jiraSite, URI uri, StandardUsernamePasswordCredentials credentials) {
        ExtendedJiraRestClient jiraRestClient;
        JiraRestService jiraRestService;

        if (jiraSite.isUseBearerAuth()) {
            BearerHttpAuthenticationHandler bearerHttpAuthenticationHandler = new BearerHttpAuthenticationHandler(
                    credentials.getPassword().getPlainText());

            jiraRestClient = new ExtendedAsynchronousJiraRestClientFactory()
                    .create(uri, bearerHttpAuthenticationHandler, jiraSite.getHttpClientOptions());

            jiraRestService = new JiraRestService(
                    uri, jiraRestClient, credentials.getPassword().getPlainText(), jiraSite.getReadTimeout());
        } else {
            jiraRestClient = new ExtendedAsynchronousJiraRestClientFactory()
                    .create(
                            uri,
                            new BasicHttpAuthenticationHandler(
                                    credentials.getUsername(),
                                    credentials.getPassword().getPlainText()),
                            jiraSite.getHttpClientOptions());

            jiraRestService = new JiraRestService(
                    uri,
                    jiraRestClient,
                    credentials.getUsername(),
                    credentials.getPassword().getPlainText(),
                    jiraSite.getReadTimeout());
        }

        return new JiraSession(jiraSite, jiraRestService);
    }
}
