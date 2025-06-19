package hudson.plugins.jira.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Priority;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleProject;
import hudson.plugins.jira.JiraCreateIssueNotifier;
import hudson.plugins.jira.JiraFolderProperty;
import hudson.plugins.jira.JiraFolderPropertyTest;
import hudson.plugins.jira.JiraGlobalConfiguration;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.util.ListBoxModel;
import java.net.URL;
import java.util.Collections;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class JiraCreateIssueNotifierIntegrationTest {

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
                    .thenReturn(Collections.singletonList(new IssueType(null, 4L, "issue-type-1", false, null, null)));
            when(jiraSite.getSession(any())).thenReturn(jiraSession);

            JiraGlobalConfiguration.get().setSites(Collections.singletonList(jiraSite));

            FreeStyleProject p = j.jenkins.createProject(
                    FreeStyleProject.class, "p" + j.jenkins.getItems().size());
            ListBoxModel options = JiraCreateIssueNotifier.DESCRIPTOR.doFillTypeItems(p);
            assertNotNull(options);
            assertThat(options.size(), Matchers.equalTo(2));
            assertThat(options.get(1).value, Matchers.equalTo("4"));
            assertThat(options.get(1).name, Matchers.containsString("issue-type-1"));
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
                    .thenReturn(Collections.singletonList(new IssueType(null, 5L, "issue-type-2", false, null, null)));
            when(jiraSite.getSession(any())).thenReturn(jiraSession);

            foo.setSites(Collections.singletonList(jiraSite));
            folder.getProperties().add(foo);

            ListBoxModel options = JiraCreateIssueNotifier.DESCRIPTOR.doFillTypeItems(folder);
            assertNotNull(options);
            assertEquals(2, options.size());
            assertEquals("5", options.get(1).value);
            assertTrue(options.get(1).name.contains("issue-type-2"));
            assertTrue(options.get(1).name.contains("https://pale-ale.com.au"));
        }
    }
}