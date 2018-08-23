package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Transition;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: lanwen
 * Date: 10.09.13
 * Time: 0:57
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangingWorkflowTest {

    private static final String ISSUE_JQL = "jql";
    private static final String NON_EMPTY_WORKFLOW_LOWERCASE = "workflow";

    @Mock
    private JiraSite site;
    @Mock
    private JiraRestService restService;

    private JiraSession spySession;

    @Before
    public void setupSpy() {
        spySession = spy(new JiraSession(site, restService));
    }

    @Test
    public void onGetActionItInvokesServiceMethod() {
        spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE);
        verify(restService, times(1)).getAvailableActions(eq(ISSUE_JQL));
    }

    @Test
    public void getActionIdReturnsNullWhenServiceReturnsNull() {
        when(restService.getAvailableActions(ISSUE_JQL)).thenReturn(null);
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), nullValue());
    }


    @Test
    public void getActionIdIteratesOverAllActionsEvenOneOfNamesIsNull() {
        Transition action1 = mock(Transition.class);
        Transition action2 = mock(Transition.class);

        when(action1.getName()).thenReturn(null);
        when(action2.getName()).thenReturn("name");

        when(restService.getAvailableActions(ISSUE_JQL)).thenReturn(Lists.newArrayList(action1, action2));
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), nullValue());

        verify(action1, times(1)).getName();
        verify(action2, times(2)).getName(); // one for null check, other for equals
    }

    @Test
    public void getActionIdReturnsNullWhenNullWorkflowUsed() {
        String workflowAction = null;
        Transition action1 = mock(Transition.class);
        when(action1.getName()).thenReturn("name");

        when(restService.getAvailableActions(ISSUE_JQL)).thenReturn(Lists.newArrayList(action1));
        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, workflowAction), nullValue());
    }

    @Test
    public void getActionIdReturnsIdWhenFoundIgnorecaseWorkflow() {
        String id = randomNumeric(5);
        Transition action1 = mock(Transition.class);
        when(action1.getName()).thenReturn(NON_EMPTY_WORKFLOW_LOWERCASE.toUpperCase());
        when(restService.getAvailableActions(ISSUE_JQL)).thenReturn(Lists.newArrayList(action1));
        when(action1.getId()).thenReturn(Integer.valueOf(id));

        assertThat(spySession.getActionIdForIssue(ISSUE_JQL, NON_EMPTY_WORKFLOW_LOWERCASE), equalTo(Integer.valueOf(id)));
    }

}
