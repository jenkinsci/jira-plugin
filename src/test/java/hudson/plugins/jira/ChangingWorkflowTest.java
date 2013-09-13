package hudson.plugins.jira;

import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.RemoteFieldValue;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.plugins.jira.soap.RemoteNamedObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

/**
 * User: lanwen
 * Date: 10.09.13
 * Time: 0:57
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangingWorkflowTest {

    public static final String NON_EMPTY_COMMENT = "Non empty comment";
    private final String TOKEN = "TOKEN";
    private final String ISSUE_JQL = "jql";
    private final String NON_EMPTY_WORKFLOW_LOWERCASE = "workflow";

    @Mock
    private JiraSite site;

    @Mock
    private JiraSoapService service;

    @Mock
    private JiraSession mockSession;


    private JiraSession spySession;

    @Before
    public void setupSpy() {
        spySession = spy(new JiraSession(site, service, TOKEN));
    }

    @Test
    public void onGetActionItInvokesServiceMethod() throws RemoteException {
        spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE);
        verify(service, times(1)).getAvailableActions(eq(TOKEN), eq(ISSUE_JQL));
    }

    @Test
    public void getActionIdReturnsNullWhenServiceReturnsNull() throws RemoteException {
        when(service.getAvailableActions(TOKEN, ISSUE_JQL)).thenReturn(null);
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), nullValue());
    }


    @Test
    public void getActionIdIteratesOverAllActionsEvenOneOfNamesIsNull() throws RemoteException {
        RemoteNamedObject action1 = mock(RemoteNamedObject.class);
        RemoteNamedObject action2 = mock(RemoteNamedObject.class);

        when(action1.getName()).thenReturn(null);
        when(action2.getName()).thenReturn("name");

        when(service.getAvailableActions(TOKEN, ISSUE_JQL)).thenReturn(new RemoteNamedObject[]{action1, action2});
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), nullValue());

        verify(action1, times(1)).getName();
        verify(action2, times(2)).getName(); // one for null check, other for equals
    }

    @Test
    public void getActionIdReturnsNullWhenNullWorkflowUsed() throws RemoteException {
        String workflowAction = null;
        RemoteNamedObject action1 = mock(RemoteNamedObject.class);
        when(action1.getName()).thenReturn("name");

        when(service.getAvailableActions(TOKEN, ISSUE_JQL)).thenReturn(new RemoteNamedObject[]{action1});
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, workflowAction), nullValue());
    }

    @Test
    public void getActionIdReturnsIdWhenFoundIgnorecaseWorkflow() throws RemoteException {
        String id = randomAlphanumeric(5);
        RemoteNamedObject action1 = mock(RemoteNamedObject.class);
        when(action1.getName()).thenReturn(NON_EMPTY_WORKFLOW_LOWERCASE.toUpperCase());
        when(service.getAvailableActions(TOKEN, ISSUE_JQL)).thenReturn(new RemoteNamedObject[]{action1});
        when(action1.getId()).thenReturn(id);

        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), equalTo(id));
    }


    @Test
    public void addCommentsOnNonEmptyWorkflowAndNonEmptyComment() throws IOException, ServiceException {
        when(site.getSession()).thenReturn(mockSession);
        when(mockSession.getIssuesFromJqlSearch(anyString())).thenReturn(new RemoteIssue[]{mock(RemoteIssue.class)});
        when(mockSession.getActionIdForIssue(anyString(),
                eq(NON_EMPTY_WORKFLOW_LOWERCASE))).thenReturn(randomAlphanumeric(5));
        when(site.progressMatchingIssues(anyString(), anyString(), anyString(), Matchers.any(PrintStream.class)))
                .thenCallRealMethod();

        site.progressMatchingIssues(ISSUE_JQL,
                NON_EMPTY_WORKFLOW_LOWERCASE, NON_EMPTY_COMMENT, mock(PrintStream.class));

        verify(mockSession, times(1)).addComment(anyString(), eq(NON_EMPTY_COMMENT),
                isNull(String.class), isNull(String.class));
        verify(mockSession, times(1)).progressWorkflowAction(anyString(), anyString(),
                Matchers.any(RemoteFieldValue[].class));
    }


    @Test
    public void addCommentsOnNullWorkflowAndNonEmptyComment() throws IOException, ServiceException {
        when(site.getSession()).thenReturn(mockSession);
        when(mockSession.getIssuesFromJqlSearch(anyString())).thenReturn(new RemoteIssue[]{mock(RemoteIssue.class)});
        when(site.progressMatchingIssues(anyString(), anyString(), anyString(), Matchers.any(PrintStream.class)))
                .thenCallRealMethod();

        site.progressMatchingIssues(ISSUE_JQL, null, NON_EMPTY_COMMENT, mock(PrintStream.class));

        verify(mockSession, times(1)).addComment(anyString(), eq(NON_EMPTY_COMMENT),
                isNull(String.class), isNull(String.class));
    }


    @Test
    public void dontAddCommentsOnNullWorkflowAndNullComment() throws IOException, ServiceException {
        when(site.getSession()).thenReturn(mockSession);
        when(mockSession.getIssuesFromJqlSearch(anyString())).thenReturn(new RemoteIssue[]{mock(RemoteIssue.class)});
        when(site.progressMatchingIssues(anyString(), anyString(), anyString(), Matchers.any(PrintStream.class)))
                .thenCallRealMethod();

        site.progressMatchingIssues(ISSUE_JQL, null, null, mock(PrintStream.class));

        verify(mockSession, never()).addComment(anyString(), anyString(), isNull(String.class), isNull(String.class));
    }


}
