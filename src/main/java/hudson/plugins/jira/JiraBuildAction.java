package hudson.plugins.jira;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import hudson.plugins.jira.model.JiraIssue;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Jira issues related to the build.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class JiraBuildAction implements RunAction2 {

    private final HashSet<JiraIssue> issues;
    private transient Run<?, ?> owner;

    public JiraBuildAction(@NonNull Set<JiraIssue> issues) {
        this.issues = new HashSet<>(issues);
    }

    // Leave it in place for binary compatibility.
    /**
     * @param owner the owner of this action
     * @param issues the Jira issues
     *
     * @deprecated use {@link #JiraBuildAction(java.util.Set)} instead
     */
    @Deprecated
    public JiraBuildAction(Run<?, ?> owner, @NonNull Set<JiraIssue> issues) {
        this(issues);
        // the owner will be set by #onAttached(hudson.model.Run)
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        this.owner = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.owner = r;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Messages.JiraBuildAction_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "jira";
    }

    public Run<?, ?> getOwner() {
        return owner;
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
     *
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
