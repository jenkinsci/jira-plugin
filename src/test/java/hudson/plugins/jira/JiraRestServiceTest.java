package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.util.concurrent.Promise;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
}
