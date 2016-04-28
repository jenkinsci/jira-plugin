package hudson.plugins.jira.selector.jobs.perforce;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import com.tek42.perforce.model.Changelist;
import com.tek42.perforce.model.Changelist.JobEntry;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.jobs.JobIssueSelector;
import hudson.plugins.perforce.PerforceChangeLogEntry;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

@Extension(optional = true)
public class PerforceJobIssueSelector extends JobIssueSelector {
    private static final Logger LOGGER = Logger.getLogger(PerforceJobIssueSelector.class.getName());

    @Extension(optional = true)
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

        @Override
        public String getDisplayName() {
            return "Perforce job selector";
        }
    }

    @DataBoundConstructor
    public PerforceJobIssueSelector() {
    }

    @Override
    protected void addJobIdsFromBuild(Run<?, ?> build, Set<String> issues, TaskListener listener) {
        LOGGER.finer("Searching for JIRA issues in perforce jobs in " + build);
        for (ChangeLogSet<? extends Entry> set : RunScmChangeExtractor.getChanges(build)) {
            for (Entry change : set) {
                LOGGER.fine("Looking for JIRA IDs as Perforce Jobs in " + change.getMsg());
                if (PerforceChangeLogEntry.class.isAssignableFrom(change.getClass())) {
                    PerforceChangeLogEntry perforceChangeLogEntry = (PerforceChangeLogEntry) change;
                    Changelist changeList = perforceChangeLogEntry.getChange();
                    if (changeList != null) {
                        List<JobEntry> jobs = changeList.getJobs();
                        if (jobs != null) {
                            for (JobEntry job : jobs) {
                                String jobId = job.getJob();
                                if (issues.add(jobId)) {
                                    LOGGER.finer("Added perforce job id " + jobId + " from build " + build);
                                }
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
