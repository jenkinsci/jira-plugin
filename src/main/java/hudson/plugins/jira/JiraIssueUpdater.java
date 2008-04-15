package hudson.plugins.jira;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * Parses build changelog for JIRA issue IDs and then
 * updates JIRA issues accordingly.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraIssueUpdater extends Publisher {
    public JiraIssueUpdater() {
    }

    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return Updater.perform(build, listener);
    }

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<Publisher> {
        private DescriptorImpl() {
            super(JiraIssueUpdater.class);
        }

        public String getDisplayName() {
            return Messages.JiraIssueUpdater_DisplayName();
        }

        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        public Publisher newInstance(StaplerRequest req) {
            return new JiraIssueUpdater();
        }
    }
}
