package hudson.plugins.jira;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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
        this.issues = new HashSet( issues);
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

    @Exported(inline = true)
    public Set<JiraIssue> getIssues() {
        return issues;
    }

    @Exported
    public String getServerURL() {
        JiraSite jiraSite = JiraSite.get(owner.getParent());
        URL url = jiraSite != null ? jiraSite.getUrl() : null;
        return url != null ? url.toString() : null;
    }

    /**
     * Finds {@link JiraIssue} whose ID matches the given one.
     * @param issueID e.g. JENKINS-1234
     * @return JIRAIssue representing the issueID
     */
    public JiraIssue getIssue(String issueID) {
        for (JiraIssue issue : issues) {
            if (issue.getKey().equals(issueID)) {
                return issue;
            }
        }
        return null;
    }

    public void addIssues(Set<JiraIssue> issuesToBeSaved) {
        this.issues.addAll(issuesToBeSaved);
    }
}
