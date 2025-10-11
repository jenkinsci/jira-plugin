package hudson.plugins.jira.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.model.FreeStyleBuild;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SinceLastSuccessfulBuildIssueSelectorTest {

    private static class MockEntry extends Entry {
        private final String msg;

        public MockEntry(String msg) {
            this.msg = msg;
        }

        @Override
        public java.util.Collection<String> getAffectedPaths() {
            return null;
        }

        @Override
        public hudson.model.User getAuthor() {
            return null;
        }

        @Override
        public String getMsg() {
            return this.msg;
        }
    }

    @Mock(strictness = Mock.Strictness.LENIENT)
    private FreeStyleBuild currentRun;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private FreeStyleBuild previousSuccessfulRun;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private FreeStyleBuild intermediateRun1;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private FreeStyleBuild intermediateRun2;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private JiraSite site;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private TaskListener listener;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private PrintStream logger;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ChangeLogSet changeLogSet;

    private SinceLastSuccessfulBuildIssueSelector selector;

    @BeforeEach
    void setUp() {
        selector = new SinceLastSuccessfulBuildIssueSelector();
        when(listener.getLogger()).thenReturn(logger);
        
        // Mock JiraSite with default issue pattern
        when(site.getIssuePattern()).thenReturn(JiraSite.DEFAULT_ISSUE_PATTERN);
        
        // Mock empty change sets by default (can be overridden in individual tests)
        when(changeLogSet.iterator()).thenReturn(Collections.emptyIterator());
        when(changeLogSet.isEmptySet()).thenReturn(true);
        
        // Mock getChangeSet() for AbstractBuild compatibility
        when(currentRun.getChangeSet()).thenReturn(changeLogSet);
        when(previousSuccessfulRun.getChangeSet()).thenReturn(changeLogSet);
        when(intermediateRun1.getChangeSet()).thenReturn(changeLogSet);
        when(intermediateRun2.getChangeSet()).thenReturn(changeLogSet);
        
        // Mock getChangeSets() for reflection-based extraction
        when(currentRun.getChangeSets()).thenReturn(Collections.singletonList(changeLogSet));
        when(previousSuccessfulRun.getChangeSets()).thenReturn(Collections.singletonList(changeLogSet));
        when(intermediateRun1.getChangeSets()).thenReturn(Collections.singletonList(changeLogSet));
        when(intermediateRun2.getChangeSets()).thenReturn(Collections.singletonList(changeLogSet));
    }

    @Test
    void findIssueIdsWhenNoPreviousSuccessfulBuild() {
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(null);

        Set<String> result = selector.findIssueIds(currentRun, site, listener);

        assertThat(result, empty());
        verify(logger).println("No previous successful build found. Searching only in current build.");
    }

    @Test
    void findIssueIdsWhenPreviousSuccessfulBuildExists() {
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);
        when(previousSuccessfulRun.getNumber()).thenReturn(5);
        when(currentRun.getPreviousBuild()).thenReturn(intermediateRun1);
        when(intermediateRun1.getPreviousBuild()).thenReturn(intermediateRun2);
        when(intermediateRun2.getPreviousBuild()).thenReturn(previousSuccessfulRun);

        selector.findIssueIds(currentRun, site, listener);

        verify(logger).println("Collecting JIRA issues since last successful build #5");
        verify(logger).println("Found 0 JIRA issue(s) across 3 build(s)");
    }

    @Test
    void findIssueIdsWithSingleBuildSinceSuccess() {
        // Only one build since last successful build
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);
        when(previousSuccessfulRun.getNumber()).thenReturn(10);
        when(currentRun.getPreviousBuild()).thenReturn(previousSuccessfulRun);

        selector.findIssueIds(currentRun, site, listener);

        verify(logger).println("Collecting JIRA issues since last successful build #10");
        verify(logger).println("Found 0 JIRA issue(s) across 1 build(s)");
    }

    @Test
    void findIssueIdsWithMultipleBuildsSinceSuccess() {
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);
        when(previousSuccessfulRun.getNumber()).thenReturn(3);
        when(currentRun.getPreviousBuild()).thenReturn(intermediateRun1);
        when(intermediateRun1.getPreviousBuild()).thenReturn(intermediateRun2);
        when(intermediateRun2.getPreviousBuild()).thenReturn(previousSuccessfulRun);

        selector.findIssueIds(currentRun, site, listener);

        verify(logger).println("Collecting JIRA issues since last successful build #3");
        verify(logger).println("Found 0 JIRA issue(s) across 3 build(s)");
    }

    @Test
    void findIssueIdsStopsAtSuccessfulBuild() {
        // Build chain that includes the successful build
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);
        when(previousSuccessfulRun.getNumber()).thenReturn(7);
        when(currentRun.getPreviousBuild()).thenReturn(intermediateRun1);
        when(intermediateRun1.getPreviousBuild()).thenReturn(previousSuccessfulRun);

        selector.findIssueIds(currentRun, site, listener);

        // Should stop processing at the successful build
        verify(logger).println("Collecting JIRA issues since last successful build #7");
        verify(logger).println("Found 0 JIRA issue(s) across 2 build(s)");
    }

    @Test
    void findIssueIdsHandlesNullBuildChain() {
        // Build chain with null intermediate builds
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);
        when(previousSuccessfulRun.getNumber()).thenReturn(1);
        when(currentRun.getPreviousBuild()).thenReturn(null);

        selector.findIssueIds(currentRun, site, listener);

        verify(logger).println("Collecting JIRA issues since last successful build #1");
        verify(logger).println("Found 0 JIRA issue(s) across 1 build(s)");
    }

    @Test
    void findIssueIdsExtractsIssuesFromChangelogs() {
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);
        when(previousSuccessfulRun.getNumber()).thenReturn(5);
        when(currentRun.getPreviousBuild()).thenReturn(intermediateRun1);
        when(intermediateRun1.getPreviousBuild()).thenReturn(previousSuccessfulRun);

        Set<? extends Entry> currentRunEntries = new HashSet<>(Arrays.asList(
                new MockEntry("Fixed JIRA-123"),
                new MockEntry("Updated ABC-456")
        ));
        Set<? extends Entry> intermediateRunEntries = new HashSet<>(Arrays.asList(
                new MockEntry("Fixed DEF-789")
        ));

        // Create separate ChangeLogSet mocks for each build
        ChangeLogSet currentRunChangeLogSet = mock(ChangeLogSet.class);
        ChangeLogSet intermediateRunChangeLogSet = mock(ChangeLogSet.class);

        when(currentRunChangeLogSet.iterator()).thenReturn(currentRunEntries.iterator());
        when(currentRunChangeLogSet.isEmptySet()).thenReturn(false);
        when(intermediateRunChangeLogSet.iterator()).thenReturn(intermediateRunEntries.iterator());
        when(intermediateRunChangeLogSet.isEmptySet()).thenReturn(false);

        when(currentRun.getChangeSet()).thenReturn(currentRunChangeLogSet);
        when(currentRun.getChangeSets()).thenReturn(Collections.singletonList(currentRunChangeLogSet));
        when(intermediateRun1.getChangeSet()).thenReturn(intermediateRunChangeLogSet);
        when(intermediateRun1.getChangeSets()).thenReturn(Collections.singletonList(intermediateRunChangeLogSet));

        Set<String> result = selector.findIssueIds(currentRun, site, listener);

        assertThat(result, hasSize(3));
        assertThat(result, equalTo(new HashSet<>(Arrays.asList("JIRA-123", "ABC-456", "DEF-789"))));
        verify(logger).println("Collecting JIRA issues since last successful build #5");
        verify(logger).println("Found 3 JIRA issue(s) across 2 build(s)");
    }

    @Test
    void findIssueIdsWithNoPreviousSuccessfulBuildExtractsFromCurrentBuild() {
        // No previous successful build, but current build has issues
        when(currentRun.getPreviousSuccessfulBuild()).thenReturn(null);

        Set<? extends Entry> currentRunEntries = new HashSet<>(Arrays.asList(
                new MockEntry("Fixed JIRA-999"),
                new MockEntry("Updated XYZ-111")
        ));

        ChangeLogSet currentRunChangeLogSet = mock(ChangeLogSet.class);
        when(currentRunChangeLogSet.iterator()).thenReturn(currentRunEntries.iterator());
        when(currentRunChangeLogSet.isEmptySet()).thenReturn(false);

        when(currentRun.getChangeSet()).thenReturn(currentRunChangeLogSet);
        when(currentRun.getChangeSets()).thenReturn(Collections.singletonList(currentRunChangeLogSet));

        Set<String> result = selector.findIssueIds(currentRun, site, listener);

        assertThat(result, hasSize(2));
        assertThat(result, equalTo(new HashSet<>(Arrays.asList("JIRA-999", "XYZ-111"))));
        verify(logger).println("No previous successful build found. Searching only in current build.");
    }
}
