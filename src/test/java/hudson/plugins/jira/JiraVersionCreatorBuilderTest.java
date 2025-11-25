package hudson.plugins.jira;

import static org.mockito.ArgumentMatchers.any;
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

class JiraVersionCreatorBuilderTest {

    private JiraSite site;

    private JiraSession session;

    @BeforeEach
    void createMocks() {
        site = mock(JiraSite.class);
        session = mock(JiraSession.class);
    }

    @WithJenkins
    @Test
    void testPipelineWithJiraSite(JenkinsRule r) throws Exception {
        JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
        jiraGlobalConfiguration.setSites(Collections.singletonList(site));
        doReturn(session).when(site).getSession(any());
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("""
                        jiraCreateVersion(jiraVersion: 'Version', jiraProjectKey: 'project-key')
                """, true));
        WorkflowRun b = r.buildAndAssertStatus(Result.SUCCESS, job);
        r.assertLogContains("[Jira] Creating version Version in project project-key.", b);
    }
}
