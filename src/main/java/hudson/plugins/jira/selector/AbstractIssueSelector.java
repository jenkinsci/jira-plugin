package hudson.plugins.jira.selector;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Strategy of finding issues which should be updated after completed run.
 *
 * @author Franta Mejta
 */
public abstract class AbstractIssueSelector extends AbstractDescribableImpl<AbstractIssueSelector>
        implements ExtensionPoint {

    private static final Logger BASE_LOGGER = Logger.getLogger(AbstractIssueSelector.class.getName());

    /**
     * Finds the strings that match Jira issue ID patterns.
     *
     * This method returns all likely candidates and shouldn't check
     * if such ID actually exists or not.
     *
     * @param run The completed run.
     * @param site Jira site configured for current job.
     * @param listener Current's run listener.
     * @return Set of ids of issues which should be updated.
     */
    public abstract Set<String> findIssueIds(
            @NonNull Run<?, ?> run, @NonNull JiraSite site, @NonNull TaskListener listener);

    /**
     * Adds issues to issueIds from parameters
     */
    protected void addIssuesFromParameters(
            Run<?, ?> build, JiraSite site, TaskListener listener, Set<String> issueIds) {
        // Now look for any JiraIssueParameterValue's set in the build
        // Implements JENKINS-12312
        ParametersAction parameters = build.getAction(ParametersAction.class);

        if (parameters != null) {
            for (ParameterValue val : parameters.getParameters()) {
                if (val instanceof JiraIssueParameterValue) {
                    String issueId = ((JiraIssueParameterValue) val).getValue().toString();
                    if (issueIds.add(issueId)) {
                        BASE_LOGGER.finer("Added perforce issue " + issueId + " from build " + build);
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
     * Adds issues to issueIds from the current build. Issues from parameters
     * are added as well as issues matching pattern
     * {@link #addIssuesFromChangeLog(Run, JiraSite, TaskListener, Set)}
     * {@link #addIssuesFromParameters(Run, JiraSite, TaskListener, Set)}
     */
    protected void addIssuesFromCurrentBuild(
            Run<?, ?> build, JiraSite site, TaskListener listener, Set<String> issueIds) {
        addIssuesFromChangeLog(build, site, listener, issueIds);
        addIssuesFromParameters(build, site, listener, issueIds);
    }

    /**
     * Finds the strings that match Jira issue ID patterns. This method returns
     * all likely candidates and doesn't check if such ID actually exists or
     * not. We don't want to use {@link JiraSite#existsIssue(String)} here so
     * that new projects in Jira can be detected.
     *
     */
    protected static void findIssues(Run<?, ?> build, Set<String> issueIds, Pattern pattern, TaskListener listener) {
        for (ChangeLogSet<? extends Entry> set : RunScmChangeExtractor.getChanges(build)) {
            for (Entry change : set) {
                BASE_LOGGER.fine("Looking for Jira ID in " + change.getMsg());
                Matcher m = pattern.matcher(change.getMsg());

                while (m.find()) {
                    if (m.groupCount() >= 1) {
                        String content = StringUtils.upperCase(m.group(1));
                        issueIds.add(content);
                    } else {
                        listener.getLogger()
                                .println("Warning: The Jira pattern " + pattern + " doesn't define a capturing group!");
                    }
                }
            }
        }
    }
}
