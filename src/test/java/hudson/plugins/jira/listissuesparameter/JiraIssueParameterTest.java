package hudson.plugins.jira.listissuesparameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import hudson.EnvVars;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JiraIssueParameterTest {

    @Test
    void scriptedPipeline(JenkinsRule r) throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("""
                properties([
                  parameters([
                    jiraIssue(name: 'JIRA_ISSUE', description: 'Jira Test Description', jiraIssueFilter: 'project=PRJ')
                  ])
                ])""", true));
        r.buildAndAssertSuccess(p);

        ParametersDefinitionProperty parameters = p.getProperty(ParametersDefinitionProperty.class);
        assertThat(parameters, is(notNullValue()));
        assertThat(
                parameters.getParameterDefinitions(),
                hasItem(allOf(
                        instanceOf(JiraIssueParameterDefinition.class),
                        hasProperty("name", is("JIRA_ISSUE")),
                        hasProperty("description", is("Jira Test Description")),
                        hasProperty("jiraIssueFilter", is("project=PRJ")))));
    }

    @Test
    void nullValueDoesNotThrowException(JenkinsRule r) throws Exception {
        // Test that JiraIssueParameterValue with null value doesn't throw NPE
        JiraIssueParameterValue paramValue = new JiraIssueParameterValue("JIRA_ISSUE", null);

        // Test buildEnvironment method doesn't throw NPE with null value
        EnvVars envVars = new EnvVars();
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo 'test'", true));
        Run<?, ?> run = r.buildAndAssertSuccess(job);

        assertDoesNotThrow(() -> paramValue.buildEnvironment(run, envVars));
        assertThat(envVars.get("JIRA_ISSUE"), is(""));

        // Test that toString works with null value
        assertDoesNotThrow(() -> paramValue.toString());
    }

    @Test
    void nonNullValueInBuildEnvironment(JenkinsRule r) throws Exception {
        // Test that JiraIssueParameterValue with non-null value works correctly
        JiraIssueParameterValue paramValue = new JiraIssueParameterValue("JIRA_ISSUE", "TEST-123");

        // Test buildEnvironment method with non-null value
        EnvVars envVars = new EnvVars();
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo 'test'", true));
        Run<?, ?> run = r.buildAndAssertSuccess(job);

        assertDoesNotThrow(() -> paramValue.buildEnvironment(run, envVars));
        assertThat(envVars.get("JIRA_ISSUE"), is("TEST-123"));
    }

    @Test
    void variableResolverHandlesNullAndNonNullValues() throws Exception {
        // Test with null value
        JiraIssueParameterValue nullParamValue = new JiraIssueParameterValue("JIRA_ISSUE", null);
        hudson.util.VariableResolver<String> nullResolver = nullParamValue.createVariableResolver(null);

        // Should return empty string for matching parameter name with null value
        assertThat(nullResolver.resolve("JIRA_ISSUE"), is(""));
        // Should return null for non-matching parameter name
        assertThat(nullResolver.resolve("OTHER_PARAM"), is(nullValue()));

        // Test with non-null value
        JiraIssueParameterValue nonNullParamValue = new JiraIssueParameterValue("JIRA_ISSUE", "TEST-123");
        hudson.util.VariableResolver<String> nonNullResolver = nonNullParamValue.createVariableResolver(null);

        // Should return the actual value for matching parameter name
        assertThat(nonNullResolver.resolve("JIRA_ISSUE"), is("TEST-123"));
        // Should return null for non-matching parameter name
        assertThat(nonNullResolver.resolve("OTHER_PARAM"), is(nullValue()));
    }

    @Test
    void originalNullPointerExceptionScenarioFixed(JenkinsRule r) throws Exception {
        // This test replicates the original issue scenario where a null JIRA_ISSUE parameter
        // would cause a NullPointerException during pipeline execution with ${params.JIRA_ISSUE}

        JiraIssueParameterValue paramValue = new JiraIssueParameterValue("JIRA_ISSUE", null);

        // Test the buildEnvironment path (used when setting environment variables)
        EnvVars envVars = new EnvVars();
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo \"Ticket: ${params.JIRA_ISSUE}\"", true));
        Run<?, ?> run = r.buildAndAssertSuccess(job);

        // This should not throw NPE and should set empty string
        assertDoesNotThrow(() -> paramValue.buildEnvironment(run, envVars));
        assertThat(envVars.get("JIRA_ISSUE"), is(""));

        // Test the variable resolver path (used during pipeline script evaluation)
        hudson.util.VariableResolver<String> resolver = paramValue.createVariableResolver(null);

        // This should not throw NPE and should return empty string
        String resolvedValue = assertDoesNotThrow(() -> resolver.resolve("JIRA_ISSUE"));
        assertThat(resolvedValue, is(""));
    }
}
