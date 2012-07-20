package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Parses build changelog for JIRA issue IDs and then
 * updates JIRA issues accordingly.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraIssueUpdater extends Recorder implements MatrixAggregatable {
    public JiraIssueUpdater() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // Don't do anything for individual matrix runs.
        if (build instanceof MatrixRun) {
            return true;
        }

        return Updater.perform(build, listener);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                PrintStream logger = listener.getLogger();
                logger.println("End of Matrix Build. Updating JIRA.");
                return Updater.perform(this.build, this.listener);
            }
        };
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private DescriptorImpl() {
            super(JiraIssueUpdater.class);
        }

        @Override
		public String getDisplayName() {
            // Displayed in the publisher section
            return Messages.JiraIssueUpdater_DisplayName();
           
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) {
            return new JiraIssueUpdater();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
