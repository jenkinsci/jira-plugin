package hudson.plugins.jira;

import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * {@link MavenReporter} for JIRA.
 * Modern plugins don't have to do this &mdash; they should simply have
 * {@link JiraIssueUpdater} and have its descriptor extend from {@link BuildStepDescriptor},
 * and you can get rid of this class altogether.
 * In case of the JIRA plugin, however, this is left for a compatibility reason.
 *
 * @author Kohsuke Kawaguchi
 */
public class MavenJiraIssueUpdater extends MavenReporter {

    private static final long serialVersionUID = -3416800198673836204L;

    @Override
    public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return Updater.perform(build, listener);
    }

    @Override
    public MavenReporterDescriptor getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private DescriptorImpl() {
            super(MavenJiraIssueUpdater.class);
        }

        @Override
        public String getDisplayName() {
            // Placed in the build settings section
            return Messages.JiraIssueUpdater_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        @Override
        public MavenJiraIssueUpdater newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return new MavenJiraIssueUpdater();
        }
    }
}
