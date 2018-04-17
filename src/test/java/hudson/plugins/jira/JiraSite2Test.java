package hudson.plugins.jira;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class JiraSite2Test {

    private static final String ANY_USER = "Kohsuke";
    private static final String ANY_PASSWORD = "Kawaguchi";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private URL nonExistentUrl;

    @Before
    public void init() throws MalformedURLException {
        nonExistentUrl = new URL("https://nonexistent.url");
    }

    @Test
    public void createSessionWithProvidedCredentials() {
        JiraSite site = new JiraSite(nonExistentUrl, null,
                (String)null,
                false, false,
                null, false, null,
                null, true);
        site.setTimeout(1);
        JiraSession session = site.createSession();
        assertThat(session, notNullValue());
    }

    @Test
    public void createSessionReturnsNullIfUsernameIsNull() {
        JiraSite site = new JiraSite(nonExistentUrl, null,
                null, ANY_PASSWORD,
                false, false,
                null, false, null,
                null, true);
        site.setTimeout(1);
        JiraSession session = site.createSession();
        assertThat(session, nullValue());
    }

}
