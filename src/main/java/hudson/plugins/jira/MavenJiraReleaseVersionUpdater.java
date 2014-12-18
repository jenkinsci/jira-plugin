package hudson.plugins.jira;

import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * Created by Reda on 18/12/2014.
 */
public class MavenJiraReleaseVersionUpdater extends MavenReporter {

    private String jiraProjectKey;
    private String jiraRelease;

    @DataBoundConstructor
    public MavenJiraReleaseVersionUpdater(String jiraProjectKey, String jiraRelease) {
        this.jiraRelease = jiraRelease;
        this.jiraProjectKey = jiraProjectKey;
    }

    public String getJiraRelease() {
        return jiraRelease;
    }

    public void setJiraRelease(String jiraRelease) {
        this.jiraRelease = jiraRelease;
    }

    public String getJiraProjectKey() {
        return jiraProjectKey;
    }

    public void setJiraProjectKey(String jiraProjectKey) {
        this.jiraProjectKey = jiraProjectKey;
    }

    @Override
    public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return Releaser.perform(getSiteForProject(build.getProject()), jiraProjectKey, jiraRelease, build, listener);
    }

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }

    @Override
    public MavenReporterDescriptor getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private DescriptorImpl() {
            super(MavenJiraReleaseVersionUpdater.class);
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
        public MavenJiraReleaseVersionUpdater newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return req.bindJSON(MavenJiraReleaseVersionUpdater.class, formData);
        }
    }
}
