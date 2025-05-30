package hudson.plugins.jira.selector.perforce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.perforce.p4java.impl.generic.core.Fix;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.plugins.jira.JiraSite;
import hudson.scm.ChangeLogSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jenkinsci.plugins.p4.changes.P4ChangeEntry;
import org.jenkinsci.plugins.p4.changes.P4ChangeSet;
import org.junit.jupiter.api.Test;

class P4JobIssueSelectorTest extends JobIssueSelectorTest {

    @Test
    void findsTwoP4Jobs() {
        final String jobIdIW1231 = "IW-1231";
        final String jobIdEC3453 = "EC-3453";

        FreeStyleBuild build = mock(FreeStyleBuild.class);
        BuildListener listener = mock(BuildListener.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        P4ChangeSet perforceChangeLogSet = mock(P4ChangeSet.class);
        JiraSite jiraSite = mock(JiraSite.class);
        Fix fixIW1231 = mock(Fix.class);
        Fix fixEC3453 = mock(Fix.class);

        when(fixIW1231.getJobId()).thenReturn(jobIdIW1231);
        when(fixEC3453.getJobId()).thenReturn(jobIdEC3453);

        when(listener.getLogger()).thenReturn(System.out);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        ArrayList<P4ChangeEntry> entries = new ArrayList<>();

        P4ChangeEntry entry1 = new P4ChangeEntry(perforceChangeLogSet);
        entry1.getJobs().add(fixIW1231);
        entries.add(entry1);

        P4ChangeEntry entry2 = new P4ChangeEntry(perforceChangeLogSet);
        entry2.getJobs().add(fixEC3453);
        entries.add(entry2);
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        Set<String> expected = new HashSet<>(Arrays.asList(jobIdEC3453, jobIdIW1231));

        P4JobIssueSelector selector = new P4JobIssueSelector();
        Set<String> result = selector.findIssueIds(build, jiraSite, listener);

        assertEquals(expected.size(), result.size());
        assertEquals(expected, result);
    }

    @Override
    protected JobIssueSelector createJobIssueSelector() {
        return new P4JobIssueSelector();
    }
}
