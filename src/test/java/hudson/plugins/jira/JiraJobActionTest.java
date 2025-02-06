package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import hudson.plugins.jira.model.JiraIssue;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

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
}
