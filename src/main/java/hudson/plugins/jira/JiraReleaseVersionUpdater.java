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
import org.kohsuke.stapler.StaplerRequest;

/**
 * Task which releases the jira version specified in the parameters when the build completes.
 *
 * @author Justen Walker justen.walker@gmail.com
 * @deprecated Replaced by {@link JiraReleaseVersionUpdaterBuilder} which can be used as a PostBuild step with conditional triggering.<br>
 *     Kept for backward compatibility.
 */
public class JiraReleaseVersionUpdater extends Notifier {
	private static final long serialVersionUID = 699563338312232811L;

	private String jiraProjectKey;
	private String jiraRelease;

	@DataBoundConstructor
	public JiraReleaseVersionUpdater(String jiraProjectKey, String jiraRelease) {
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
	public BuildStepDescriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}
	
	@Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		return new VersionReleaser().perform(build.getProject(), jiraProjectKey, jiraRelease, build, listener);
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
	
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(JiraReleaseVersionUpdater.class);
		}

		@Override
		public JiraReleaseVersionUpdater newInstance(StaplerRequest req,
				JSONObject formData) throws FormException {
			return req.bindJSON(JiraReleaseVersionUpdater.class, formData);
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.JiraReleaseVersionBuilder_DisplayName();
		}

		@Override
		public String getHelpFile() {
			return "/plugin/jira/help-release.html";
		}
	}
}
