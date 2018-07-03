package hudson.plugins.jira;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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

/**
 * A build step which creates new JIRA version
 *
 * @author Artem Koshelev artkoshelev@gmail.com
 * @deprecated Replaced by {@link JiraVersionCreatorBuilder}. Read its description to see why.
 * 	Kept for backward compatibility.
 */
@Deprecated
public class JiraVersionCreator extends Notifier {
    private String jiraVersion;
    private String jiraProjectKey;

    @DataBoundConstructor
    public JiraVersionCreator(String jiraVersion, String jiraProjectKey) {
        this.jiraVersion = jiraVersion;
        this.jiraProjectKey = jiraProjectKey;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        return new VersionCreator().perform(build.getProject(), jiraVersion, jiraProjectKey, build, listener);
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
        public JiraVersionCreator newInstance(StaplerRequest req, JSONObject formData) throws FormException {
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
