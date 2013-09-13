package hudson.plugins.jira;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
public class MockJiraSite extends JiraSite {
    public MockJiraSite() throws MalformedURLException {
        super(new URL("http://www.sun.com/"), null, null, null, false, false, null, false, "", "", false);
    }

    @Override
    public boolean existsIssue(String id) {
        return DEFAULT_ISSUE_PATTERN.matcher(id).matches();
    }
}
