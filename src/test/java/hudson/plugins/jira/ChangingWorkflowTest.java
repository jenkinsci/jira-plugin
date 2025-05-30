package hudson.plugins.jira;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Transition;
import hudson.model.Item;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * User: lanwen
 * Date: 10.09.13
 * Time: 0:57
 */
@ExtendWith(MockitoExtension.class)
public class ChangingWorkflowTest {

    public static final String NON_EMPTY_COMMENT = "Non empty comment";
    private final String ISSUE_JQL = "jql";
    private final String NON_EMPTY_WORKFLOW_LOWERCASE = "workflow";

    @Mock
    private JiraSite site;

    @Mock
    private JiraRestService restService;

    @Mock
    private JiraSession mockSession;

    @Mock
    private Item mockItem;

    private JiraSession spySession;

    @BeforeEach
    void setupSpy() {
        spySession = spy(new JiraSession(site, restService));
    }

    @Test
    void onGetActionItInvokesServiceMethod() {
        spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE);
        verify(restService, times(1)).getAvailableActions(eq(ISSUE_JQL));
    }

    @Test
    void getActionIdReturnsNullWhenServiceReturnsNull() {
        doReturn(null).when(restService).getAvailableActions(ISSUE_JQL);
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), nullValue());
    }

    @Test
    void getActionIdIteratesOverAllActionsEvenOneOfNamesIsNull() {
        Transition action1 = mock(Transition.class);
        Transition action2 = mock(Transition.class);

        doReturn(null).when(action1).getName();
        doReturn("name").when(action2).getName();

        doReturn(Arrays.asList(action1, action2)).when(restService).getAvailableActions(ISSUE_JQL);
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), nullValue());

        verify(action1, times(1)).getName();
        verify(action2, times(2)).getName(); // one for null check, other for equals
    }

    @Test
    void getActionIdReturnsNullWhenNullWorkflowUsed() {
        String workflowAction = null;
        Transition action1 = mock(Transition.class);
        when(action1.getName()).thenReturn("name");

        when(restService.getAvailableActions(ISSUE_JQL)).thenReturn(Collections.singletonList(action1));
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, workflowAction), nullValue());
    }

    @Test
    void getActionIdReturnsIdWhenFoundIgnorecaseWorkflow() {
        String id = randomNumeric(5);
        Transition action1 = mock(Transition.class);
        when(action1.getName()).thenReturn(NON_EMPTY_WORKFLOW_LOWERCASE.toUpperCase());
        when(restService.getAvailableActions(ISSUE_JQL)).thenReturn(Arrays.asList(action1));
        when(action1.getId()).thenReturn(Integer.valueOf(id));

        assertThat(
                spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), equalTo(Integer.valueOf(id)));
    }

    @Test
    void addCommentsOnNonEmptyWorkflowAndNonEmptyComment() throws Exception {
        when(site.getSession(any(), anyBoolean())).thenCallRealMethod();
        when(site.getSession(any())).thenCallRealMethod();
        when(site.createSession(any(), anyBoolean())).thenReturn(mockSession);
        site.getSession(mockItem);

        when(mockSession.getIssuesFromJqlSearch(anyString())).thenReturn(Arrays.asList(mock(Issue.class)));

        when(site.progressMatchingIssues(anyString(), any(), anyString(), any(PrintStream.class)))
                .thenCallRealMethod();
        site.progressMatchingIssues(
                ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE, NON_EMPTY_COMMENT, mock(PrintStream.class));

        verify(mockSession, times(1)).addComment(any(), eq(NON_EMPTY_COMMENT), isNull(), isNull());
        verify(mockSession, times(1)).progressWorkflowAction(any(), anyInt());
    }

    @Test
    void addCommentsOnNullWorkflowAndNonEmptyComment() throws Exception {
        when(site.getSession(any())).thenCallRealMethod();
        when(site.getSession(any(), anyBoolean())).thenCallRealMethod();
        when(site.createSession(any(), anyBoolean())).thenReturn(mockSession);
        site.getSession(mockItem);

        when(mockSession.getIssuesFromJqlSearch(anyString())).thenReturn(Arrays.asList(mock(Issue.class)));

        when(site.progressMatchingIssues(anyString(), any(), anyString(), any(PrintStream.class)))
                .thenCallRealMethod();
        site.progressMatchingIssues(ISSUE_JQL, "", NON_EMPTY_COMMENT, mock(PrintStream.class));

        verify(mockSession, times(1)).addComment(any(), eq(NON_EMPTY_COMMENT), isNull(), isNull());
    }

    @Test
    void dontAddCommentsOnNullWorkflowAndNullComment() throws TimeoutException {
        site.progressMatchingIssues(ISSUE_JQL, null, null, mock(PrintStream.class));
        verify(mockSession, never()).addComment(anyString(), anyString(), isNull(), isNull());
    }
}
