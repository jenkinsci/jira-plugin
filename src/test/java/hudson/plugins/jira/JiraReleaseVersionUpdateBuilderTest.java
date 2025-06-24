package hudson.plugins.jira;

import static org.mockito.Mockito.*;

import hudson.model.Result;
import java.util.Collections;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class JiraReleaseVersionUpdateBuilderTest {

    private JiraSite site;

    @BeforeEach
    void createMocks() {
        site = mock(JiraSite.class);
    }

    @WithJenkins
    @Test
    void testPipeline(JenkinsRule r) throws Exception {
        JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
        jiraGlobalConfiguration.setSites(Collections.singletonList(site));
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(
                """
                        step([$class: 'JiraReleaseVersionUpdaterBuilder', jiraProjectKey: 'PROJECT', jiraRelease: 'release', jiraDescription: 'description'])
                """,
                true));
        WorkflowRun b = r.buildAndAssertStatus(Result.SUCCESS, job);
        r.assertLogContains("[Jira] Marking version release in project PROJECT as released.", b);
    }
}
