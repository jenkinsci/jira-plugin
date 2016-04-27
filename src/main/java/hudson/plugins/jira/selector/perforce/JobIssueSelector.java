package hudson.plugins.jira.selector.perforce;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import com.tek42.perforce.model.Changelist;
import com.tek42.perforce.model.Changelist.JobEntry;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.plugins.jira.JiraCarryOverAction;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.plugins.perforce.PerforceChangeLogEntry;

@Extension(optional = true)
public class JobIssueSelector extends AbstractIssueSelector {
    private static final Logger LOGGER = Logger.getLogger(JobIssueSelector.class.getName());

    @Extension(optional = true)
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

        @Override
        public String getDisplayName() {
            return "Perforce job selector";
        }
    }

    @DataBoundConstructor
    public JobIssueSelector() {
    }

    @Override
    public Set<String> findIssueIds(Run<?, ?> build, JiraSite site, TaskListener listener) {
        Set<String> ids = new HashSet<String>();
        addIssuesRecursive(build, ids, listener);
        printInfoAboutFoundIssues(ids, listener);
        return ids;
    }

    private void printInfoAboutFoundIssues(Set<String> ids, TaskListener listener) {
       if (ids.isEmpty()){
           listener.getLogger().println("JIRA: Found no JIRA issues.");
       }else{
           listener.getLogger().print("JIRA: The following JIRA issues will be updated: ");
           Iterator<String> it = ids.iterator();
           while(it.hasNext()){
               listener.getLogger().print(it.next());
               if (it.hasNext()){
                   listener.getLogger().print(",");
               }
           }
           listener.getLogger().println(".");
       }
    }

    protected static void addIssuesFromCurrentBuild(Run<?, ?> build, Set<String> issues, TaskListener listener) {
        addJobIdsFromBuild(build, issues, listener);
    }

    private static void addIssuesRecursive(Run<?, ?> build, Set<String> ids, TaskListener listener) {
        addIssuesFromPreviousFailedBuild(build, ids);
        addJobIdsFromBuild(build, ids, listener);
        addIssuesFromDependentBuilds(build, ids, listener);
    }

    protected static void addJobIdsFromBuild(Run<?, ?> build, Set<String> issues, TaskListener listener) {
        LOGGER.finer("Searching for JIRA issues in perforce jobs in " +build);
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
                                    LOGGER.finer("Added perforce job id " + jobId+" from build "+build);
                                }
                            }
                        }
                    }
                }
            }
        }
        addIssuesFromParameters(build, issues);
    }

    protected static void addIssuesFromParameters(Run<?, ?> build, Set<String> issues) {
        // Now look for any JiraIssueParameterValue's set in the build
        // Implements JENKINS-12312
        ParametersAction parameters = build.getAction(ParametersAction.class);

        if (parameters != null) {
            for (ParameterValue val : parameters.getParameters()) {
                if (val instanceof JiraIssueParameterValue) {
                    String issueId = ((JiraIssueParameterValue) val).getValue().toString();
                    LOGGER.finer("Added perforce issue " + issueId+" from build "+build);
                    issues.add(issueId);
                }
            }
        }
    }

    protected static void addIssuesFromDependentBuilds(Run<?, ?> build, Set<String> ids, TaskListener listener) {
        
        for (DependencyChange depc : RunScmChangeExtractor.getDependencyChanges(build.getPreviousBuild()).values()) {
            for (AbstractBuild<?, ?> b : depc.getBuilds()) {
                LOGGER.finer("Searching for JIRA issues in dependency " +b +" of " +build);
                addIssuesRecursive(b, ids, listener);
            }
        }
    }

    protected static void addIssuesFromPreviousFailedBuild(Run<?, ?> build, Set<String> ids) {
        Run<?, ?> prev = build.getPreviousBuild();
        if (prev != null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if (a != null) {
                LOGGER.finer("Searching for JIRA issues in previously failed build " + prev.number);
                Collection<String> jobIDs = a.getIDs();
                ids.addAll(jobIDs);
                if (LOGGER.isLoggable(Level.FINER)) {
                    for (String jobId : a.getIDs()) {
                        LOGGER.finer("Adding job " + jobId);
                    }
                }
            }
        }
    }

}
