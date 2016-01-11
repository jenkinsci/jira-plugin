package hudson.plugins.jira.deprecated;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.plugins.jira.JiraIssue;
import hudson.plugins.jira.Messages;

import java.util.*;

/**
 * JIRA issues related to the build.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeprecatedJiraBuildAction implements Action {
    public final AbstractBuild<?, ?> owner;

    public JiraIssue[] issues;

    public DeprecatedJiraBuildAction(AbstractBuild<?, ?> owner, Collection<JiraIssue> issues) {
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
