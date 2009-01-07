package hudson.plugins.jira;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * @author Kohsuke Kawaguchi
 */
public class MockJiraSite extends JiraSite {
    public MockJiraSite() throws MalformedURLException {
        super(new URL("http://www.sun.com/"),null,null,false);
    }

    @Override
    public boolean existsIssue(String id) {
        return Updater.ISSUE_PATTERN.matcher(id).matches();
    }
}
