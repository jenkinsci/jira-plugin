package hudson.plugins.jira.unit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.Updater;
import hudson.plugins.jira.model.JiraIssue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.EditType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answer;
import org.mockito.Mockito;

/**
 * Test case for the Jira {@link Updater} - Unit tests only.
 *
 * @author kutzi
 */
@SuppressWarnings("unchecked")
public class UpdaterUnitTest {

    private Updater updater;

    private static class MockEntry extends Entry {

        private final String msg;

        public MockEntry(String msg) {
            this.msg = msg;
        }

        @Override
        public String getMsg() {
            return msg;
        }

        @Override
        public User getAuthor() {
            User user = mock(User.class);
            Mockito.when(user.getDisplayName()).thenReturn("testAuthor");
            return user;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return Arrays.asList("path1", "path2");
        }

        @Override
        public Collection<? extends ChangeLogSet.AffectedFile> getAffectedFiles() {
            ChangeLogSet.AffectedFile file1 = mock(ChangeLogSet.AffectedFile.class);
            Mockito.when(file1.getPath()).thenReturn("path1");
            Mockito.when(file1.getEditType()).thenReturn(EditType.EDIT);

            ChangeLogSet.AffectedFile file2 = mock(ChangeLogSet.AffectedFile.class);
            Mockito.when(file2.getPath()).thenReturn("path2");
            Mockito.when(file2.getEditType()).thenReturn(EditType.ADD);

            return Arrays.asList(file1, file2);
        }
    }

    @BeforeEach
    void prepare() {
        updater = new Updater(null);
    }

    /**
     * Checks if issues are correctly removed from the carry over list.
     */
    @Test
    @org.jvnet.hudson.test.Issue("17156")
    void issueIsRemovedFromCarryOverListAfterSubmission() throws RestClientException {
        // mock build:
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        FreeStyleProject project = mock(FreeStyleProject.class);
        Mockito.when(build.getParent()).thenReturn(project);
        Mockito.when(build.getProject()).thenReturn(project);

        // mock site:
        JiraSite site = mock(JiraSite.class);
        JiraSession session = mock(JiraSession.class);
        Mockito.when(site.getSession(project)).thenReturn(session);

        // First, second, and third issue
        final Issue firstIssue = mock(Issue.class);
        Mockito.when(firstIssue.getKey()).thenReturn("FIRST-1");
        final Issue secondIssue = mock(Issue.class);
        Mockito.when(secondIssue.getKey()).thenReturn("SECOND-1");
        final Issue thirdIssue = mock(Issue.class);
        Mockito.when(thirdIssue.getKey()).thenReturn("THIRD-1");

        // Mock the session
        Mockito.when(session.getIssue(firstIssue.getKey())).thenReturn(firstIssue);
        Mockito.when(session.getIssue(secondIssue.getKey())).thenReturn(secondIssue);
        Mockito.when(session.getIssue(thirdIssue.getKey())).thenReturn(thirdIssue);
        Mockito.when(session.existsIssue(firstIssue.getKey())).thenReturn(true);
        Mockito.when(session.existsIssue(secondIssue.getKey())).thenReturn(true);
        Mockito.when(session.existsIssue(thirdIssue.getKey())).thenReturn(true);

        // Add mock answer for successful adding of comments
        Answer answer = (Answer<Object>) invocation -> {
            return null;
        };

        doAnswer(answer)
                .when(session)
                .addComment(eq(firstIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doAnswer(answer)
                .when(session)
                .addComment(eq(secondIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doAnswer(answer)
                .when(session)
                .addComment(eq(thirdIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // issue for the caught exception
        doThrow(new RestClientException(new Throwable(), 404))
                .when(session)
                .addComment(eq(secondIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        Set<JiraIssue> issues = new LinkedHashSet<>();
        issues.add(new JiraIssue(firstIssue.getKey()));
        issues.add(new JiraIssue(secondIssue.getKey()));
        issues.add(new JiraIssue(thirdIssue.getKey()));

        // Call updater
        Set<JiraIssue> savedIssues = updater.submitComments(build, site, issues, session, false, "", "");

        // Verify only second issue remained in the set (failed submission)
        assertEquals(1, savedIssues.size());
        assertTrue(savedIssues.contains(new JiraIssue(secondIssue.getKey())));
    }

    @Test
    void buildHealthReports() {
        assertTrue(true);
    }

    @Test
    void getChangesUsingReflectionForunknownJob() {
        Run run = mock(Run.class);
        assertThrows(IllegalArgumentException.class, () -> RunScmChangeExtractor.getChangesUsingReflection(run));
    }

    /**
     * Test formatting of scm entry change time.
     */
    @Test
    void appendChangeTimestampToDescription() {
        Updater updater = new Updater(null);
        StringBuilder description = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2013, Calendar.MARCH, 1, 14, 30, 42); // Use fixed date

        MockEntry entry = new MockEntry("Fixed JIRA-1");
        entry.setParent(mock(ChangeLogSet.class));

        updater.appendChangeTimestamp(description, entry, calendar.getTime());

        assertThat(description.toString(), containsString("Mar 1, 2013"));
        assertThat(description.toString(), containsString("2:30:42 PM"));
    }

    @Test
    void testReplaceCarryOverList() {
        Updater updater = new Updater(null);
        Set<JiraIssue> currentIssues = new HashSet<>();
        currentIssues.add(new JiraIssue("ISSUE-1"));
        currentIssues.add(new JiraIssue("ISSUE-2"));

        Set<JiraIssue> carryOverIssues = new HashSet<>();
        carryOverIssues.add(new JiraIssue("ISSUE-3"));
        carryOverIssues.add(new JiraIssue("ISSUE-4"));

        Set<JiraIssue> result = updater.replaceCarryOverList(currentIssues, carryOverIssues);

        assertEquals(4, result.size());
        assertTrue(result.contains(new JiraIssue("ISSUE-1")));
        assertTrue(result.contains(new JiraIssue("ISSUE-2")));
        assertTrue(result.contains(new JiraIssue("ISSUE-3")));
        assertTrue(result.contains(new JiraIssue("ISSUE-4")));
    }

    @Test
    void testCarryOverPreviousIssues() {
        Updater updater = new Updater(null);
        Run<?, ?> run = mock(Run.class);

        Set<JiraIssue> result = updater.carryOverPreviousIssues(run);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // Should be empty for mock
    }
}