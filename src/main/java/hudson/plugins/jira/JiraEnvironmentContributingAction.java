package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

/*
 * JiraEnvironmentVariableBuilder adds an instance of this class to the build to 
 * provide the environment variables
 */
public class JiraEnvironmentContributingAction extends InvisibleAction implements EnvironmentContributingAction {

    public static final String ISSUES_VARIABLE_NAME = "JIRA_ISSUES";
    public static final String JIRA_URL_VARIABLE_NAME = "JIRA_URL";
    
    private final String issuesList;
    
    private final String jiraUrl;
    
    public String getIssuesList(){
        return issuesList;
    }
    
    public String getJiraUrl() {
        return jiraUrl;
    }
    
    public JiraEnvironmentContributingAction(String issuesList, String jiraUrl) {
        this.issuesList = issuesList;
        this.jiraUrl = jiraUrl;
    }
    
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> ab, EnvVars ev) {
        if (ev != null){
            ev.put(ISSUES_VARIABLE_NAME, issuesList);
            ev.put(JIRA_URL_VARIABLE_NAME, getJiraUrl());
        }
    }
    
}
