package hudson.plugins.jira.selector.jobs.p4;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.jenkinsci.plugins.p4.changes.P4ChangeEntry;
import org.kohsuke.stapler.DataBoundConstructor;

import com.perforce.p4java.core.IFix;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.jobs.JobIssueSelector;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

@Extension(optional = true)
public class P4JobIssueSelector extends JobIssueSelector {
    private static final Logger LOGGER = Logger.getLogger(P4JobIssueSelector.class.getName());

    @Extension(optional = true)
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

        @Override
        public String getDisplayName() {
            return "Perforce Software job selector";
        }
    }

    @DataBoundConstructor
    public P4JobIssueSelector() {
    }

    protected void addJobIdsFromBuild(Run<?, ?> build, Set<String> issues, TaskListener listener) {
        LOGGER.finer("Searching for JIRA issues in perforce jobs in " + build);
        for (ChangeLogSet<? extends Entry> set : RunScmChangeExtractor.getChanges(build)) {
            for (Entry change : set) {
                LOGGER.fine("Looking for JIRA IDs as Perforce Jobs in " + change.getMsg());
                if (P4ChangeEntry.class.isAssignableFrom(change.getClass())) {
                    P4ChangeEntry p4ChangeEntry = (P4ChangeEntry) change;

                    List<IFix> jobs = p4ChangeEntry.getJobs();
                    if (jobs != null) {
                        for (IFix job : jobs) {
                            String jobId = job.getJobId();
                            if (issues.add(jobId)) {
                                LOGGER.finer("Added perforce job id " + jobId + " from build " + build);
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

}
