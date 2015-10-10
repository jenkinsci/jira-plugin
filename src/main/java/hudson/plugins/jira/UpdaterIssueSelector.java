package hudson.plugins.jira;

import java.util.Set;

import javax.annotation.Nonnull;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Strategy of finding issues which should be updated after completed run.
 *
 * @author Franta Mejta
 */
public abstract class UpdaterIssueSelector extends AbstractDescribableImpl<UpdaterIssueSelector> implements ExtensionPoint {

    /**
     * Finds the strings that match JIRA issue ID patterns.
     *
     * This method returns all likely candidates and shouldn't check
     * if such ID actually exists or not.
     *
     * @param run The completed run.
     * @param site Jira site configured for current job.
     * @param listener Current's run listener.
     * @return Set of ids of issues which should be updated.
     */
    public abstract Set<String> findIssueIds(@Nonnull Run<?, ?> run, @Nonnull JiraSite site, @Nonnull TaskListener listener);

}
