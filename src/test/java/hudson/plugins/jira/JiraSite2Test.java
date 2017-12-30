package hudson.plugins.jira;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URL;

public class JiraSite2Test {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testClientInitialization() throws Exception {
        JiraSite site = new JiraSite(new URL("https://nonexistent.url"), null,
                "user", "password",
                false, false,
                null, false, null,
                null, true);
        site.setTimeout(1);
        site.createSession();
    }
}
