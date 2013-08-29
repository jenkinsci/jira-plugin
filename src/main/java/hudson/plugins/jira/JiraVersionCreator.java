package hudson.plugins.jira;

import static ch.lambdaj.Lambda.filter;
import static hudson.plugins.jira.JiraVersionMatcher.hasName;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A build step which creates new jira version
 * @author Artem Koshelev artkoshelev@gmail.com
 *
 */
public class JiraVersionCreator extends Notifier {
	private static final String VERSION_EXISTS = 
			"A version with name %s already exists in project %s, so nothing to do.";

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

		try {
			realVersion = build.getEnvironment(listener).expand(jiraVersion);

			if (realVersion == null || realVersion.isEmpty()) {
				throw new IllegalArgumentException("No version specified");
			}
			
			JiraSite site = getSiteForProject(build.getProject());
			List<JiraVersion> sameNamedVersions = filter(
					hasName(equalTo(realVersion)), 
					site.getVersions(jiraProjectKey));
			
			if (sameNamedVersions.size() == 0) {
				site.addVersion(realVersion, jiraProjectKey);
			} else {
				listener.getLogger().println(
						String.format(VERSION_EXISTS, realVersion, jiraProjectKey));
			}
		} catch (Exception e) {
			e.printStackTrace(listener.fatalError(
					"Unable to add version %s to jira project %s", realVersion,
					jiraProjectKey, e));
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
		public JiraVersionCreator newInstance(StaplerRequest req,
				JSONObject formData) throws FormException {
			return req.bindJSON(JiraVersionCreator.class, formData);
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
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
