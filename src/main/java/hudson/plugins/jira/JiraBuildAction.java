package hudson.plugins.jira;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.jira.model.JiraIssue;

/**
 * JIRA issues related to the build.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraBuildAction implements Action {

    public final Run<?, ?> owner;

    public JiraIssue[] issues;

    public JiraBuildAction(Run<?, ?> owner, Collection<JiraIssue> issues) {
        this.owner = owner;
        this.issues = issues.toArray(new JiraIssue[issues.size()]);
        Arrays.sort(this.issues);
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return Messages.JiraBuildAction_DisplayName();
    }

    public String getUrlName() {
        return "jira";
    }

    /**
     * Finds {@link JiraIssue} whose ID matches the given one.
     */
    public JiraIssue getIssue(String id) {
        for (JiraIssue issue : issues) {
            if (issue.id.equals(id)) {
                return issue;
            }
        }
        return null;
    }

    public void addIssues(Set<JiraIssue> issuesToBeSaved) {
        SortedSet<JiraIssue> allIssues = new TreeSet<JiraIssue>();
        allIssues.addAll(issuesToBeSaved);
        allIssues.addAll(Arrays.asList(this.issues));

        this.issues = allIssues.toArray(new JiraIssue[allIssues.size()]);
    }
}
