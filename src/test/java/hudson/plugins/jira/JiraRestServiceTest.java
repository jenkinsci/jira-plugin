package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;

import hudson.plugins.jira.extension.ExtendedJiraRestClient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class JiraRestServiceTest {

    private final URI JIRA_URI = URI.create("http://example.com:8080/");
    private final String USERNAME = "user";
    private final String PASSWORD = "password";
    private ExtendedJiraRestClient client;
    private SearchRestClient searchRestClient;
    private Promise searchResult;

    @Before
    public void createMocks() throws InterruptedException, ExecutionException, TimeoutException {
        client = mock(ExtendedJiraRestClient.class);
        searchRestClient = mock(SearchRestClient.class);
        searchResult = mock(Promise.class);

        when(client.getSearchClient()).thenReturn(searchRestClient);
        when(searchRestClient.searchJql(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anySetOf(String.class)))
            .thenReturn(searchResult);
        when(searchResult.get(Mockito.anyInt(), Mockito.any())).thenReturn(searchResult);
    }

    @Test
    public void baseApiPath() {
        JiraRestService service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());

        URI uri = URI.create("https://example.com/path/to/jira");
        service = new JiraRestService(uri, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/path/to/jira/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());
    }

    @Test(expected = TimeoutException.class)
    public void getIssuesFromJqlSearchTimeout() throws TimeoutException, InterruptedException, ExecutionException {
        JiraRestService service = spy(new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT));
        when(searchResult.get(Mockito.anyInt(), Mockito.any()))
            .thenThrow(new TimeoutException());
        service.getIssuesFromJqlSearch("*", null);
    }
    
    @Test(expected = Test.None.class)
    public void addCommentTest() throws TimeoutException, InterruptedException, ExecutionException {
        JiraRestService service = spy(new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT));
        String comment = "test comment";
        Promise<Void> promise = mock(Promise.class);
        IssueRestClient irc = Mockito.mock(IssueRestClient.class);
        when(client.getIssueClient()).thenReturn(irc);
        when(irc.addComment(Mockito.any(), Mockito.any())).thenReturn(promise);
        service.addComment("2", comment, "testGroup", "testRole");
        service.addComment("2", comment, null, "testRole");
        service.addComment("2", comment, null, null);
    }

    @Test(expected = Test.None.class)
    public void getProjectsKeysTest() throws TimeoutException, InterruptedException, ExecutionException, MalformedURLException, URISyntaxException {
        JiraRestService service = spy(new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT));
        BasicProject project1 = new BasicProject(new URL("http://jiraproject1.jira").toURI(), "key1", null, null);
        BasicProject project2 = new BasicProject(new URL("http://jiraproject2.jira").toURI(), "key2", 2L, "project2");
        List<BasicProject> projectList = new ArrayList<BasicProject>();
        projectList.add(project1);
        projectList.add(project2);
        Iterable<BasicProject> projects = projectList;
        ProjectRestClient prc = Mockito.mock(ProjectRestClient.class);
        when(client.getProjectClient()).thenReturn(prc);
        when(prc.getAllProjects()).thenReturn(Promises.promise(projects));
        List<String> projectKeys = service.getProjectsKeys();
        Assert.assertTrue(projectKeys.size() == 2);
        Assert.assertTrue(projectKeys.contains("key1"));
        Assert.assertTrue(projectKeys.contains("key2"));
    }
    
    @Test(expected = Test.None.class)
    public void createIssueTest() throws MalformedURLException, URISyntaxException {
        JiraRestService service = spy(new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT));
        IssueRestClient irc = Mockito.mock(IssueRestClient.class);
        BasicIssue bi = new BasicIssue(new URL("http://jiraissue1.jira").toURI(), "issueKey", 1L);
        List<String> list = new ArrayList<>();
        list.add("comp1");
        list.add("comp2");
        Iterable<String> components = list;
        when(client.getIssueClient()).thenReturn(irc);
        when(irc.createIssue(Mockito.any())).thenReturn(Promises.promise(bi));
        service.createIssue("projectKey", "description", "assignee", components, "summary", 1L, null);
        service.createIssue("projectKey", "description", "", components, "summary", 1L, 1L);
        Iterable<String> componentsEmpty = new ArrayList<>();
        service.createIssue("projectKey", "description", "", componentsEmpty, "summary", 1L, 1L);
    }
}
