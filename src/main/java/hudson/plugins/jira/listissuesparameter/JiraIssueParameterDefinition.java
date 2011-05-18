package hudson.plugins.jira.listissuesparameter;

import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.soap.RemoteIssue;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.rpc.ServiceException;

public class JiraIssueParameterDefinition extends ParameterDefinition implements
		Comparable<JiraIssueParameterDefinition> {
	private static final long serialVersionUID = 3927562542249244416L;

	private String jiraIssueFilter;

	private final UUID uuid;

	@DataBoundConstructor
	public JiraIssueParameterDefinition(String name, String description,
			String jiraIssueFilter, String uuid) {
		super(name, description);

		this.jiraIssueFilter = jiraIssueFilter;

		if (uuid == null || uuid.length() == 0) {
			this.uuid = UUID.randomUUID();
		} else {
			this.uuid = UUID.fromString(uuid);
		}
	}

	public int compareTo(JiraIssueParameterDefinition pd) {
		if (pd.uuid.equals(uuid)) {
			return 0;
		}
		return -1;
	}

	@Override
	public ParameterValue createValue(StaplerRequest req) {
		String[] values = req.getParameterValues(getName());
		if (values == null || values.length != 1) {
			return null;
		} else {
			return new JiraIssueParameterValue(getName(), values[0]);
		}
	}

	@Override
	public ParameterValue createValue(StaplerRequest req, JSONObject formData) {
		JiraIssueParameterValue value = req.bindJSON(
				JiraIssueParameterValue.class, formData);
		return value;
	}

	@Override
	public ParameterDescriptor getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public List<JiraIssueParameterDefinition.Result> getIssues() {
		AbstractProject<?, ?> context = null;

		@SuppressWarnings("rawtypes")
		List<AbstractProject> jobs = Hudson.getInstance().getItems(
				AbstractProject.class);

		// which project is this parameter bound to?
		for (AbstractProject<?, ?> project : jobs) {
			ParametersDefinitionProperty property = (ParametersDefinitionProperty) project
					.getProperty(ParametersDefinitionProperty.class);
			if (property != null) {
				List<ParameterDefinition> parameterDefinitions = property
						.getParameterDefinitions();
				if (parameterDefinitions != null) {
					for (ParameterDefinition pd : parameterDefinitions) {
						if (pd instanceof JiraIssueParameterDefinition
								&& ((JiraIssueParameterDefinition) pd)
										.compareTo(this) == 0) {
							context = project;
							break;
						}
					}
				}
			}
		}

		JiraSite site = JiraSite.get(context);

		RemoteIssue[] issues = null;
		List<Result> issueValues = new ArrayList<Result>();

		try {
			JiraSession session = site.createSession();
			issues = session.getIssuesFromJqlSearch(jiraIssueFilter);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}

		if (issues != null) {
			for (RemoteIssue issue : issues) {
				issueValues.add(new Result(issue));
			}
		}

		return issueValues;
	}

	public String getJiraIssueFilter() {
		return jiraIssueFilter;
	}

	public void setJiraIssueFilter(String jiraIssueFilter) {
		this.jiraIssueFilter = jiraIssueFilter;
	}

	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {
		@Override
		public String getDisplayName() {
			return "JIRA Issue Parameter";
		}
	}

	public static class Result {
		public final String key;
		public final String summary;

		public Result(final RemoteIssue issue) {
			this.key = issue.getKey();
			this.summary = issue.getSummary();
		}
	}
}
