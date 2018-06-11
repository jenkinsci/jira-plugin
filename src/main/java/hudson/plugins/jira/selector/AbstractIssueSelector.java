package hudson.plugins.jira.selector;

import java.util.Set;

import javax.annotation.Nonnull;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;

/**
 * Strategy of finding issues which should be updated after completed run.
 *
 * @author Franta Mejta
 */
public abstract class AbstractIssueSelector extends AbstractDescribableImpl<AbstractIssueSelector> implements ExtensionPoint {

    /**
     * Finds the strings that match JIRA issue ID patterns.
     *
     * This method returns all likely candidates and shouldn't check
     * if such ID actually exists or not.
     *
     * @param run The completed run.
     * @param site JIRA site configured for current job.
     * @param listener Current's run listener.
     * @return Set of ids of issues which should be updated.
     */
    public abstract Set<String> findIssueIds(@Nonnull Run<?, ?> run, @Nonnull JiraSite site, @Nonnull TaskListener listener);

}
