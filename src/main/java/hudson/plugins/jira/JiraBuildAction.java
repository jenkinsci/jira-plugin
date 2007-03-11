package hudson.plugins.jira;

import hudson.model.Action;
import hudson.model.Build;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Report issues related to the build.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraBuildAction implements Action {
    public final Build owner;

    public final JiraIssue[] issues;

    public JiraBuildAction(Build owner, Collection<JiraIssue> issues) {
        this.owner = owner;
        this.issues = issues.toArray(new JiraIssue[issues.size()]);
        Arrays.sort(this.issues);
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "JIRA issues";
    }

    public String getUrlName() {
        return "jira";
    }

    /**
     * Finds {@link JiraIssue} whose ID matches the given one.
     */
    public JiraIssue getIssue(String id) {
        for (JiraIssue issue : issues) {
            if(issue.id.equals(id))
                return issue;
        }
        return null;
    }
}
