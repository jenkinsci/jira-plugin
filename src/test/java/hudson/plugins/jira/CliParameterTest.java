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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CliParameterTest {

  @Rule public JenkinsRule jenkins = new JenkinsRule();

  FreeStyleProject project;

  @Before
  public void setup() throws IOException {
    project = jenkins.createFreeStyleProject();
  }

  @Test
  public void jiraIssueParameterViaCli() throws Exception {
    project.addProperty(
        new ParametersDefinitionProperty(
            new JiraIssueParameterDefinition("jiraissue", "description", "filter")));

    CLICommandInvoker invoker = new CLICommandInvoker(jenkins, new BuildCommand());
    CLICommandInvoker.Result result =
        invoker.invokeWithArgs(project.getName(), "-p", "jiraissue=TEST-1");
    assertThat(result, succeeded());
  }

  @Test
  public void jiraVersionParameterViaCli() throws Exception {
    project.addProperty(
        new ParametersDefinitionProperty(
            new JiraVersionParameterDefinition(
                "jiraversion", "description", "PROJ", "RELEASE", "true", "false")));

    CLICommandInvoker invoker = new CLICommandInvoker(jenkins, new BuildCommand());
    CLICommandInvoker.Result result =
        invoker.invokeWithArgs(project.getName(), "-p", "jiraversion=1.0");
    assertThat(result, succeeded());
  }
}
