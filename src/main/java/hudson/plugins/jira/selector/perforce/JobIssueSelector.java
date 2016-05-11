package hudson.plugins.jira.selector.perforce;

import java.util.Set;
import java.util.regex.Pattern;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.selector.DefaultIssueSelector;

/**
 * Base class for job selectors. Perforce offers mechanism to associate JIRA
 * issues with change lists called jobs. The classes inheriting from this class
 * find issues by examining jobs associated with changes
 * 
 * @author Jacek Tomaka
 * @since 2.3
 */
public abstract class JobIssueSelector extends DefaultIssueSelector {

    @Override
    /**
     * See {@link #addJobIdsFromChangeLog(Run, JiraSite, TaskListener, Set)}
     */
    protected void addIssuesFromChangeLog(Run<?, ?> build, Pattern pattern, TaskListener listener, Set<String> issueIds) {
        addJobIdsFromChangeLog(build, pattern, listener, issueIds);
    }

    /**
     * Adds job ids from change log to issueIds.
     */
    protected abstract void addJobIdsFromChangeLog(Run<?, ?> build, Pattern pattern, TaskListener listener,
            Set<String> issueIds);

}
