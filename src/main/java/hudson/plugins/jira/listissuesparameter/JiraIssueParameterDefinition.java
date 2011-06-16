package hudson.plugins.jira.listissuesparameter;

import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.soap.RemoteIssue;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

public class JiraIssueParameterDefinition extends ParameterDefinition {
	private static final long serialVersionUID = 3927562542249244416L;

	private String jiraIssueFilter;

	@DataBoundConstructor
	public JiraIssueParameterDefinition(String name, String description, String jiraIssueFilter) {
		super(name, description);

		this.jiraIssueFilter = jiraIssueFilter;
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

	public List<JiraIssueParameterDefinition.Result> getIssues() {
		AbstractProject<?, ?> context = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);
		JiraSite site = JiraSite.get(context);

		RemoteIssue[] issues = null;
		List<Result> issueValues = new ArrayList<Result>();

		try {
			JiraSession session = site.createSession();

			// session will be null if no SOAP-accessible JIRA site is available
			if (session != null) {
				issues = session.getIssuesFromJqlSearch(jiraIssueFilter);
			}
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
