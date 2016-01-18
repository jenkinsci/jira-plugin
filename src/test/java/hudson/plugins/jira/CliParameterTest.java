package hudson.plugins.jira;

import hudson.cli.BuildCommand;
import hudson.cli.CLICommandInvoker;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterDefinition;
import hudson.plugins.jira.versionparameter.JiraVersionParameterDefinition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;
import static hudson.cli.CLICommandInvoker.Matcher.*;


public class CliParameterTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolderRule = new TemporaryFolder();

    @Test
    public void testJiraIssueParameterViaCli() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        project.addProperty(
            new ParametersDefinitionProperty(
                new JiraIssueParameterDefinition("jiraissue", "description", "filter")
            )
        );

        CLICommandInvoker invoker = new CLICommandInvoker(jenkins, new BuildCommand());
        CLICommandInvoker.Result result = invoker.invokeWithArgs(project.getName(), "-p", "jiraissue=TEST-1");
        assertThat(result, succeeded());
    }

    @Test
    public void testJiraVersionParameterViaCli() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        project.addProperty(
            new ParametersDefinitionProperty(
                new JiraVersionParameterDefinition("jiraversion", "description", "PROJ", "RELEASE", "true", "false")
            )
        );

        CLICommandInvoker invoker = new CLICommandInvoker(jenkins, new BuildCommand());
        CLICommandInvoker.Result result = invoker.invokeWithArgs(project.getName(), "-p", "jiraversion=1.0");
        assertThat(result, succeeded());
    }
}