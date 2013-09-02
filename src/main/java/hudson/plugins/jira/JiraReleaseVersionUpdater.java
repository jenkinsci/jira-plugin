package hudson.plugins.jira;

import static ch.lambdaj.Lambda.filter;
import static hudson.plugins.jira.JiraVersionMatcher.hasName;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.jira.JiraIssueUpdater.DescriptorImpl;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

/**
 * Task which releases the jira version specified in the parameters when the build completes.
 * 
 * @author Justen Walker <justen.walker@gmail.com>
 * 
 */
public class JiraReleaseVersionUpdater extends Notifier {

	private static final String VERSION_ALREADY_RELEASED = 
			"The version %s is already released in project %s, so nothing to do.";
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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) {
		String realRelease = "NOT_SET";

		try {
			realRelease = build.getEnvironment(listener).expand(jiraRelease);

			if (realRelease == null || realRelease.isEmpty()) {
				throw new IllegalArgumentException("Release is Empty");
			}

			JiraSite site = getSiteForProject(build.getProject());
			List<JiraVersion> sameNamedVersions = filter(
					hasName(equalTo(realRelease)), 
					site.getVersions(jiraProjectKey));
			
			if (sameNamedVersions.size() == 1 && sameNamedVersions.get(0).isReleased()) {
				listener.getLogger().println(
						String.format(VERSION_ALREADY_RELEASED, realRelease, jiraProjectKey));
			} else {
				site.releaseVersion(jiraProjectKey, realRelease);
			}		
		} catch (Exception e) {
			e.printStackTrace(listener.fatalError(
					"Unable to release jira version %s/%s: %s", realRelease,
					jiraProjectKey, e));
			listener.finished(Result.FAILURE);
			return false;
		}
		return true;
	}

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
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
