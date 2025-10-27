package hudson.plugins.jira.selector;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraCarryOverAction;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import hudson.plugins.jira.RunScmChangeExtractor;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.kohsuke.stapler.DataBoundConstructor;

public class DefaultIssueSelector extends AbstractIssueSelector {

    private static final Logger LOGGER = Logger.getLogger(DefaultIssueSelector.class.getName());

    @DataBoundConstructor
    public DefaultIssueSelector() {}

    /**
     * See {@link #addIssuesRecursive(Run, JiraSite, TaskListener, Set)}
     */
    @Override
    public Set<String> findIssueIds(
            @NonNull final Run<?, ?> run, @NonNull final JiraSite site, @NonNull final TaskListener listener) {
        HashSet<String> issuesIds = new LinkedHashSet<>();
        addIssuesRecursive(run, site, listener, issuesIds);
        return issuesIds;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

        @Override
        public String getDisplayName() {
            return Messages.DefaultIssueSelector_DisplayName();
        }
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Adds issues to issueIds. Adds issues carried over from previous build,
     * issues from current build and from dependent builds
     * {@link #addIssuesCarriedOverFromPreviousBuild(Run, JiraSite, TaskListener, Set)}
     * {@link #addIssuesFromCurrentBuild(Run, JiraSite, TaskListener, Set)}
     * {@link #addIssuesFromDependentBuilds(Run, JiraSite, TaskListener, Set)}
     */
    protected void addIssuesRecursive(Run<?, ?> build, JiraSite site, TaskListener listener, Set<String> issuesIds) {
        addIssuesCarriedOverFromPreviousBuild(build, site, listener, issuesIds);
        addIssuesFromCurrentBuild(build, site, listener, issuesIds);
        addIssuesFromDependentBuilds(build, site, listener, issuesIds);
    }

    /**
     * Adds issues to issueIds by examining dependency changes from last build.
     * For each dependency change
     * {@link #addIssuesRecursive(Run, JiraSite, TaskListener, Set)} is called.
     */
    protected void addIssuesFromDependentBuilds(
            Run<?, ?> build, JiraSite site, TaskListener listener, Set<String> issueIds) {
        Pattern pattern = site.getIssuePattern();

        for (DependencyChange depc :
                RunScmChangeExtractor.getDependencyChanges(build).values()) {
            for (AbstractBuild<?, ?> b : depc.getBuilds()) {
                getLogger().finer("Searching for Jira issues in dependency " + b + " of " + build);

                // Fix JENKINS-44989
                // The original code before refactoring just called "findIssues", not "findIssueIdsRecursive"
                findIssues(b, issueIds, pattern, listener);
            }
        }
    }

    /**
     * Adds issues that were carried over from previous build to issueIds
     */
    protected void addIssuesCarriedOverFromPreviousBuild(
            Run<?, ?> build, JiraSite site, TaskListener listener, Set<String> ids) {
        Run<?, ?> prev = build.getPreviousCompletedBuild();
        if (prev != null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if (a != null) {
                getLogger().finer("Searching for Jira issues in previously failed build " + prev.number);
                Collection<String> jobIDs = a.getIDs();
                ids.addAll(jobIDs);
                if (getLogger().isLoggable(Level.FINER)) {
                    for (String jobId : a.getIDs()) {
                        getLogger().finer("Adding job " + jobId);
                    }
                }
            }
        }
    }
}
