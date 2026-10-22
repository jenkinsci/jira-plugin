package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunScmChangeExtractorTest {

    public abstract static class WorkflowRunLike extends Run {
        protected WorkflowRunLike() {
            super(mock(Job.class), (Calendar) null);
        }

        public abstract List<ChangeLogSet<? extends Entry>> getChangeSets();
    }

    @Test
    void getChangesThrowsOnNullRun() {
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> RunScmChangeExtractor.getChanges(null));
        assertEquals("run cannot be null!", ex.getMessage());
    }

    @Test
    void getChangesReturnsEmptyListWhenChangeSetIsEmpty() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        ChangeLogSet<? extends Entry> changeLogSet = mock(ChangeLogSet.class);
        doReturn(true).when(changeLogSet).isEmptySet();
        doReturn(changeLogSet).when(build).getChangeSet();

        List<ChangeLogSet<? extends Entry>> changes = RunScmChangeExtractor.getChanges(build);

        assertTrue(changes.isEmpty());
    }

    @Test
    void getChangesReturnsSingletonListWhenChangeSetIsNotEmpty() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        ChangeLogSet<? extends Entry> changeLogSet = mock(ChangeLogSet.class);
        doReturn(false).when(changeLogSet).isEmptySet();
        doReturn(changeLogSet).when(build).getChangeSet();

        List<ChangeLogSet<? extends Entry>> changes = RunScmChangeExtractor.getChanges(build);

        assertEquals(1, changes.size());
        assertSame(changeLogSet, changes.get(0));
    }

    @Test
    void getChangesUsesReflectionForWorkflowRun() {
        WorkflowRunLike run = mock(WorkflowRunLike.class);
        ChangeLogSet<? extends Entry> changeLogSet = mock(ChangeLogSet.class);
        List<ChangeLogSet<? extends Entry>> expected = Collections.singletonList(changeLogSet);
        doReturn(expected).when(run).getChangeSets();

        List<ChangeLogSet<? extends Entry>> changes = RunScmChangeExtractor.getChanges(run);

        assertSame(expected, changes);
    }

    @Test
    void getChangesThrowsIllegalArgumentExceptionForUnsupportedRunType() {
        Run<?, ?> run = mock(Run.class);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> RunScmChangeExtractor.getChanges(run));
        assertTrue(ex.getMessage().startsWith("Unsupported Run type"));
    }

    @Test
    void getChangesThrowsIllegalStateExceptionWhenReflectionFails() {
        WorkflowRunLike run = mock(WorkflowRunLike.class);
        doThrow(new RuntimeException("reflection failure")).when(run).getChangeSets();

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> RunScmChangeExtractor.getChanges(run));
        assertEquals("cannot call getChangeSets", ex.getMessage());
    }

    @Test
    void getDependencyChangesReturnsMapForAbstractBuildWithPreviousBuild() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        AbstractBuild<?, ?> previousBuild = mock(AbstractBuild.class);
        doReturn(previousBuild).when(build).getPreviousCompletedBuild();

        AbstractProject project = mock(AbstractProject.class);
        DependencyChange change = mock(DependencyChange.class);
        Map<AbstractProject, DependencyChange> expected = Collections.singletonMap(project, change);
        doReturn(expected).when(build).getDependencyChanges(previousBuild);

        Map<AbstractProject, DependencyChange> actual = RunScmChangeExtractor.getDependencyChanges(build);

        assertSame(expected, actual);
    }

    @Test
    void getDependencyChangesReturnsEmptyMapWhenPreviousBuildNotAbstractBuild() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        doReturn(null).when(build).getPreviousCompletedBuild();

        Map<AbstractProject, DependencyChange> actual = RunScmChangeExtractor.getDependencyChanges(build);

        assertTrue(actual.isEmpty());
    }

    @Test
    void getDependencyChangesReturnsEmptyMapForNonAbstractBuild() {
        Run<?, ?> run = mock(Run.class);

        Map<AbstractProject, DependencyChange> actual = RunScmChangeExtractor.getDependencyChanges(run);

        assertTrue(actual.isEmpty());
    }
}
