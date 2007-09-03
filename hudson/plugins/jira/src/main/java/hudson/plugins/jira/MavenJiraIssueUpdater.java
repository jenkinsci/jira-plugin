package hudson.plugins.jira;

import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.model.BuildListener;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class MavenJiraIssueUpdater extends MavenReporter {

    @Override
    public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return Updater.perform(build, listener);
    }

    public MavenReporterDescriptor getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private DescriptorImpl() {
            super(MavenJiraIssueUpdater.class);
        }

        public String getDisplayName() {
            return "Updated relevant JIRA issues";
        }

        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        public MavenJiraIssueUpdater newInstance(StaplerRequest req) throws FormException {
            return new MavenJiraIssueUpdater();
        }
    }
}
