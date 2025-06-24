package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.model.TaskListener;
import hudson.plugins.jira.model.JiraIssue;
import java.io.PrintStream;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@WithJenkins
class JiraJobActionTest {

    JiraSite site;

    WorkflowJob job;

    WorkflowMultiBranchProject mbp;

    final JiraIssue issue = new JiraIssue("EXAMPLE-123", "I like cake");

    @BeforeEach
    void setup(JenkinsRule r) throws Exception {
        mbp = r.jenkins.createProject(WorkflowMultiBranchProject.class, "mbp");

        site = spy(new JiraSite("https://foo.com"));
        doReturn(JiraSite.DEFAULT_ISSUE_PATTERN).when(site).getIssuePattern();
        doReturn(issue).when(site).getIssue("EXAMPLE-123");
    }

    @Test
    void detectBranchNameIssue() throws Exception {
        job = new WorkflowJob(mbp, "feature/EXAMPLE-123");
        JiraJobAction.setAction(job, site);

        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNotNull(action.getIssue());
        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    void detectBranchNameIssueWithEncodedJobName() throws Exception {
        job = new WorkflowJob(mbp, "feature%2FEXAMPLE-123");
        JiraJobAction.setAction(job, site);

        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNotNull(action.getIssue());

        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    void detectBranchNameIssueJustIssueKey() throws Exception {
        job = new WorkflowJob(mbp, "EXAMPLE-123");
        JiraJobAction.setAction(job, site);

        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNotNull(action.getIssue());

        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    void detectBranchNameIssueNoIssueKey() throws Exception {
        job = new WorkflowJob(mbp, "NOTHING INTERESTING");
        JiraJobAction.setAction(job, site);
        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNull(action);
    }

    @Test
    @WithoutJenkins
    void testJobActionRestException() {
        Throwable throwable = mock(Throwable.class);
        PrintStream logger = mock(PrintStream.class);
        TaskListener listener = mock(TaskListener.class);
        WorkflowRun run = mock(WorkflowRun.class);
        WorkflowJob parent = mock(WorkflowJob.class);
        when(listener.getLogger()).thenReturn(logger);
        when(run.getParent()).thenReturn(parent);
        try (MockedStatic<JiraJobAction> jobActionMockedStatic = Mockito.mockStatic(JiraJobAction.class);
                MockedStatic<JiraSite> jiraSiteMockedStatic = Mockito.mockStatic(JiraSite.class)) {
            jiraSiteMockedStatic.when(() -> JiraSite.get(parent)).thenReturn(site);
            jobActionMockedStatic
                    .when(() -> JiraJobAction.setAction(parent, site))
                    .thenThrow(new RestClientException(
                            "[Jira Error] Jira REST setAction error. Cause: 401 error", throwable));
            JiraJobAction.RunListenerImpl.fireStarted(run, listener);
            verify(logger).println("[Jira Error] Jira REST setAction error. Cause: 401 error");
        }
    }
}
