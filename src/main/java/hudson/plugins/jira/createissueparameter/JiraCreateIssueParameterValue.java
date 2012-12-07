package hudson.plugins.jira.createissueparameter;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created with IntelliJ IDEA.
 * User: rupali
 * Date: 6/12/12
 * Time: 2:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class JiraCreateIssueParameterValue extends ParameterValue{

     private String projectKey;

    @DataBoundConstructor
    public JiraCreateIssueParameterValue(final String name, final String projectkey) {
         super(name);
        if(projectkey == null) throw new IllegalArgumentException("ProjectKey cannot be null");
        this.projectKey=projectkey;



    }

    @Override
    public void buildEnvVars(final AbstractBuild<?, ?> build, final EnvVars env) {
        env.put(getName(), getProjectKey());
    }

    @Override
    public VariableResolver<String> createVariableResolver(
            final AbstractBuild<?, ?> build) {
        return new VariableResolver<String>() {
            public String resolve(final String name) {
                return JiraCreateIssueParameterValue.this.name.equals(name) ? getProjectKey()
                        : null;
            }
        };
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }


}
