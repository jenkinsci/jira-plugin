package hudson.plugins.jira;

import hudson.plugins.jira.model.JiraIssue;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;

public class JiraJobActionTest {

    JiraSite site;

    WorkflowJob job;

    WorkflowMultiBranchProject mbp;

    @Rule
    public JenkinsRule r = new JenkinsRule();

    final JiraIssue issue = new JiraIssue("EXAMPLE-123", "I like cake");

    @Before
    public void setup() throws Exception {
        mbp = r.jenkins.createProject( WorkflowMultiBranchProject.class, "mbp" );

        site = spy(new JiraSite("https://foo.com"));
        doReturn(JiraSite.DEFAULT_ISSUE_PATTERN).when(site).getIssuePattern();
        doReturn(issue).when(site).getIssue("EXAMPLE-123");
    }

    @Test
    public void detectBranchNameIssue() throws Exception {
        job = new WorkflowJob( mbp, "feature/EXAMPLE-123" );
        JiraJobAction.setAction(job, site);

        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNotNull(action.getIssue());
        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    public void detectBranchNameIssueWithEncodedJobName() throws Exception {
        job = new WorkflowJob( mbp, "feature%2FEXAMPLE-123");
        JiraJobAction.setAction(job, site);

        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNotNull(action.getIssue());

        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    public void detectBranchNameIssueJustIssueKey() throws Exception {
        job = new WorkflowJob( mbp, "EXAMPLE-123");
        JiraJobAction.setAction(job, site);

        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNotNull(action.getIssue());

        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    public void detectBranchNameIssueNoIssueKey() throws Exception {
        job = new WorkflowJob( mbp, "NOTHING INTERESTING");
        JiraJobAction.setAction(job, site);
        JiraJobAction action = job.getAction(JiraJobAction.class);
        assertNull(action);
    }

}
