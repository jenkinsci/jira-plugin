package hudson.plugins.jira;

import java.util.Set;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;

public final class DefaultUpdaterIssueSelector extends UpdaterIssueSelector {

    @DataBoundConstructor
    public DefaultUpdaterIssueSelector() {
    }

    @Override
    public Set<String> findIssueIds(@Nonnull final Run<?, ?> run, @Nonnull final JiraSite site, @Nonnull final TaskListener listener) {
        return Updater.findIssueIdsRecursive(run, site.getIssuePattern(), listener);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<UpdaterIssueSelector> {

        @Override
        public String getDisplayName() {
            return Messages.DefaultUpdaterIssueSelector_DisplayName();
        }
    }
}
