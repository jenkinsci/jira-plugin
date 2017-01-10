package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

/*
 * {@link JiraIssuesSizeEnvironmentVariableBuilder} adds an instance of this class to the build to
 * provide the environment variable
 */
public class JiraIssuesSizeEnvironmentContributingAction extends InvisibleAction implements EnvironmentContributingAction {

    private final Integer issuesNumber;

    private final String variableName;

    public int getIssuesNumber() {
        return issuesNumber;
    }

    public String getVariableName() {
        return variableName;
    }

    public JiraIssuesSizeEnvironmentContributingAction(int issuesNumber, String variableName) {
        this.issuesNumber = issuesNumber;
        this.variableName = variableName;
    }
    
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> ab, EnvVars ev) {
        if (ev != null){
            ev.put(variableName, issuesNumber.toString());
        }
    }
    
}
