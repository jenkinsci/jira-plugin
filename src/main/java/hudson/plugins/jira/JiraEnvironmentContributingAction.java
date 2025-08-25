package hudson.plugins.jira;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

/*
 * JiraEnvironmentVariableBuilder adds an instance of this class to the build to provide the environment variables
 *
 */
public class JiraEnvironmentContributingAction extends InvisibleAction implements EnvironmentContributingAction {

    public static final String ISSUES_VARIABLE_NAME = "JIRA_ISSUES";
    public static final String JIRA_URL_VARIABLE_NAME = "JIRA_URL";
    public static final String ISSUES_SIZE_VARIABLE_NAME = "JIRA_ISSUES_SIZE";

    private final String issuesList;

    private final Integer issuesSize;

    private final String jiraUrl;

    @Nullable
    public String getIssuesList() {
        return issuesList;
    }

    public Integer getNumberOfIssues() {
        return issuesSize == null ? Integer.valueOf(0) : issuesSize;
    }

    @Nullable
    public String getJiraUrl() {
        return jiraUrl;
    }

    public JiraEnvironmentContributingAction(String issuesList, Integer issuesSize, String jiraUrl) {
        this.issuesList = issuesList;
        this.issuesSize = issuesSize;
        this.jiraUrl = jiraUrl;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> ab, EnvVars ev) {
        if (ev != null) {
            ev.put(ISSUES_VARIABLE_NAME, getIssuesList());
            ev.put(ISSUES_SIZE_VARIABLE_NAME, getNumberOfIssues().toString());
            ev.put(JIRA_URL_VARIABLE_NAME, getJiraUrl());
        }
    }
}
