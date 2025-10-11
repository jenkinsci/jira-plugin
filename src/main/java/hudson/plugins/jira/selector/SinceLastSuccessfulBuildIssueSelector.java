package hudson.plugins.jira.selector;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import java.util.LinkedHashSet;
import java.util.Set;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Selects JIRA issues from the current build and all builds
 * since the last successful one.
 */
public class SinceLastSuccessfulBuildIssueSelector extends AbstractIssueSelector {

    @DataBoundConstructor
    public SinceLastSuccessfulBuildIssueSelector() {}

    @Override
    public Set<String> findIssueIds(
            @NonNull final Run<?, ?> run, @NonNull final JiraSite site, @NonNull final TaskListener listener) {
        Set<String> issueIds = new LinkedHashSet<>();

        Run<?, ?> lastSuccessfulBuild = run.getPreviousSuccessfulBuild();

        if (lastSuccessfulBuild == null) {
            listener.getLogger().println("No previous successful build found. Searching only in current build.");
            addIssuesFromCurrentBuild(run, site, listener, issueIds);
            return issueIds;
        }

        listener.getLogger()
                .println("Collecting JIRA issues since last successful build #" + lastSuccessfulBuild.getNumber());

        Run<?, ?> build = run;
        int buildsProcessed = 0;

        while (build != null && build != lastSuccessfulBuild) {
            addIssuesFromCurrentBuild(build, site, listener, issueIds);

            buildsProcessed++;
            build = build.getPreviousBuild();
        }

        listener.getLogger()
                .println("Found " + issueIds.size() + " JIRA issue(s) across " + buildsProcessed + " build(s)");

        return issueIds;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

        @Override
        public String getDisplayName() {
            return Messages.SinceLastSuccessfulBuildIssueSelector_DisplayName();
        }
    }
}
