package hudson.plugins.jira.versionparameter;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.util.VariableResolver;

public class JiraVersionParameterValue extends ParameterValue {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7715888375360839483L;
	
	private String version;
	
	@DataBoundConstructor
	public JiraVersionParameterValue(final String name, final String version) {
		super(name);
		if(version == null) throw new IllegalArgumentException("Version cannot be null");
		this.version = version;
	}

	@Override
	public void buildEnvVars(final AbstractBuild<?, ?> build, final EnvVars env) {
		env.put(getName(), getVersion());
	}

	@Override
	public VariableResolver<String> createVariableResolver(
			final AbstractBuild<?, ?> build) {
		return new VariableResolver<String>() {
			public String resolve(final String name) {
				return JiraVersionParameterValue.this.name.equals(name) ? getVersion()
						: null;
			}
		};
	}

	public void setVersion(final String version) {
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}
	
    @Override
    public String toString() {
        return "(JiraVersionParameterValue) " + getName() + "='" + version + "'";
    }
}
