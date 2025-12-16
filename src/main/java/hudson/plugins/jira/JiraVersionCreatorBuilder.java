package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * A build step which creates new Jira version. It has the same functionality as
 * {@link JiraVersionCreator} but can be used multiple times in the same build
 * (e.g. for different projects) and supports conditional triggering.
 *
 * @author marcin.czerwinski marcinczrw@gmail.com
 */
public class JiraVersionCreatorBuilder extends Builder implements SimpleBuildStep {

    private String jiraVersion;
    private String jiraProjectKey;
    private boolean failIfAlreadyExists = true;

    @DataBoundConstructor
    public JiraVersionCreatorBuilder(String jiraVersion, String jiraProjectKey) {
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
    public void perform(Run<?, ?> run, EnvVars env, TaskListener listener) {
        VersionCreator versionCreator = new VersionCreator();
        versionCreator.setFailIfAlreadyExists(failIfAlreadyExists);
        versionCreator.perform(run.getParent(), jiraVersion, jiraProjectKey, run, listener);
    }

    @Override
    public boolean requiresWorkspace() {
        return false;
    }

    @Override
    public BuildStepDescriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Symbol("jiraCreateVersion")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            super(JiraVersionCreatorBuilder.class);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public JiraVersionCreatorBuilder newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            return req.bindJSON(JiraVersionCreatorBuilder.class, formData);
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraVersionCreatorBuilder_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help-version-create.html";
        }
    }
}
