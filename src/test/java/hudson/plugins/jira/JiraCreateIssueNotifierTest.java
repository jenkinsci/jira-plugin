package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Status;
import hudson.EnvVars;
import hudson.model.*;
import hudson.Launcher;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by warden on 17.09.15.
 */
public class JiraCreateIssueNotifierTest {

    private static final String JIRA_PROJECT = "PROJECT";
    private static final String COMPONENT = "some, componentA";
    private static final String ASSIGNEE = "user.name";
    private static final String DESCRIPTION = "Some description";

    List<Component> jiraComponents = new ArrayList<Component>();

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

        jiraComponents.add(new Component(null,null, "componentA", null, null));

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
        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString())).thenReturn(issue);

        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    public void testPerformFailureFailure() throws Exception {
        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, DESCRIPTION, ASSIGNEE, COMPONENT));
        doReturn(site).when(notifier).getSiteForProject((AbstractProject) Mockito.any());

        Issue issue = mock(Issue.class);
        Status status = new Status(null, null, "1","Open",null);
        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString())).thenReturn(issue);
        when(session.getIssueByKey(Mockito.anyString())).thenReturn(issue);
        when(issue.getStatus()).thenReturn(status);

        Assert.assertEquals(temporaryDirectory.list().length, 0);

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        Assert.assertEquals(temporaryDirectory.list().length, 1);

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        Assert.assertEquals(temporaryDirectory.list().length, 1);

//        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString())).thenReturn(issue);
        when(issue.getStatus()).thenReturn( new Status(null, null, "6","Closed",null));
        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        Assert.assertEquals(temporaryDirectory.list().length, 1);
    }

    @Test
    public void testPerformFailureSuccessIssueOpen() throws Exception {
        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", ""));
        doReturn(site).when(notifier).getSiteForProject((AbstractProject) Mockito.any());

        Issue issue = mock(Issue.class);
        Status status = new Status(null, null, "1","Open",null);
        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString())).thenReturn(issue);
        when(issue.getStatus()).thenReturn(status);
        when(session.getIssueByKey(Mockito.anyString())).thenReturn(issue);


        Assert.assertEquals(temporaryDirectory.list().length, 0);

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        Assert.assertEquals(temporaryDirectory.list().length, 1);

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.SUCCESS);
        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        Assert.assertEquals(temporaryDirectory.list().length, 1);
    }

    @Test
    public void testPerformFailureSuccessIssueClosedWithComponents() throws Exception {
        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", ""));
        doReturn(site).when(notifier).getSiteForProject((AbstractProject) Mockito.any());

        Issue issue = mock(Issue.class);
        Status status = new Status(null, null, "6","Closed",null);

        when(session.createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString())).thenReturn(issue);
        when(issue.getStatus()).thenReturn(status);
        when(session.getIssueByKey(Mockito.anyString())).thenReturn(issue);

        Assert.assertEquals(temporaryDirectory.list().length, 0);

        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);
        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        Assert.assertEquals(temporaryDirectory.list().length, 1);

        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(currentBuild.getResult()).thenReturn(Result.SUCCESS);
        Assert.assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        // file should be deleted
        Assert.assertEquals(temporaryDirectory.list().length, 0);
    }

}