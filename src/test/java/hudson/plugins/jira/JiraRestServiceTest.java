package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import io.atlassian.util.concurrent.Promise;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JiraRestServiceTest {

    private final URI JIRA_URI = URI.create("http://example.com:8080/");
    private final String USERNAME = "user";
    private final String PASSWORD = "password";
    private ExtendedJiraRestClient client;
    private SearchRestClient searchRestClient;
    private Promise promise;
    private SearchResult searchResult;

    @BeforeEach
    void createMocks() throws InterruptedException, ExecutionException, TimeoutException {
        client = mock(ExtendedJiraRestClient.class);
        searchRestClient = mock(SearchRestClient.class);
        promise = mock(Promise.class);
        searchResult = mock(SearchResult.class);

        doReturn(searchRestClient).when(client).getSearchClient();
        doReturn(promise).when(searchRestClient).searchJql(any(), any(), anyInt(), any());
        doReturn(searchResult).when(promise).get(anyLong(), any());
    }

    @Test
    void baseApiPath() {
        JiraRestService service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());

        URI uri = URI.create("https://example.com/path/to/jira");
        service = new JiraRestService(uri, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/path/to/jira/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());
    }

    @Test
    void getIssuesFromJqlSearchTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        JiraRestService service =
                spy(new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT));
        doThrow(new TimeoutException()).when(promise).get(Mockito.anyLong(), Mockito.any());
        assertThrows(TimeoutException.class, () -> service.getIssuesFromJqlSearch("*", null));
    }
}
