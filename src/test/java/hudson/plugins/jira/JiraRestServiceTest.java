package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.UserRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import hudson.plugins.jira.extension.ExtendedVersion;
import hudson.plugins.jira.extension.ExtendedVersionRestClient;
import io.atlassian.util.concurrent.Promise;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Every promise-backed call in {@link JiraRestService} follows the same contract on failure: the
 * caught exception itself (not its usually-null {@code getCause()}) becomes the cause of the thrown
 * {@link RestClientException}, and an {@link InterruptedException} re-interrupts the current thread
 * before it is wrapped. These tests exercise that contract for every such call site.
 */
class JiraRestServiceTest {

    private final URI JIRA_URI = URI.create("http://example.com:8080/");
    private final String USERNAME = "user";
    private final String PASSWORD = "password";

    private ExtendedJiraRestClient client;
    private IssueRestClient issueClient;
    private MetadataRestClient metadataClient;
    private ProjectRestClient projectClient;
    private SearchRestClient searchRestClient;
    private UserRestClient userClient;
    private ExtendedVersionRestClient extendedVersionRestClient;

    private JiraRestService service;

    @BeforeEach
    void createMocks() throws Exception {
        client = mock(ExtendedJiraRestClient.class);
        issueClient = mock(IssueRestClient.class);
        metadataClient = mock(MetadataRestClient.class);
        projectClient = mock(ProjectRestClient.class);
        searchRestClient = mock(SearchRestClient.class);
        userClient = mock(UserRestClient.class);
        extendedVersionRestClient = mock(ExtendedVersionRestClient.class);

        doReturn(issueClient).when(client).getIssueClient();
        doReturn(metadataClient).when(client).getMetadataClient();
        doReturn(projectClient).when(client).getProjectClient();
        doReturn(searchRestClient).when(client).getSearchClient();
        doReturn(userClient).when(client).getUserClient();
        doReturn(extendedVersionRestClient).when(client).getExtendedVersionRestClient();

        // getIssue() succeeds by default; tests that exercise progressWorkflowAction() and
        // getAvailableActions() rely on it, since both call getIssue() before their own REST call.
        Promise<Issue> issuePromise = mock(Promise.class);
        doReturn(mock(Issue.class)).when(issuePromise).get(anyLong(), any());
        doReturn(issuePromise).when(issueClient).getIssue(anyString());

        service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
    }

    @Test
    void baseApiPath() {
        assertEquals("/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());

        URI uri = URI.create("https://example.com/path/to/jira");
        JiraRestService withPath = new JiraRestService(uri, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/path/to/jira/" + JiraRestService.BASE_API_PATH, withPath.getBaseApiPath());
    }

    @Test
    void addCommentPreservesCauseAndInterruptFlag() throws Exception {
        Promise<Void> promise = mock(Promise.class);
        doReturn(promise).when(issueClient).addComment(any(), any());

        assertPreservesCauseAndInterruptFlag(promise, () -> service.addComment("KEY-1", "a comment", null, null));
    }

    @Test
    void getIssueWrapsGenericFailurePreservingCause() throws Exception {
        Promise<Issue> promise = mock(Promise.class);
        doReturn(promise).when(issueClient).getIssue("KEY-1");

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getIssue("KEY-1"));
    }

    @Test
    void getIssueNotFoundStillUsesTheWrappedRestClientExceptionAsCause() throws Exception {
        Promise<Issue> promise = mock(Promise.class);
        doReturn(promise).when(issueClient).getIssue("KEY-1");

        RestClientException notFound = new RestClientException((Throwable) null, 404);
        doThrow(new ExecutionException(notFound)).when(promise).get(anyLong(), any());

        RestClientException thrown = assertThrows(RestClientException.class, () -> service.getIssue("KEY-1"));
        assertSame(notFound, thrown.getCause());
    }

    @Test
    void getIssueTypesPreservesCauseAndInterruptFlag() throws Exception {
        Promise<Iterable> promise = mock(Promise.class);
        doReturn(promise).when(metadataClient).getIssueTypes();

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getIssueTypes());
    }

