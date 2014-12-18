package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * Created by Reda on 18/12/2014.
 */
public class JiraReleaseVersionUpdaterBuilder extends Builder {

    private String jiraProjectKey;
    private String jiraRelease;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public JiraReleaseVersionUpdaterBuilder(String jiraProjectKey, String jiraRelease) {
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return VersionReleaser.perform(getSiteForProject(build.getProject()), jiraProjectKey, jiraRelease, build, listener);
    }

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private DescriptorImpl() {
            super(JiraReleaseVersionUpdaterBuilder.class);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            // Placed in the build settings section
            return Messages.JiraReleaseVersionBuilder_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        @Override
        public JiraReleaseVersionUpdaterBuilder newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return req.bindJSON(JiraReleaseVersionUpdaterBuilder.class, formData);
        }
    }
}
