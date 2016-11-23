package hudson.plugins.jira;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.Sets;
import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.jira.model.JiraIssue;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;

/**
 * JIRA issues related to the build.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class JiraBuildAction implements Action {

    public final Run<?, ?> owner;

    private Set<JiraIssue> issues;

    public JiraBuildAction(@Nonnull Run<?, ?> owner, @Nonnull Set<JiraIssue> issues) {
        this.owner = owner;
        this.issues = Sets.newHashSet(issues);
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

    @Exported
    public Iterable<JiraIssue> getIssues() {
        return issues;
    }

    /**
     * Finds {@link JiraIssue} whose ID matches the given one.
     * @param issueID e.g. JENKINS-1234
     * @return JIRAIssue representing the issueID
     */
    public JiraIssue getIssue(String issueID) {
        for (JiraIssue issue : issues) {
            if (issue.getId().equals(issueID)) {
                return issue;
            }
        }
        return null;
    }

    public void addIssues(Set<JiraIssue> issuesToBeSaved) {
        this.issues.addAll(issuesToBeSaved);
    }
}
