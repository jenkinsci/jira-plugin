package hudson.plugins.jira.selector.perforce;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.tek42.perforce.model.Changelist;
import com.tek42.perforce.model.Changelist.JobEntry;

import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.plugins.perforce.PerforceChangeLogEntry;
import hudson.plugins.perforce.PerforceChangeLogSet;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;


public class JobSelectorTest {

    @Test
    public void testFindsTwoPerforceJobs() {
        final String jobIdIW1231 = "IW-1231";
        final String jobIdEC3453 = "EC-3453"; 

        FreeStyleBuild build = mock(FreeStyleBuild.class);
        BuildListener listener = mock(BuildListener.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        PerforceChangeLogSet perforceChangeLogSet = mock(PerforceChangeLogSet.class);
        JiraSite jiraSite = mock(JiraSite.class);
        when(listener.getLogger()).thenReturn(System.out);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Changelist changelist1 = new Changelist();
        JobEntry iw1231 = new JobEntry();
        
        iw1231.setJob(jobIdIW1231);
        changelist1.getJobs().add(iw1231);
        JobEntry ec3453 = new JobEntry();
        Changelist changelist2 = new Changelist();
       
        ec3453.setJob(jobIdEC3453);
        changelist2.getJobs().add(ec3453);
        ArrayList<PerforceChangeLogEntry> entries = new ArrayList<PerforceChangeLogEntry>();
       
        PerforceChangeLogEntry entry1 = new PerforceChangeLogEntry(perforceChangeLogSet);
        entry1.setChange(changelist1);
        entries.add(entry1);
        PerforceChangeLogEntry entry2 = new PerforceChangeLogEntry(perforceChangeLogSet);
        entry2.setChange(changelist2);
        entries.add(entry2);
        when(changeLogSet.iterator()).thenReturn(entries.iterator());
        

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        Set<String> expected = Sets.newHashSet(jobIdEC3453,jobIdIW1231);

        JobIssueSelector selector = new JobIssueSelector();
        Set<String> result = selector.findIssueIds(build, jiraSite, listener);

        Assert.assertEquals(expected.size(), result.size());
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testFindsIssuesWithJiraParameters() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);
        JiraSite jiraSite = mock(JiraSite.class);
        JiraIssueParameterValue parameter = mock(JiraIssueParameterValue.class);
        JiraIssueParameterValue parameterTwo = mock(JiraIssueParameterValue.class);
        ParametersAction action = mock(ParametersAction.class);
        List<ParameterValue> parameters = new ArrayList<ParameterValue>();

        when(listener.getLogger()).thenReturn(System.out);
        when(changeLogSet.iterator()).thenReturn(Collections.EMPTY_LIST.iterator());
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getAction(ParametersAction.class)).thenReturn(action);
        when(action.getParameters()).thenReturn(parameters);
        when(parameter.getValue()).thenReturn("JIRA-123");
        when(parameterTwo.getValue()).thenReturn("JIRA-321");

        Set<String> ids;

        JobIssueSelector jobIssueSelector = new JobIssueSelector();
        // Initial state contains zero parameters
        ids = jobIssueSelector.findIssueIds(build, jiraSite, listener);
        Assert.assertTrue(ids.isEmpty());


        parameters.add(parameter);
        ids = jobIssueSelector.findIssueIds(build, jiraSite, listener);
        Assert.assertEquals(1, ids.size());
        Assert.assertEquals("JIRA-123", ids.iterator().next());

        parameters.add(parameterTwo);
        ids = jobIssueSelector.findIssueIds(build, jiraSite, listener);
        Assert.assertEquals(2, ids.size());
        Set<String> expected = Sets.newTreeSet(Sets.newHashSet("JIRA-123", "JIRA-321"));
        Assert.assertEquals(expected, ids);
    }

}
