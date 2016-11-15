package hudson.plugins.jira.deprecated;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.plugins.jira.model.JiraIssue;
import hudson.plugins.jira.Messages;

import java.util.*;

/**
 * Old version of class JIRA issues related to the build.
 * Used only in junit test JiraBuildActionTest for
 * testing version transition compatibility of PR-72.
 *
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
            if (issue.getKey().equals(id)) {
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
