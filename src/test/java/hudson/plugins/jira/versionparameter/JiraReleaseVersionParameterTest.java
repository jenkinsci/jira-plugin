package hudson.plugins.jira.versionparameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import hudson.model.ParametersDefinitionProperty;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JiraReleaseVersionParameterTest {

    @Test
    void scriptedPipeline(JenkinsRule r) throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("""
                properties([
                  parameters([
                    jiraReleaseVersion(name: 'JIRA', description: 'Jira Test Description', jiraProjectKey: 'PRJ', jiraReleasePattern: 'v[0-9]+', jiraShowReleased: 'true', jiraShowArchived: 'true')
                  ])
                ])""", true));
        r.buildAndAssertSuccess(p);

        ParametersDefinitionProperty parameters = p.getProperty(ParametersDefinitionProperty.class);
        assertThat(parameters, is(notNullValue()));
        assertThat(
                parameters.getParameterDefinitions(),
                hasItem(allOf(
                        instanceOf(JiraVersionParameterDefinition.class),
                        hasProperty("name", is("JIRA")),
                        hasProperty("description", is("Jira Test Description")),
                        hasProperty("jiraProjectKey", is("PRJ")))));
    }
}
