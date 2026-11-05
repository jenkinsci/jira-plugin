package hudson.plugins.jira.selector.perforce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.perforce.p4java.core.IFix;
import com.perforce.p4java.impl.generic.core.Fix;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.plugins.jira.JiraSite;
import hudson.scm.ChangeLogSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jenkinsci.plugins.p4.changes.P4ChangeEntry;
import org.jenkinsci.plugins.p4.changes.P4ChangeSet;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class P4JobIssueSelectorTest extends JobIssueSelectorTest {

    @Test
    void findsTwoP4Jobs() {
        Set<String> result = findIssueIdsIn(p4EntryWith(fixWithId("IW-1231")), p4EntryWith(fixWithId("EC-3453")));

        assertEquals(Set.of("IW-1231", "EC-3453"), result);
    }

    @Test
    void skipsP4EntryWithoutJobList() {
        P4ChangeEntry entryWithoutJobs = mock(P4ChangeEntry.class);
        when(entryWithoutJobs.getJobs()).thenReturn(null);

        Set<String> result = findIssueIdsIn(entryWithoutJobs, p4EntryWith(fixWithId("IW-1231")));

        assertEquals(Set.of("IW-1231"), result);
    }

    @Test
    void ignoresEntriesFromOtherScms() {
        ChangeLogSet.Entry otherScmEntry = mock(ChangeLogSet.Entry.class);
        when(otherScmEntry.getMsg()).thenReturn("EC-3453 mentioned in a commit message");

        Set<String> result = findIssueIdsIn(otherScmEntry, p4EntryWith(fixWithId("IW-1231")));

        assertEquals(Set.of("IW-1231"), result);
    }

    @Test
    void collapsesJobSharedByTwoEntries() {
        Set<String> result = findIssueIdsIn(p4EntryWith(fixWithId("IW-1231")), p4EntryWith(fixWithId("IW-1231")));

        assertEquals(Set.of("IW-1231"), result);
    }

    /**
     * Uses a real {@link P4ChangeEntry}, whose constructor needs a running Jenkins since p4 1.18.1,
     * so a change in how p4 exposes jobs fails here rather than being hidden by a stub.
     * The {@link JenkinsRule} parameter stays although unused: requesting it is what boots Jenkins
     * and marks this as the one Jenkins-booting test, the others here run without Jenkins.
     */
    @Test
    @WithJenkins
    void readsJobsFromRealP4ChangeEntry(JenkinsRule jenkins) {
        P4ChangeEntry entry = new P4ChangeEntry(mock(P4ChangeSet.class));
        entry.addJob(fixWithId("IW-1231"));

        assertEquals(Set.of("IW-1231"), findIssueIdsIn(entry));
    }

    @Override
    protected JobIssueSelector createJobIssueSelector() {
        return new P4JobIssueSelector();
    }

    private static Fix fixWithId(String jobId) {
        Fix fix = mock(Fix.class);
        when(fix.getJobId()).thenReturn(jobId);
        return fix;
    }

    private static P4ChangeEntry p4EntryWith(IFix... jobs) {
        P4ChangeEntry entry = mock(P4ChangeEntry.class);
        when(entry.getJobs()).thenReturn(List.of(jobs));
        return entry;
    }

    private static Set<String> findIssueIdsIn(ChangeLogSet.Entry... entries) {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        BuildListener listener = mock(BuildListener.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);

        when(listener.getLogger()).thenReturn(System.out);
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(changeLogSet.iterator()).thenReturn(Arrays.asList(entries).iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        return new P4JobIssueSelector().findIssueIds(build, mock(JiraSite.class), listener);
    }
}
