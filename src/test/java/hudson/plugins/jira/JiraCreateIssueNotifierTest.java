package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Status;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class JiraCreateIssueNotifierTest {

    private static final String JIRA_PROJECT = "PROJECT";
    private static final String COMPONENT = "some, componentA";
    private static final String ASSIGNEE = "user.name";
    private static final String DESCRIPTION = "Some description";

    List<Component> jiraComponents = new ArrayList<>();

    Launcher launcher = mock(Launcher.class);
    BuildListener buildListener = mock(BuildListener.class);
    PrintStream logger = mock(PrintStream.class);
    JiraSite site = mock(JiraSite.class);
    JiraSession session = mock(JiraSession.class);
    EnvVars env;

    AbstractProject project = mock(AbstractProject.class);
    AbstractBuild previousBuild = mock(FreeStyleBuild.class);
    AbstractBuild currentBuild = mock(FreeStyleBuild.class);
    File temporaryDirectory;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void createCommonMocks() throws IOException, InterruptedException {
        env = new EnvVars();
        env.put("BUILD_NUMBER", "10");
        env.put("BUILD_URL", "/some/url/to/job");
        env.put("JOB_NAME", "Some job");

        jiraComponents.add(new Component(null, null, "componentA", null, null));

        when(site.getSession()).thenReturn(session);

        doReturn(env).when(currentBuild).getEnvironment((TaskListener) Mockito.any());

        temporaryDirectory = temporaryFolder.newFolder();

        when(project.getBuildDir()).thenReturn(temporaryDirectory);
        when(currentBuild.getProject()).thenReturn(project);
        when(currentBuild.getEnvironment(buildListener)).thenReturn(env);
        when(currentBuild.getPreviousBuild()).thenReturn(previousBuild);
        when(buildListener.getLogger()).thenReturn(logger);

        when(session.getComponents(Mockito.anyString())).thenReturn(jiraComponents);
    }

    @Test
    public void testPerformSuccessFailure() throws Exception {

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);

        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", ""));
        doReturn(site).when(notifier).getSiteForProject((AbstractProject) Mockito.any());

        Issue issue = mock(Issue.class);
        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyLong())).thenReturn(issue);

        assertTrue(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    public void testPerformFailureFailure() throws Exception {
        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, DESCRIPTION, ASSIGNEE, COMPONENT));
        doReturn(site).when(notifier).getSiteForProject((AbstractProject) Mockito.any());

        Issue issue = mock(Issue.class);
        Status status = new Status(null, null, "1","Open",null);
        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyLong())).thenReturn(issue);
        when(session.getIssueByKey(Mockito.anyString())).thenReturn(issue);
        when(issue.getStatus()).thenReturn(status);

        assertEquals(0, temporaryDirectory.list().length);

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        assertEquals(1, temporaryDirectory.list().length);

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        assertEquals(1, temporaryDirectory.list().length);

        when(issue.getStatus()).thenReturn(new Status(null, null, "6", JiraCreateIssueNotifier.finishedStatuses.Closed.toString(), null));
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        assertEquals(1, temporaryDirectory.list().length);
    }

    @Test
    public void testPerformFailureSuccessIssueOpen() throws Exception {
        Long typeId = 1L;
        Long priorityId = 0L;
        Integer actionIdOnSuccess = 5;

        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", "", typeId, priorityId, actionIdOnSuccess));

        assertEquals(typeId, notifier.getTypeId());
        assertEquals(priorityId, notifier.getPriorityId());
        assertEquals(actionIdOnSuccess, notifier.getActionIdOnSuccess());

        doReturn(site).when(notifier).getSiteForProject((AbstractProject) Mockito.any());

        Issue issue = mock(Issue.class);
        Status status = new Status(null, null, "1", "Open", null);
        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString(),
                Mockito.eq(typeId), Mockito.isNull(Long.class))).thenReturn(issue);
        when(issue.getStatus()).thenReturn(status);
        when(session.getIssueByKey(Mockito.anyString())).thenReturn(issue);


        assertEquals(0, temporaryDirectory.list().length);

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        assertEquals(1, temporaryDirectory.list().length);

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.SUCCESS);
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        verify(session).progressWorkflowAction("null", actionIdOnSuccess);

        assertEquals(1, temporaryDirectory.list().length);
    }

    @Test
    public void testPerformFailureSuccessIssueClosedWithComponents() throws Exception {
        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", ""));
        doReturn(site).when(notifier).getSiteForProject((AbstractProject) Mockito.any());

        Issue issue = mock(Issue.class);
        Status status = new Status(null, null, JiraCreateIssueNotifier.finishedStatuses.Closed.toString() , null, null);

        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyLong())).thenReturn(issue);
        when(issue.getStatus()).thenReturn(status);
        when(session.getIssueByKey(Mockito.anyString())).thenReturn(issue);

        assertEquals(0, temporaryDirectory.list().length);

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        assertEquals(1, temporaryDirectory.list().length);

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.SUCCESS);
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        // file should be deleted
        assertEquals(0, temporaryDirectory.list().length);
    }

}
