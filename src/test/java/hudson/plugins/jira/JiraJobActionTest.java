package hudson.plugins.jira;

import hudson.model.Action;
import hudson.model.Job;
import hudson.plugins.jira.model.JiraIssue;
import jenkins.branch.MultiBranchProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraJobActionTest {

    @Mock
    JiraSite site;

    @Mock
    Job job;

    @Mock
    MultiBranchProject mbp;

    final JiraIssue issue = new JiraIssue("EXAMPLE-123", "I like cake");

    @Test
    public void testDetectBranchNameIssue() throws Exception {
        when(job.getName()).thenReturn("EXAMPLE-123");
        ArgumentCaptor<JiraJobAction> captor = ArgumentCaptor.forClass(JiraJobAction.class);
        JiraJobAction.setAction(job, site);
        verify(job).addAction(captor.capture());

        JiraJobAction action = captor.getValue();
        assertNotNull(action.getIssue());

        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    public void testDetectBranchNameIssueWithEncodedJobName() throws Exception {
        when(job.getName()).thenReturn("feature%2FEXAMPLE-123");
        ArgumentCaptor<JiraJobAction> captor = ArgumentCaptor.forClass(JiraJobAction.class);
        JiraJobAction.setAction(job, site);
        verify(job).addAction(captor.capture());

        JiraJobAction action = captor.getValue();
        assertNotNull(action.getIssue());

        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    public void testDetectBranchNameIssueJustIssueKey() throws Exception {
        when(job.getName()).thenReturn("EXAMPLE-123");
        ArgumentCaptor<JiraJobAction> captor = ArgumentCaptor.forClass(JiraJobAction.class);
        JiraJobAction.setAction(job, site);
        verify(job).addAction(captor.capture());

        JiraJobAction action = captor.getValue();
        assertNotNull(action.getIssue());

        assertEquals("EXAMPLE-123", action.getIssue().getKey());
        assertEquals("I like cake", action.getIssue().getSummary());
    }

    @Test
    public void testDetectBranchNameIssueNoIssueKey() throws Exception {
        when(job.getName()).thenReturn("NOTHING INTERESTING");
        JiraJobAction.setAction(job, site);
        verify(job, never()).addAction((Action) anyObject());
    }

    @Before
    public void setup() throws Exception {
        when(job.getParent()).thenReturn(mbp);
        when(site.getIssuePattern()).thenReturn(JiraSite.DEFAULT_ISSUE_PATTERN);
        when(site.getIssue("EXAMPLE-123")).thenReturn(issue);
    }
}
