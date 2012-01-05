package hudson.plugins.jira.listissuesparameter;

import hudson.EnvVars;
import hudson.model.ParameterValue;
import hudson.model.AbstractBuild;
import hudson.util.VariableResolver;

import org.kohsuke.stapler.DataBoundConstructor;

public class JiraIssueParameterValue extends ParameterValue {
	private static final long serialVersionUID = -1078274709338167211L;

	private String issue;

	@DataBoundConstructor
	public JiraIssueParameterValue(final String name, final String issue) {
		super(name);
		this.issue = issue;
	}

	@Override
	public void buildEnvVars(final AbstractBuild<?, ?> build, final EnvVars env) {
		env.put(getName(), getIssue());
	}

	@Override
	public VariableResolver<String> createVariableResolver(
			final AbstractBuild<?, ?> build) {
		return new VariableResolver<String>() {
			public String resolve(final String name) {
				return JiraIssueParameterValue.this.name.equals(name) ? getIssue()
						: null;
			}
		};
	}

	public void setIssue(final String issue) {
		this.issue = issue;
	}

	public String getIssue() {
		return issue;
	}

    @Override
    public String toString() {
        return "(JiraIssueParameterValue) " + getName() + "='" + issue + "'";
    }
}
