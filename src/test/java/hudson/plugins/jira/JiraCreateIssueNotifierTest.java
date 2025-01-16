package hudson.plugins.jira;

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
import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

@WithJenkins
public class JiraCreateIssueNotifierTest {

    private static final String JIRA_PROJECT = "PROJECT";
    private static final String COMPONENT = "some, componentA";
    private static final String ASSIGNEE = "user.name";
    private static final String DESCRIPTION = "Some description";
    private static final String DESCRIPTION_PARAM = "${DESCRIPTION}";

    List<Component> jiraComponents = new ArrayList<>();

    Launcher launcher = mock(Launcher.class);
    BuildListener buildListener = mock(BuildListener.class);
    PrintStream logger = mock(PrintStream.class);
    JiraSite site = mock(JiraSite.class);
    JiraSession session = mock(JiraSession.class);
    EnvVars env;

    FreeStyleProject project = mock(FreeStyleProject.class);
    AbstractBuild previousBuild = mock(FreeStyleBuild.class);
    AbstractBuild currentBuild = mock(FreeStyleBuild.class);
    File temporaryDirectory;

    @TempDir
    public File temporaryFolder;

    @BeforeEach
    void createCommonMocks() throws IOException, InterruptedException {
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
    @WithoutJenkins
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
    @WithoutJenkins
    void performSuccessFailureWithEnv() throws Exception {
        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(currentBuild.getResult()).thenReturn(Result.FAILURE);

        JiraCreateIssueNotifier notifier =
                spy(new JiraCreateIssueNotifier(JIRA_PROJECT, DESCRIPTION_PARAM, "", "", 1L, 1L, 1));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);
        when(session.createIssue(
                        Mockito.anyString(),
                        contains(DESCRIPTION),
                        Mockito.anyString(),
                        Mockito.anyIterable(),
                        Mockito.anyString(),
                        Mockito.anyLong(),
                        Mockito.anyLong()))
                .thenReturn(issue);

        assertTrue(notifier.perform(currentBuild, launcher, buildListener));
    }

    @Test
    @WithoutJenkins
    void performFailureFailure() throws Exception {
        JiraCreateIssueNotifier notifier =
                spy(new JiraCreateIssueNotifier(JIRA_PROJECT, DESCRIPTION, ASSIGNEE, COMPONENT, 1L, 1L, 1));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);

        Status status = new Status(null, null, "1", "Open", null, null);
        when(session.createIssue(
                        Mockito.anyString(),
                        contains(DESCRIPTION),
                        Mockito.anyString(),
                        Mockito.anyIterable(),
                        Mockito.anyString(),
                        Mockito.anyLong(),
                        Mockito.anyLong()))
                .thenReturn(issue);
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

        when(issue.getStatus())
                .thenReturn(new Status(
                        null, null, "6", JiraCreateIssueNotifier.finishedStatuses.Closed.toString(), null, null));
        assertTrue(notifier.perform(currentBuild, launcher, buildListener));

