package hudson.plugins.jira;

import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.jira.model.JiraIssue;
import java.io.Serializable;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
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

  public JiraBuildAction(@Nonnull Set<JiraIssue> issues) {
    this.issues = new HashSet(issues);
  }

  @Override
  public void onAttached(Run<?, ?> r) {
    this.owner = r;
  }

  @Override
  public void onLoad(Run<?, ?> r) {
    this.owner = r;
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
