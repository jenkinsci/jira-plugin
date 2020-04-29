package hudson.plugins.jira.selector.perforce;

import com.perforce.p4java.core.IFix;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jenkinsci.plugins.p4.changes.P4ChangeEntry;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Job selector for Perforce Software SCM plugin (P4)
 *
 * @author Jacek Tomaka
 * @since 2.3
 */
public class P4JobIssueSelector extends JobIssueSelector {

  private static final Logger LOGGER = Logger.getLogger(P4JobIssueSelector.class.getName());

  @DataBoundConstructor
  public P4JobIssueSelector() {
  }

  @Override
  protected void addJobIdsFromChangeLog(Run<?, ?> build, JiraSite site, TaskListener listener,
      Set<String> issueIds) {
    getLogger().finer("Searching for Jira issues in Perforce jobs in " + build);
    for (ChangeLogSet<? extends Entry> set : RunScmChangeExtractor.getChanges(build)) {
      for (Entry change : set) {
        getLogger().fine("Looking for Jira IDs as Perforce Jobs in " + change.getMsg());
        if (P4ChangeEntry.class.isAssignableFrom(change.getClass())) {
          P4ChangeEntry p4ChangeEntry = (P4ChangeEntry) change;

          List<IFix> jobs = p4ChangeEntry.getJobs();
          if (jobs != null) {
            for (IFix job : jobs) {
              String jobId = job.getJobId();
              if (issueIds.add(jobId)) {
                getLogger().finer(
                    "Added Perforce job id " + jobId + " from build " + build);
              }
            }
          }
        }
      }
    }
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Extension(optional = true)
  public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

    @Override
    public String getDisplayName() {
      return Messages.P4JobIssueSelector_DisplayName();
    }
  }

}
