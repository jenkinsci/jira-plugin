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

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static hudson.plugins.jira.JiraVersionMatcher.hasName;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.hamcrest.Matchers.equalTo;

/**
 * A build step which creates new JIRA version
 *
 * @author Artem Koshelev artkoshelev@gmail.com
 */
public class JiraVersionCreator extends Notifier {
    private String jiraVersion;
    private String jiraProjectKey;

    @DataBoundConstructor
    public JiraVersionCreator(String jiraVersion, String jiraProjectKey) {
        this.jiraVersion = jiraVersion;
        this.jiraProjectKey = jiraProjectKey;
    }

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
        String realVersion = null;
        String realProjectKey = null;

        try {
            JiraSite site = getSiteForProject(build.getProject());
            realVersion = build.getEnvironment(listener).expand(jiraVersion);
            realProjectKey = build.getEnvironment(listener).expand(jiraProjectKey);

            if (isEmpty(realVersion)) {
                throw new IllegalArgumentException("No version specified");
            }
            if (isEmpty(realProjectKey)) {
                throw new IllegalArgumentException("No project specified");
            }

            List<JiraVersion> sameNamedVersions = filter(
                    hasName(equalTo(realVersion)),
                    site.getVersions(realProjectKey));

            if (sameNamedVersions.size() == 0) {
                listener.getLogger().println(Messages.JiraVersionCreator_CreatingVersion(realVersion, realProjectKey));
                site.addVersion(realVersion, realProjectKey);
            } else {
                listener.getLogger().println(Messages.JiraVersionCreator_VersionExists(realVersion, realProjectKey));
            }

        } catch (Exception e) {
            e.printStackTrace(listener.fatalError(
                    "Unable to add version %s to JIRA project %s",
                    realVersion,
                    realProjectKey,
                    e
            ));

            listener.finished(Result.FAILURE);
            return false;
        }

        return true;
    }

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
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
