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

    private final Integer issuesSize;

    private final String jiraUrl;

    private final String issuesSizeVariableName;

    public String getIssuesList(){
        return issuesList;
    }

    public Integer getNumberOfIssues(){
        return issuesSize;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public String getIssuesSizeVariableName() {
        return issuesSizeVariableName;
    }
    
    public JiraEnvironmentContributingAction(String issuesList, int issuesSize, String jiraUrl, String issuesSizeVariableName) {
        this.issuesList = issuesList;
        this.issuesSize = issuesSize;
        this.jiraUrl = jiraUrl;
        this.issuesSizeVariableName = issuesSizeVariableName;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> ab, EnvVars ev) {
        if (ev != null) {
            ev.put(ISSUES_VARIABLE_NAME, issuesList);
            ev.put(getIssuesSizeVariableName(), getNumberOfIssues().toString());
            ev.put(JIRA_URL_VARIABLE_NAME, getJiraUrl());
        }
    }
}