    @Test
    void getIssueTypesForProjectPreservesCauseAndInterruptFlag() throws Exception {
        Promise promise = mock(Promise.class);
        doReturn(promise).when(projectClient).getProject("KEY");

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getIssueTypes("KEY"));
    }

    @Test
    void getPrioritiesPreservesCauseAndInterruptFlag() throws Exception {
        Promise<Iterable> promise = mock(Promise.class);
        doReturn(promise).when(metadataClient).getPriorities();

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getPriorities());
    }

    @Test
    void getProjectsKeysPreservesCauseAndInterruptFlag() throws Exception {
        Promise<Iterable> promise = mock(Promise.class);
        doReturn(promise).when(projectClient).getAllProjects();

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getProjectsKeys());
    }

    @Test
    void getIssuesFromJqlSearchPreservesCauseAndInterruptFlag() throws Exception {
        Promise<SearchResult> promise = mock(Promise.class);
        doReturn(promise).when(searchRestClient).searchJql(any(), any(), anyInt(), any());

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getIssuesFromJqlSearch("*", null));
    }

    @Test
    void addVersionPreservesCauseAndInterruptFlag() throws Exception {
        Promise<ExtendedVersion> promise = mock(Promise.class);
        doReturn(promise).when(extendedVersionRestClient).createExtendedVersion(any());

        assertPreservesCauseAndInterruptFlag(promise, () -> service.addVersion("KEY", "1.0"));
    }

    @Test
    void releaseVersionPreservesCauseAndInterruptFlag() throws Exception {
        Promise<ExtendedVersion> promise = mock(Promise.class);
        doReturn(promise).when(extendedVersionRestClient).updateExtendedVersion(any(), any());

        ExtendedVersion version = new ExtendedVersion(
                URI.create("http://example.com:8080/version/100"), 100L, "1.0", "desc", false, false, null, null);

        assertPreservesCauseAndInterruptFlag(promise, () -> service.releaseVersion("KEY", version));
    }

    @Test
    void createIssuePreservesCauseAndInterruptFlag() throws Exception {
        Promise promise = mock(Promise.class);
        doReturn(promise).when(issueClient).createIssue(any());

        assertPreservesCauseAndInterruptFlag(
                promise,
                () -> service.createIssue("KEY", "description", null, Collections.emptyList(), "summary", 1L, null));
    }

    @Test
    void getUserWrapsGenericFailurePreservingCause() throws Exception {
        Promise promise = mock(Promise.class);
        doReturn(promise).when(userClient).getUser("bob");

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getUser("bob"));
    }

    @Test
    void getUserNotFoundStillUsesTheWrappedRestClientExceptionAsCause() throws Exception {
        Promise promise = mock(Promise.class);
        doReturn(promise).when(userClient).getUser("bob");

        RestClientException notFound = new RestClientException((Throwable) null, 404);
        doThrow(new ExecutionException(notFound)).when(promise).get(anyLong(), any());

        RestClientException thrown = assertThrows(RestClientException.class, () -> service.getUser("bob"));
        assertSame(notFound, thrown.getCause());
    }

    @Test
    void updateIssuePreservesCauseAndInterruptFlag() throws Exception {
        Promise promise = mock(Promise.class);
        doReturn(promise).when(issueClient).updateIssue(anyString(), any());

        assertPreservesCauseAndInterruptFlag(promise, () -> service.updateIssue("KEY-1", Collections.emptyList()));
    }

    @Test
    void setIssueLabelsPreservesCauseAndInterruptFlag() throws Exception {
        Promise promise = mock(Promise.class);
        doReturn(promise).when(issueClient).updateIssue(anyString(), any());

        assertPreservesCauseAndInterruptFlag(promise, () -> service.setIssueLabels("KEY-1", Collections.emptyList()));
    }

    @Test
    void setIssueFieldsPreservesCauseAndInterruptFlag() throws Exception {
        Promise promise = mock(Promise.class);
        doReturn(promise).when(issueClient).updateIssue(anyString(), any());

        assertPreservesCauseAndInterruptFlag(promise, () -> service.setIssueFields("KEY-1", Collections.emptyList()));
    }

    @Test
    void progressWorkflowActionPreservesCauseAndInterruptFlag() throws Exception {
        Promise<Void> promise = mock(Promise.class);
        doReturn(promise).when(issueClient).transition(any(Issue.class), any());

        assertPreservesCauseAndInterruptFlag(promise, () -> service.progressWorkflowAction("KEY-1", 5));
    }

    @Test
    void getAvailableActionsPreservesCauseAndInterruptFlag() throws Exception {
        Promise<Iterable> promise = mock(Promise.class);
        doReturn(promise).when(issueClient).getTransitions(any(Issue.class));

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getAvailableActions("KEY-1"));
    }

    @Test
    void getStatusesPreservesCauseAndInterruptFlag() throws Exception {
        Promise<Iterable> promise = mock(Promise.class);
        doReturn(promise).when(metadataClient).getStatuses();

        assertPreservesCauseAndInterruptFlag(promise, () -> service.getStatuses());
    }

    /**
     * Verifies both halves of the fix for a single promise-backed call: a plain failure is wrapped
     * with the original exception as its cause (not the exception's own, usually-null, cause), and an
     * {@link InterruptedException} additionally re-interrupts the calling thread before being wrapped.
     */
    private void assertPreservesCauseAndInterruptFlag(Promise<?> promise, Executable call) throws Exception {
        RestClientException failure = new RestClientException("boom", null);
        doThrow(failure).when(promise).get(anyLong(), any());

        RestClientException thrown = assertThrows(RestClientException.class, call);
        assertSame(
                failure,
                thrown.getCause(),
                "cause should be the caught exception itself, not its (usually null) getCause()");

        InterruptedException interrupted = new InterruptedException("interrupted while waiting on Jira");
        doThrow(interrupted).when(promise).get(anyLong(), any());

        RestClientException thrownOnInterrupt = assertThrows(RestClientException.class, call);
        assertSame(interrupted, thrownOnInterrupt.getCause());
        assertTrue(Thread.interrupted(), "InterruptedException must re-interrupt the current thread");

        TimeoutException timeout = new TimeoutException("timed out waiting on Jira");
        doThrow(timeout).when(promise).get(anyLong(), any());

        RestClientException thrownOnTimeout = assertThrows(RestClientException.class, call);
        assertSame(timeout, thrownOnTimeout.getCause());
    }
}
