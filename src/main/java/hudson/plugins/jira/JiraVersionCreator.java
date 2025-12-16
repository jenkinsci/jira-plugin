package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * A build step which creates new Jira version
 *
 * @author Artem Koshelev artkoshelev@gmail.com
 * @deprecated Replaced by {@link JiraVersionCreatorBuilder}. Read its description to see why.
 * Kept for backward compatibility.
 */
@Deprecated
public class JiraVersionCreator extends Notifier {
    private String jiraVersion;
    private String jiraProjectKey;
    private boolean failIfAlreadyExists = true;

    @DataBoundConstructor
    public JiraVersionCreator(String jiraVersion, String jiraProjectKey) {
        this.jiraVersion = jiraVersion;
        this.jiraProjectKey = jiraProjectKey;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getJiraVersion() {
        return jiraVersion;
    }

    public void setJiraVersion(String jiraVersion) {
        this.jiraVersion = jiraVersion;
    }

    public String getJiraProjectKey() {
        return jiraProjectKey;
    }

    public void setJiraProjectKey(String jiraProjectKey) {
        this.jiraProjectKey = jiraProjectKey;
    }

    public boolean isFailIfAlreadyExists() {
        return failIfAlreadyExists;
    }

    @DataBoundSetter
    public void setFailIfAlreadyExists(boolean failIfAlreadyExists) {
        this.failIfAlreadyExists = failIfAlreadyExists;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        VersionCreator versionCreator = new VersionCreator();
        versionCreator.setFailIfAlreadyExists(failIfAlreadyExists);
        return versionCreator.perform(build.getProject(), jiraVersion, jiraProjectKey, build, listener);
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(JiraVersionCreator.class);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public JiraVersionCreator newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            return req.bindJSON(JiraVersionCreator.class, formData);
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraVersionCreator_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help-version-create.html";
        }
    }
}
