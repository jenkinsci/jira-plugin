package hudson.plugins.jira;

import static hudson.cli.CLICommandInvoker.Matcher.succeeded;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.cli.BuildCommand;
import hudson.cli.CLICommandInvoker;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterDefinition;
import hudson.plugins.jira.versionparameter.JiraVersionParameterDefinition;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CliParameterTest {

    private JenkinsRule jenkins;

    FreeStyleProject project;

    @BeforeEach
    void setup(JenkinsRule jenkins) throws IOException {
        this.jenkins = jenkins;
        project = jenkins.createFreeStyleProject();
    }

    @Test
    void jiraIssueParameterViaCli() throws Exception {
        project.addProperty(new ParametersDefinitionProperty(
                new JiraIssueParameterDefinition("jiraissue", "description", "filter")));

        CLICommandInvoker invoker = new CLICommandInvoker(jenkins, new BuildCommand());
        CLICommandInvoker.Result result = invoker.invokeWithArgs(project.getName(), "-s", "-p", "jiraissue=TEST-1");
        assertThat(result, succeeded());
    }

    @Test
    void jiraVersionParameterViaCli() throws Exception {
        project.addProperty(new ParametersDefinitionProperty(
                new JiraVersionParameterDefinition("jiraversion", "description", "PROJ", "RELEASE", "true", "false")));

        CLICommandInvoker invoker = new CLICommandInvoker(jenkins, new BuildCommand());
        CLICommandInvoker.Result result = invoker.invokeWithArgs(project.getName(), "-s", "-p", "jiraversion=1.0");
        assertThat(result, succeeded());
    }
}
