package hudson.plugins.jira.integration.listissuesparameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import hudson.model.ParametersDefinitionProperty;
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
        p.setDefinition(new CpsFlowDefinition(
                """
                properties([
                  parameters([
                    jiraIssue(name: 'JIRA_ISSUE', description: 'Jira Test Description', jiraIssueFilter: 'project=PRJ')
                  ])
                ])""",
                true));
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
}
