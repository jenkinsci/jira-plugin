package hudson.plugins.jira.selector;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

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
import hudson.plugins.jira.Messages;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class DefaultIssueSelector extends AbstractIssueSelector {

    private static final Logger LOGGER = Logger.getLogger(DefaultIssueSelector.class.getName());

    @DataBoundConstructor
    public DefaultIssueSelector() {
    }

    /**
     * See {@link #addIssuesRecursive(Run, JiraSite, TaskListener, Set)}
     */
    @Override
    public Set<String> findIssueIds(@Nonnull final Run<?, ?> run, @Nonnull final JiraSite site,
            @Nonnull final TaskListener listener) {
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
     * Finds the strings that match JIRA issue ID patterns. This method returns
     * all likely candidates and doesn't check if such ID actually exists or
     * not. We don't want to use {@link JiraSite#existsIssue(String)} here so
     * that new projects in JIRA can be detected.
     * 
     */
    protected static void findIssues(Run<?, ?> build, Set<String> issueIds, Pattern pattern, TaskListener listener) {
        for (ChangeLogSet<? extends Entry> set : RunScmChangeExtractor.getChanges(build)) {
            for (Entry change : set) {
                LOGGER.fine("Looking for JIRA ID in " + change.getMsg());
                Matcher m = pattern.matcher(change.getMsg());

                while (m.find()) {
                    if (m.groupCount() >= 1) {
                        String content = StringUtils.upperCase(m.group(1));
                        issueIds.add(content);
                    } else {
                        listener.getLogger()
                                .println("Warning: The JIRA pattern " + pattern + " doesn't define a capturing group!");
                    }
                }
            }
        }
    }

    /**
     * Calls {@link #findIssues(Run, Set, Pattern, TaskListener)} with
     * {@link JiraSite#getIssuePattern()} as pattern
     */
    protected void addIssuesFromChangeLog(Run<?, ?> build, JiraSite site, TaskListener listener, Set<String> issueIds) {
        Pattern pattern = site.getIssuePattern();
        findIssues(build, issueIds, pattern, listener);
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
     * Adds issues to issueIds from the current build. Issues from parameters
     * are added as well as issues matching pattern
     * {@link #addIssuesFromChangeLog(Run, JiraSite, TaskListener, Set)}
     * {@link #addIssuesFromParameters(Run, JiraSite, TaskListener, Set)}
     */
    protected void addIssuesFromCurrentBuild(Run<?, ?> build, JiraSite site, TaskListener listener,
            Set<String> issueIds) {
        addIssuesFromChangeLog(build, site, listener, issueIds);
        addIssuesFromParameters(build, site, listener, issueIds);
    }

    /**
     * Adds issues to issueIds by examining dependency changes from last build.
     * For each dependency change
     * {@link #addIssuesRecursive(Run, JiraSite, TaskListener, Set)} is called.
     */
    protected void addIssuesFromDependentBuilds(Run<?, ?> build, JiraSite site, TaskListener listener,
            Set<String> issueIds) {		
        Pattern pattern = site.getIssuePattern();

        for (DependencyChange depc : RunScmChangeExtractor.getDependencyChanges(build).values()) {
            for (AbstractBuild<?, ?> b : depc.getBuilds()) {
                getLogger().finer("Searching for JIRA issues in dependency " + b + " of " + build);

                // Fix JENKINS-44989
                // The original code before refactoring just called "findIssues", not "findIssueIdsRecursive"
                findIssues(b, issueIds, pattern, listener);
            }
        }
    }

    /**
     * Adds issues to issueIds from parameters
     */
    protected void addIssuesFromParameters(Run<?, ?> build, JiraSite site, TaskListener listener,
            Set<String> issueIds) {
        // Now look for any JiraIssueParameterValue's set in the build
        // Implements JENKINS-12312
        ParametersAction parameters = build.getAction(ParametersAction.class);

        if (parameters != null) {
            for (ParameterValue val : parameters.getParameters()) {
                if (val instanceof JiraIssueParameterValue) {
                    String issueId = ((JiraIssueParameterValue) val).getValue().toString();
                    if (issueIds.add(issueId)) {
                        getLogger().finer("Added perforce issue " + issueId + " from build " + build);
                    }
                }
            }
        }
    }

    /**
     * Adds issues that were carried over from previous build to issueIds
     */
    protected void addIssuesCarriedOverFromPreviousBuild(Run<?, ?> build, JiraSite site, TaskListener listener,
            Set<String> ids) {
        Run<?, ?> prev = build.getPreviousBuild();
        if (prev != null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if (a != null) {
                getLogger().finer("Searching for JIRA issues in previously failed build " + prev.number);
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
