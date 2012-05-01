package hudson.plugins.jira;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.BuildWrapper;

public class JiraCreateReleaseNotes extends BuildWrapper {
	
	public static final String DEFAULT_FILTER = "status in (Resolved, Closed)";
	
	private String jiraEnvironmentVariable;
	private String jiraProjectKey;
	private String jiraRelease;
	private String jiraFilter;

	@DataBoundConstructor
	public JiraCreateReleaseNotes(String jiraProjectKey, String jiraRelease, String jiraEnvironmentVariable) {
		this(jiraProjectKey, jiraRelease, jiraEnvironmentVariable, DEFAULT_FILTER);
	}
	
	public JiraCreateReleaseNotes(String jiraProjectKey, String jiraRelease, String jiraEnvironmentVariable, String jiraFilter) {
		this.jiraRelease = jiraRelease;
		this.jiraProjectKey = jiraProjectKey;
		this.jiraEnvironmentVariable = jiraEnvironmentVariable;
		this.jiraFilter = jiraFilter;
	}
	
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		String realRelease = null;
		String releaseNotes = "No Release Notes";
		String realFilter = DEFAULT_FILTER;
		try {
			realRelease = build.getEnvironment(listener).expand(jiraRelease);

			if (realRelease == null || realRelease.isEmpty()) {
				throw new IllegalArgumentException("Release is Empty");
			}
			
			if( jiraFilter != null ) realFilter = build.getEnvironment(listener).expand(jiraFilter);

			JiraSite site = JiraSite.get(build.getProject());
			
			releaseNotes = site.getReleaseNotesForFixVersion(jiraProjectKey, realRelease, realFilter);

		} catch (Exception e) {
			e.printStackTrace(listener.fatalError(
					"Unable to generate release notes for JIRA version %s/%s: %s", realRelease,
					jiraProjectKey, e));
			listener.finished(Result.FAILURE);
			return new Environment() { };
		}
		
		Map<String,String> envMap = new HashMap<String,String>();
		envMap.put(jiraEnvironmentVariable, releaseNotes);
		
		final Map<String,String> resultVariables = envMap; 
	
		return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.putAll(resultVariables);
            }
        };
	}
	
	public String getJiraEnvironmentVariable() {
		return jiraEnvironmentVariable;
	}

	public void setJiraEnvironmentVariable(String jiraEnvironmentVariable) {
		this.jiraEnvironmentVariable = jiraEnvironmentVariable;
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
	
	public String getJiraFilter() {
		return jiraFilter;
	}

	public void setJiraFilter(String jiraFilter) {
		this.jiraFilter = jiraFilter;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
	
	@Extension
	public final static class Descriptor extends BuildWrapperDescriptor {

		@Override
		public String getDisplayName() {
			return "Generate Release Notes";
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}
	}
}
