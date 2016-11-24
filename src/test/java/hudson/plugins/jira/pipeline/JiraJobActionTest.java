package hudson.plugins.jira.pipeline;

import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.model.JiraIssue;
import jenkins.branch.MultiBranchProject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraJobActionTest {

    @Test
    public void testDetectBranchNameIssue() throws Exception {
        JiraSite site = mock(JiraSite.class);
        WorkflowJob job = mock(WorkflowJob.class);
        MultiBranchProject mbp = mock(MultiBranchProject.class);
        JiraIssue issue = new JiraIssue("EXAMPLE-123", "I like cake");

        when(job.getParent()).thenReturn(mbp);
        when(job.getName()).thenReturn("feature/EXAMPLE-123-I-like-cake");
        when(site.url).thenReturn(new URL("http://jira.example.com/"));
        when(site.getIssue("EXAMPLE-123")).thenReturn(issue);

        JiraJobAction.setAction(job, site);

        ArgumentCaptor<JiraJobAction> captor = ArgumentCaptor.forClass(JiraJobAction.class);
        verify(job).addAction(captor.capture());

        JiraJobAction action = captor.capture();
        assertNotNull(action.getJiraIssue());

        assertEquals("EXAMPLE-123", action.getJiraIssue().getKey());
        assertEquals("I like cake", action.getJiraIssue().getKey());
    }
}
