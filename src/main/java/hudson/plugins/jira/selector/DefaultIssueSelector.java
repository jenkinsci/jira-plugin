package hudson.plugins.jira.selector;

import java.util.HashSet;
import java.util.Set;
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

public final class DefaultIssueSelector extends AbstractIssueSelector {

    private static final Logger LOGGER = Logger.getLogger(DefaultIssueSelector.class.getName());

    @DataBoundConstructor
    public DefaultIssueSelector() {
    }

    @Override
    public Set<String> findIssueIds(@Nonnull final Run<?, ?> run, @Nonnull final JiraSite site,
            @Nonnull final TaskListener listener) {
        return findIssueIdsRecursive(run, site.getIssuePattern(), listener);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

        @Override
        public String getDisplayName() {
            return Messages.DefaultUpdaterIssueSelector_DisplayName();
        }
    }

    /**
     * Finds the strings that match JIRA issue ID patterns. This method returns
     * all likely candidates and doesn't check if such ID actually exists or
     * not. We don't want to use {@link JiraSite#existsIssue(String)} here so
     * that new projects in JIRA can be detected.
     */
    private static Set<String> findIssueIdsRecursive(Run<?, ?> build, Pattern pattern, TaskListener listener) {
        Set<String> ids = new HashSet<String>();

        // first, issues that were carried forward.
        Run<?, ?> prev = build.getPreviousBuild();
        if (prev != null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if (a != null) {
                ids.addAll(a.getIDs());
            }
        }

        // then issues in this build
        findIssues(build, ids, pattern, listener);

        // check for issues fixed in dependencies
        for (DependencyChange depc : RunScmChangeExtractor.getDependencyChanges(build.getPreviousBuild()).values()) {
            for (AbstractBuild<?, ?> b : depc.getBuilds()) {
                findIssues(b, ids, pattern, listener);
            }
        }
        return ids;
    }

    /**
     * @param pattern pattern to use to match issue ids
     *            
     */
    protected static void findIssues(Run<?, ?> build, Set<String> ids, Pattern pattern, TaskListener listener) {
        for (ChangeLogSet<? extends Entry> set : RunScmChangeExtractor.getChanges(build)) {
            for (Entry change : set) {
                LOGGER.fine("Looking for JIRA ID in " + change.getMsg());
                Matcher m = pattern.matcher(change.getMsg());

                while (m.find()) {
                    if (m.groupCount() >= 1) {
                        String content = StringUtils.upperCase(m.group(1));
                        ids.add(content);
                    } else {
                        listener.getLogger()
                                .println("Warning: The JIRA pattern " + pattern + " doesn't define a capturing group!");
                    }
                }
            }
        }

        // Now look for any JiraIssueParameterValue's set in the build
        // Implements JENKINS-12312
        ParametersAction parameters = build.getAction(ParametersAction.class);

        if (parameters != null) {
            for (ParameterValue val : parameters.getParameters()) {
                if (val instanceof JiraIssueParameterValue) {
                    ids.add(((JiraIssueParameterValue) val).getValue().toString());
                }
            }
        }
    }

}
