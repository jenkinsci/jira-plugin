package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.UNSTABLE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueUpdateBuilderTest {

    private static final String NON_EMPTY_COMMENT = "Non empty comment";
    private static final String NON_EMPTY_WORKFLOW_LOWERCASE = "workflow";

    @Mock
    private Launcher launcher;
    @Mock
    private TaskListener listener;
    @Mock
    private AbstractBuild build;
    @Mock
    private EnvVars env;
    @Mock
    private AbstractProject project;
    @Mock
    private PrintStream logger;
    @Mock
    private JiraSite site;
    @Mock
    private JiraSession session;
    @Mock
    private Issue issue;
    @Captor
    ArgumentCaptor<Result> resultCaptor;

    @Spy
    private JiraIssueUpdateBuilder builder = new JiraIssueUpdateBuilder(null, null, null);

    private FilePath workspace;

    @Before
    public void createMocks() throws IOException, InterruptedException {
        when(build.getEnvironment(listener)).thenReturn(env);
        when(build.getParent()).thenReturn(project);
        when(listener.getLogger()).thenReturn(logger);
    }

    @Test
    public void performFailsWhenNoSite() throws InterruptedException, IOException {
        doReturn(null).when(builder).getSiteForJob(any());
        builder.perform(build, workspace, launcher, listener);
        verify(build).setResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue(), is(FAILURE));
    }

    @Test
    public void performFailsOnTimeout() throws InterruptedException, IOException, TimeoutException {
        doReturn(site).when(builder).getSiteForJob(any());
        doThrow(new TimeoutException()).when(builder).progressMatchingIssues(any(), anyString(), anyString(), anyString(), any());
        builder.perform(build, workspace, launcher, listener);
        verify(build).setResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue(), is(FAILURE));
    }

    @Test
    public void performUnstableWhenProgressFails() throws InterruptedException, IOException, TimeoutException {
        doReturn(site).when(builder).getSiteForJob(any());
        doReturn(false).when(builder).progressMatchingIssues(any(), anyString(), anyString(), anyString(), any());
        builder.perform(build, workspace, launcher, listener);
        verify(build).setResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue(), is(UNSTABLE));
    }

    @Test
    public void performProgressOK() throws InterruptedException, IOException, TimeoutException {
        doReturn(site).when(builder).getSiteForJob(any());
        doReturn(true).when(builder).progressMatchingIssues(any(), anyString(), anyString(), anyString(), any());
        builder.perform(build, workspace, launcher, listener);
    }

    @Test
    public void commentIssuesCommentsOnNonEmptyComment() {
        builder.commentIssue(session, issue, NON_EMPTY_COMMENT);
        verify(session, times(1)).addComment(any(), eq(NON_EMPTY_COMMENT), any(), any());
    }

    @Test
    public void commentIssueDoesntCommentEmptyComment() {
        builder.commentIssue(session, issue, null);
        verify(session, never()).addComment(any(), any(), any(), any());
    }

    @Test
    public void progressIssueReturnsTrueWhenEmptyWorkflow() {
        boolean result = builder.progressIssue(session, issue, null, logger);
        assertThat(result, is(true));
    }

    @Test
    public void progressIssueReturnsFalseWhenWorkflowIdNotFound() {
        when(session.getActionIdForIssue(any(), any())).thenReturn(null);
        boolean result = builder.progressIssue(session, issue, NON_EMPTY_WORKFLOW_LOWERCASE, logger);
        assertThat(result, is(false));
    }

    @Test
    public void progressMatchingIssuesReturnsFalseWhenSessionIsNull() throws TimeoutException {
        when(site.getSession()).thenReturn(null);
        boolean result = builder.progressMatchingIssues(
                site, null, null, null, logger);
        assertThat(result, is(false));
    }

    @Test
    public void progressMatchingIssuesReturnsFalseWhenOneOfIssuesFailsToProgress() throws TimeoutException {
        when(site.getSession()).thenReturn(session);
        when(session.getIssuesFromJqlSearch(any())).thenReturn(Arrays.asList(issue, issue));
        doReturn(true).doReturn(false).when(builder).progressIssue(any(), any(), any(), any());
        boolean result = builder.progressMatchingIssues(
                site, null, null, null, logger);
        assertThat(result, is(false));
    }

}