        assertEquals(1, temporaryDirectory.list().length);
    }

    @Test
    @WithoutJenkins
    void performFailureSuccessIssueOpen() throws Exception {
        Long typeId = 1L;
        Long priorityId = 0L;
        Integer actionIdOnSuccess = 5;

        JiraCreateIssueNotifier notifier =
                spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", "", typeId, priorityId, actionIdOnSuccess));

        assertEquals(typeId, notifier.getTypeId());
        assertEquals(priorityId, notifier.getPriorityId());
        assertEquals(actionIdOnSuccess, notifier.getActionIdOnSuccess());

        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);
        Status status = new Status(null, null, "1", "Open", null, null);
        when(session.createIssue(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyIterable(),
                        Mockito.anyString(),
                        Mockito.eq(typeId),
                        Mockito.isNull()))
                .thenReturn(issue);
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
    @WithoutJenkins
    void performFailureSuccessIssueClosedWithComponents() throws Exception {
        JiraCreateIssueNotifier notifier = spy(new JiraCreateIssueNotifier(JIRA_PROJECT, "", "", "", 1L, 1L, 1));
        doReturn(site).when(notifier).getSiteForProject(Mockito.any());

        Issue issue = mock(Issue.class);
        Status status =
                new Status(null, null, JiraCreateIssueNotifier.finishedStatuses.Closed.toString(), null, null, null);

        when(session.createIssue(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyIterable(),
                        Mockito.anyString(),
                        Mockito.anyLong(),
                        Mockito.anyLong()))
                .thenReturn(issue);
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

    @Test
    @WithoutJenkins
    void isDone() {
        assertTrue(JiraCreateIssueNotifier.isDone(new Status(null, null, "Closed", null, null, null)));
        assertTrue(JiraCreateIssueNotifier.isDone(new Status(null, null, "Done", null, null, null)));
        assertTrue(JiraCreateIssueNotifier.isDone(new Status(null, null, "Resolved", null, null, null)));
        assertTrue(JiraCreateIssueNotifier.isDone(
                new Status(null, null, "Abandoned", null, null, new StatusCategory(null, "Done", null, "done", null))));
        assertFalse(JiraCreateIssueNotifier.isDone(
                new Status(null, null, "Abandoned", null, null, new StatusCategory(null, "ToDo", null, "todo", null))));
    }

    @Test
    void doFillPriorityIdItems(JenkinsRule j) throws Exception {

        String credId_1 = "cred-1-id";
        String credId_2 = "cred-2-id";

        String pwd1 = "pwd1";
        String pwd2 = "pwd2";

        UsernamePasswordCredentialsImpl cred1 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_1, null, "user1", pwd1);
        UsernamePasswordCredentialsImpl cred2 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_2, null, "user2", pwd2);

        SystemCredentialsProvider systemProvider = SystemCredentialsProvider.getInstance();
        systemProvider.getCredentials().add(cred1);
        systemProvider.save();

        { // test at project level
            URL url = new URL("https://pacific-ale.com.au");
            JiraSite jiraSite = mock(JiraSite.class);
            when(jiraSite.getUrl()).thenReturn(url);
            when(jiraSite.getCredentialsId()).thenReturn(credId_1);
            when(jiraSite.getName()).thenReturn(url.toExternalForm());
            JiraSession jiraSession = mock(JiraSession.class);
            when(jiraSession.getPriorities())
                    .thenReturn(Collections.singletonList(new Priority(null, 2L, "priority-1", null, null, null)));
            when(jiraSite.getSession(any())).thenReturn(jiraSession);

            JiraGlobalConfiguration.get().setSites(Collections.singletonList(jiraSite));

            FreeStyleProject p = j.jenkins.createProject(
                    FreeStyleProject.class, "p" + j.jenkins.getItems().size());
            ListBoxModel options = JiraCreateIssueNotifier.DESCRIPTOR.doFillPriorityIdItems(p);
            assertNotNull(options);
            assertThat(options.size(), Matchers.equalTo(2));
            assertThat(options.get(1).value, Matchers.equalTo("2"));
            assertThat(options.get(1).name, Matchers.containsString("priority-1"));
            assertThat(options.get(1).name, Matchers.containsString("https://pacific-ale.com.au"));
        }

        { // test at folder level
            Folder folder = j.jenkins.createProject(
                    Folder.class, "folder" + j.jenkins.getItems().size());

            CredentialsStore folderStore = JiraFolderPropertyTest.getFolderStore(folder);
            folderStore.addCredentials(Domain.global(), cred2);

            JiraFolderProperty foo = new JiraFolderProperty();

            JiraSite jiraSite = mock(JiraSite.class);
            URL url = new URL("https://pale-ale.com.au");
            when(jiraSite.getUrl()).thenReturn(url);
            when(jiraSite.getCredentialsId()).thenReturn(credId_2);
            when(jiraSite.getName()).thenReturn(url.toExternalForm());
            JiraSession jiraSession = mock(JiraSession.class);
            when(jiraSession.getPriorities())
                    .thenReturn(Collections.singletonList(new Priority(null, 3L, "priority-2", null, null, null)));
            when(jiraSite.getSession(any())).thenReturn(jiraSession);

            foo.setSites(Collections.singletonList(jiraSite));
            folder.getProperties().add(foo);

            ListBoxModel options = JiraCreateIssueNotifier.DESCRIPTOR.doFillPriorityIdItems(folder);
            assertNotNull(options);
            assertEquals(2, options.size());
            assertEquals("3", options.get(1).value);
            assertTrue(options.get(1).name.contains("priority-2"));
            assertTrue(options.get(1).name.contains("https://pale-ale.com.au"));
        }
    }

    @Test
    void doFillTypeItems(JenkinsRule j) throws Exception {

        String credId_1 = "cred-1-id";
        String credId_2 = "cred-2-id";

        String pwd1 = "pwd1";
        String pwd2 = "pwd2";

        UsernamePasswordCredentialsImpl cred1 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_1, null, "user1", pwd1);
        UsernamePasswordCredentialsImpl cred2 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_2, null, "user2", pwd2);

        SystemCredentialsProvider systemProvider = SystemCredentialsProvider.getInstance();
        systemProvider.getCredentials().add(cred1);
        systemProvider.save();

        { // test at project level
            URL url = new URL("https://pacific-ale.com.au");
            JiraSite jiraSite = mock(JiraSite.class);
            when(jiraSite.getUrl()).thenReturn(url);
            when(jiraSite.getCredentialsId()).thenReturn(credId_1);
            when(jiraSite.getName()).thenReturn(url.toExternalForm());
            JiraSession jiraSession = mock(JiraSession.class);
            when(jiraSession.getIssueTypes())
                    .thenReturn(Collections.singletonList(new IssueType(null, 1L, "type-1", true, null, null)));
            when(jiraSite.getSession(any())).thenReturn(jiraSession);

            JiraGlobalConfiguration.get().setSites(Collections.singletonList(jiraSite));

            FreeStyleProject p = j.jenkins.createProject(
                    FreeStyleProject.class, "p" + j.jenkins.getItems().size());
            ListBoxModel options = JiraCreateIssueNotifier.DESCRIPTOR.doFillTypeIdItems(p);
            assertNotNull(options);
            assertThat(options.size(), Matchers.equalTo(2));
            assertThat(options.get(1).value, Matchers.equalTo("1"));
            assertThat(options.get(1).name, Matchers.containsString("type-1"));
            assertThat(options.get(1).name, Matchers.containsString("https://pacific-ale.com.au"));
        }

        { // test at folder level
            Folder folder = j.jenkins.createProject(
                    Folder.class, "folder" + j.jenkins.getItems().size());

            CredentialsStore folderStore = JiraFolderPropertyTest.getFolderStore(folder);
            folderStore.addCredentials(Domain.global(), cred2);

            JiraFolderProperty foo = new JiraFolderProperty();

            JiraSite jiraSite = mock(JiraSite.class);
            URL url = new URL("https://pale-ale.com.au");
            when(jiraSite.getUrl()).thenReturn(url);
            when(jiraSite.getCredentialsId()).thenReturn(credId_2);
            when(jiraSite.getName()).thenReturn(url.toExternalForm());
            JiraSession jiraSession = mock(JiraSession.class);
            when(jiraSession.getIssueTypes())
                    .thenReturn(Collections.singletonList(new IssueType(null, 2L, "type-2", false, null, null)));
            when(jiraSite.getSession(any())).thenReturn(jiraSession);

            foo.setSites(Collections.singletonList(jiraSite));
            folder.getProperties().add(foo);

            ListBoxModel options = JiraCreateIssueNotifier.DESCRIPTOR.doFillTypeIdItems(folder);
            assertNotNull(options);
            assertEquals(2, options.size());
            assertEquals("2", options.get(1).value);
            assertTrue(options.get(1).name.contains("type-2"));
            assertTrue(options.get(1).name.contains("https://pale-ale.com.au"));
        }
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
