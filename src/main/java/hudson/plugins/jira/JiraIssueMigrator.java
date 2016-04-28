package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class JiraIssueMigrator extends Notifier {

    private static final long serialVersionUID = 6909671291180081586L;

    private String jiraProjectKey;
    private String jiraRelease;
    private String jiraReplaceVersion;
    private String jiraQuery;
    private boolean addRelease;

    @DataBoundConstructor
    public JiraIssueMigrator(String jiraProjectKey, String jiraRelease, String jiraQuery, String jiraReplaceVersion, boolean addRelease) {
        this.jiraRelease = jiraRelease;
        this.jiraProjectKey = jiraProjectKey;
        this.jiraQuery = jiraQuery;
        this.jiraReplaceVersion = jiraReplaceVersion;
        this.addRelease = addRelease;
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

    public String getJiraQuery() {
        return jiraQuery;
    }

    public void setJiraQuery(String jiraQuery) {
        this.jiraQuery = jiraQuery;
    }

    public String getJiraReplaceVersion() {
        return jiraReplaceVersion;
    }

    public void setJiraReplaceVersion(String jiraReplaceVersion) {
        this.jiraReplaceVersion = jiraReplaceVersion;
    }

    public boolean isAddRelease() { return addRelease; }

    public void setAddRelease(boolean addRelease) { this.addRelease = addRelease; }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) {
        String realRelease = null;
        String realReplace = null;
        String realQuery = "";
        String realProjectKey = null;
        try {
            realRelease = build.getEnvironment(listener).expand(jiraRelease);
            realReplace = build.getEnvironment(listener).expand(jiraReplaceVersion);
            realProjectKey = build.getEnvironment(listener).expand(jiraProjectKey);

            if (realRelease == null || realRelease.isEmpty()) {
                throw new IllegalArgumentException("Release is Empty");
            }

            if (realProjectKey == null || realProjectKey.isEmpty()) {
                throw new IllegalArgumentException("No project specified");
            }

            realQuery = build.getEnvironment(listener).expand(jiraQuery);
            if (realQuery == null || realQuery.isEmpty()) {
                throw new IllegalArgumentException("JQL query is Empty");
            }

            JiraSite site = getJiraSiteForProject(build.getProject());

            if (addRelease == true) {
                site.addFixVersionToIssue(realProjectKey, realRelease, realQuery);
            } else {
                if (realReplace == null || realReplace.isEmpty()) {
                    site.migrateIssuesToFixVersion(realProjectKey, realRelease, realQuery);
                } else {
                    site.replaceFixVersion(realProjectKey, realReplace, realRelease, realQuery);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(listener.fatalError(
                    "Unable to release jira version %s/%s: %s", realRelease,
                    realProjectKey, e));
            listener.finished(Result.FAILURE);
            return false;
        }
        return true;
    }

    JiraSite getJiraSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(JiraIssueMigrator.class);
        }

        @Override
        public JiraIssueMigrator newInstance(StaplerRequest req,
                                             JSONObject formData) throws FormException {
            return req.bindJSON(JiraIssueMigrator.class, formData);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraReleaseVersionMigrator_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help-release-migrate.html";
        }
    }

}
