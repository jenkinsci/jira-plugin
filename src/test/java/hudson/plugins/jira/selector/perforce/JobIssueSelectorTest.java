package hudson.plugins.jira.selector.perforce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.plugins.jira.JiraCarryOverAction;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.scm.ChangeLogSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public abstract class JobIssueSelectorTest {

    protected abstract JobIssueSelector createJobIssueSelector();

    @Test
    public void findsIssuesWithJiraParameters() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);
        JiraSite jiraSite = mock(JiraSite.class);
        JiraIssueParameterValue parameter = mock(JiraIssueParameterValue.class);
        JiraIssueParameterValue parameterTwo = mock(JiraIssueParameterValue.class);
        ParametersAction action = mock(ParametersAction.class);
        List<ParameterValue> parameters = new ArrayList<>();

        when(listener.getLogger()).thenReturn(System.out);
        when(changeLogSet.iterator()).thenReturn(Collections.EMPTY_LIST.iterator());
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getAction(ParametersAction.class)).thenReturn(action);
        when(action.getParameters()).thenReturn(parameters);
        when(parameter.getValue()).thenReturn("JIRA-123");
        when(parameterTwo.getValue()).thenReturn("JIRA-321");

        Set<String> ids;

        JobIssueSelector jobIssueSelector = createJobIssueSelector();
        // Initial state contains zero parameters
        ids = jobIssueSelector.findIssueIds(build, jiraSite, listener);
        assertTrue(ids.isEmpty());

        parameters.add(parameter);
        ids = jobIssueSelector.findIssueIds(build, jiraSite, listener);
        assertEquals(1, ids.size());
        assertEquals("JIRA-123", ids.iterator().next());

        parameters.add(parameterTwo);
        ids = jobIssueSelector.findIssueIds(build, jiraSite, listener);
        assertEquals(2, ids.size());
        Set<String> expected = new TreeSet(Arrays.asList("JIRA-123", "JIRA-321"));
        assertEquals(expected, ids);
    }

    @Test
    public void findsCarriedOnIssues() {

        FreeStyleBuild build = mock(FreeStyleBuild.class);
        FreeStyleBuild previousBuild = mock(FreeStyleBuild.class);
        ArrayList<String> issues = new ArrayList<>();
        issues.add("GC-131");
        JiraCarryOverAction jiraCarryOverAction = mock(JiraCarryOverAction.class);
        when(build.getPreviousCompletedBuild()).thenReturn(previousBuild);
        when(previousBuild.getAction(JiraCarryOverAction.class)).thenReturn(jiraCarryOverAction);
        when(jiraCarryOverAction.getIDs()).thenReturn(issues);

        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);
        JiraSite jiraSite = mock(JiraSite.class);

        when(listener.getLogger()).thenReturn(System.out);
        when(changeLogSet.iterator()).thenReturn(Collections.EMPTY_LIST.iterator());
        when(build.getChangeSet()).thenReturn(changeLogSet);

        JobIssueSelector jobIssueSelector = createJobIssueSelector();

        Set<String> ids = jobIssueSelector.findIssueIds(build, jiraSite, listener);
        assertEquals(1, ids.size());
        Set<String> expected = new TreeSet<>(Collections.singletonList("GC-131"));
        assertEquals(expected, ids);
    }
}
