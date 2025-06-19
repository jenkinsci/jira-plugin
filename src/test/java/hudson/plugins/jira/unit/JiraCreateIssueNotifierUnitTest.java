package hudson.plugins.jira.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.atlassian.jira.rest.client.api.StatusCategory;
import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Priority;
import com.atlassian.jira.rest.client.api.domain.Status;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.jira.JiraCreateIssueNotifier;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class JiraCreateIssueNotifierUnitTest {

    private static final String JIRA_PROJECT = "PROJECT";
    private static final String COMPONENT = "some, componentA";
    private static final String ASSIGNEE = "user.name";
    private static final String DESCRIPTION = "Some description";

    @TempDir
    File temporaryFolder;

    private JiraSite site;
    private JiraSession session;

    private FreeStyleProject project;
    private FreeStyleBuild currentBuild;
    private FreeStyleBuild previousBuild;

    private Launcher launcher;
    private BuildListener buildListener;

    private EnvVars env;
    private List<Component> jiraComponents;

    private File temporaryDirectory;
    private PrintStream logger;

    @BeforeEach
    void createCommonMocks() throws IOException, InterruptedException {
        site = mock(JiraSite.class);
        session = mock(JiraSession.class);
        project = mock(FreeStyleProject.class);
        currentBuild = mock(FreeStyleBuild.class);
        previousBuild = mock(FreeStyleBuild.class);
        launcher = mock(Launcher.class);
        buildListener = mock(BuildListener.class);
        logger = mock(PrintStream.class);

        jiraComponents = new ArrayList<>();

        env = new EnvVars();
        env.put("BUILD_NUMBER", "10");
        env.put("BUILD_URL", "/some/url/to/job");
        env.put("JOB_NAME", "Some job");
        env.put("DESCRIPTION", DESCRIPTION);

        jiraComponents.add(new Component(null, null, "componentA", null, null));

        when(currentBuild.getParent()).thenReturn(project);
        when(site.getSession(currentBuild.getParent())).thenReturn(session);
        when(site.getSession(previousBuild.getParent())).thenReturn(session);

        doReturn(env).when(currentBuild).getEnvironment(Mockito.any());

        temporaryDirectory = newFolder(temporaryFolder, "junit");

        when(project.getBuildDir()).thenReturn(temporaryDirectory);
        when(currentBuild.getProject()).thenReturn(project);
        when(currentBuild.getEnvironment(buildListener)).thenReturn(env);
        when(currentBuild.getPreviousCompletedBuild()).thenReturn(previousBuild);
        when(buildListener.getLogger()).thenReturn(logger);

        when(session.getComponents(Mockito.anyString())).thenReturn(jiraComponents);
    }

    @Test
    void performSuccessFailure() throws Exception {

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);

        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", "", 1L, 1L, 1));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);
        when(session.createIssue(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyIterable(),
                        Mockito.anyString(),
                        Mockito.anyLong(),
                        Mockito.anyLong()))
                .thenReturn(issue);

        assertTrue(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    void performSuccessFailureWithEnv() throws Exception {
        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);

        JiraCreateIssueNotifier notifier =
                spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", "", 1L, 1L, 1));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);
        when(session.createIssue(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyIterable(),
                        Mockito.anyString(),
                        Mockito.anyLong(),
                        Mockito.anyLong()))
                .thenReturn(issue);

        assertTrue(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    void performFailureFailure() throws Exception {
        JiraCreateIssueNotifier notifier =
                spy(new JiraCreateIssueNotifier(JIRA_PROJECT, DESCRIPTION, ASSIGNEE, COMPONENT, 1L, 1L, 1));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);

        assertFalse(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    void performFailureSuccessIssueOpen() throws Exception {
        Long typeId = 1L;
        Long priorityId = 0L;
        Integer actionIdOnSuccess = 5;

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.SUCCESS);

        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(
                JIRA_PROJECT, DESCRIPTION, ASSIGNEE, COMPONENT, typeId, priorityId, actionIdOnSuccess));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);
        when(issue.getKey()).thenReturn("TST-1");
        Status status = mock(Status.class);
        StatusCategory statusCategory = mock(StatusCategory.class);
        when(statusCategory.getKey()).thenReturn("new");
        when(status.getStatusCategory()).thenReturn(statusCategory);
        when(issue.getStatus()).thenReturn(status);

        when(session.getIssue("TST-1")).thenReturn(issue);
        when(session.existsIssue("TST-1")).thenReturn(true);

        assertTrue(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    void performFailureSuccessIssueClosedWithComponents() throws Exception {
        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", "", 1L, 1L, 1));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);
        when(session.createIssue(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyIterable(),
                        Mockito.anyString(),
                        Mockito.anyLong(),
                        Mockito.anyLong()))
                .thenReturn(issue);

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.SUCCESS);

        assertTrue(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    void isDone() {
        assertTrue(JiraCreateIssueNotifier.isDone(new Status(null, null, "Closed", null, null, null)));
        assertTrue(JiraCreateIssueNotifier.isDone(new Status(null, null, "Done", null, null, null)));
        assertTrue(JiraCreateIssueNotifier.isDone(new Status(null, null, "Resolved", null, null, null)));
        assertTrue(JiraCreateIssueNotifier.isDone(
                new Status(null, null, "Abandoned", null, null, new StatusCategory(null, "Done", null, "done", null))));
        assertFalse(JiraCreateIssueNotifier.isDone(
                new Status(null, null, "Abandoned", null, null, new StatusCategory(null, "ToDo", null, "todo", null))));
    }

    private File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + result);
        }
        return result;
    }
}