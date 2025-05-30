package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Version;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JiraSessionTest {

    private static final String PROJECT_KEY = "myKey";
    private static final String QUERY = "query";

    private JiraSession jiraSession = null;

    @Mock
    private JiraSite site;

    @Mock
    private final JiraRestService service = null;

    @BeforeEach
    void prepareMocks() throws IOException, InterruptedException {
        jiraSession = spy(new JiraSession(site, service));
    }

    @Test
    void replaceWithFixVersionByRegex() throws URISyntaxException, TimeoutException {
        final ExtendedVersion newVersion =
                new ExtendedVersion(new URI("self"), 3L, "v3.0", null, false, false, null, null);
        List<ExtendedVersion> myVersions = new ArrayList<>();
        myVersions.add(newVersion);
        when(jiraSession.getVersions(PROJECT_KEY)).thenReturn(myVersions);

        ArrayList<Issue> issues = new ArrayList<>();
        issues.add(getIssue(Arrays.asList("v1.0"), 1L));
        issues.add(getIssue(Arrays.asList("v1.0", "v2.0", "v2.0.0"), 2L));
        when(service.getIssuesFromJqlSearch(QUERY, JiraSite.MAX_ALLOWED_ISSUES_FROM_JQL)).thenReturn(issues);

        jiraSession.replaceFixVersion(PROJECT_KEY, "/v1.*/", newVersion.getName(), QUERY);

        ArgumentCaptor<String> issueKeys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> versionList = ArgumentCaptor.forClass(List.class);
        verify(service, times(issues.size())).updateIssue(issueKeys.capture(), versionList.capture());

        // First Issue, current FixVersion replaced by new one
        assertThat(issueKeys.getAllValues().get(0), equalTo(issues.get(0).getKey()));
        List<ExtendedVersion> firstIssueUpdatedFixVersions =
                versionList.getAllValues().get(0);
        assertThat(firstIssueUpdatedFixVersions.size(), equalTo(1));
        assertThat(firstIssueUpdatedFixVersions.get(0).getName(), equalTo(newVersion.getName()));

        // Second Issue, current FixVersion stays, new fixVersion added.
        assertThat(issueKeys.getAllValues().get(1), equalTo(issues.get(1).getKey()));
        List<ExtendedVersion> secondIssueUpdatedFixVersions =
                versionList.getAllValues().get(1);
        assertThat(secondIssueUpdatedFixVersions.size(), equalTo(3));

        // Check that the collection contains versions with these names in any order
        assertThat(secondIssueUpdatedFixVersions, hasItem(hasProperty("name", equalTo(newVersion.getName()))));
        assertThat(secondIssueUpdatedFixVersions, hasItem(hasProperty("name", equalTo("v2.0"))));
        assertThat(secondIssueUpdatedFixVersions, hasItem(hasProperty("name", equalTo("v2.0.0"))));
    }

    @Test
    void replaceFixVersion() throws URISyntaxException, TimeoutException {
        final ExtendedVersion newVersion =
                new ExtendedVersion(new URI("self"), 3L, "v3.0", null, false, false, null, null);
        List<ExtendedVersion> myVersions = new ArrayList<>();
        myVersions.add(newVersion);
        when(jiraSession.getVersions(PROJECT_KEY)).thenReturn(myVersions);

        ArrayList<Issue> issues = new ArrayList<>();
        issues.add(getIssue(Arrays.asList("v1.0"), 1L));
        issues.add(getIssue(Arrays.asList("v1.0", "v1.0.0", "v2.0.0"), 2L));
        issues.add(getIssue(null, 3L));
        when(service.getIssuesFromJqlSearch(QUERY, JiraSite.MAX_ALLOWED_ISSUES_FROM_JQL)).thenReturn(issues);

        jiraSession.replaceFixVersion(PROJECT_KEY, "v1.0", newVersion.getName(), QUERY);

        ArgumentCaptor<String> issueKeys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> versionList = ArgumentCaptor.forClass(List.class);
        verify(service, times(issues.size())).updateIssue(issueKeys.capture(), versionList.capture());

        // First Issue, current FixVersion replaced by new one
        assertThat(issueKeys.getAllValues().get(0), equalTo(issues.get(0).getKey()));
        List<Version> firstIssueUpdatedFixVersions = versionList.getAllValues().get(0);
        assertThat(firstIssueUpdatedFixVersions.size(), equalTo(1));
        assertThat(firstIssueUpdatedFixVersions.get(0), equalTo(newVersion));

        // Second Issue, current FixVersion stays, new fixVersion added.
        assertThat(issueKeys.getAllValues().get(1), equalTo(issues.get(1).getKey()));
        List<Version> secondIssueUpdatedFixVersions = versionList.getAllValues().get(1);
        assertThat(secondIssueUpdatedFixVersions.size(), equalTo(3));
        assertThat(secondIssueUpdatedFixVersions, hasItem(hasProperty("name", equalTo(newVersion.getName()))));
        assertThat(secondIssueUpdatedFixVersions, hasItem(hasProperty("name", equalTo("v1.0.0"))));
        assertThat(secondIssueUpdatedFixVersions, hasItem(hasProperty("name", equalTo("v2.0.0"))));

        // Third Issue, no FixVersion, new fixVersion added.
        List<Version> thirdIssueVersions = versionList.getAllValues().get(2);
        assertThat(thirdIssueVersions.size(), equalTo(1));
        assertThat(thirdIssueVersions.get(0), equalTo(newVersion));
    }

    private Issue getIssue(List<String> versions, long id) throws URISyntaxException {
        List<Version> fixVersions = null;
        if (versions != null) {
            fixVersions = new ArrayList<>();
            for (String fixVersion : versions) {
                fixVersions.add(new Version(
                        new URI("self"), ThreadLocalRandom.current().nextLong(), fixVersion, null, false, false, null));
            }
        }

        return new Issue(
                "",
                new URI(""),
                PROJECT_KEY + id,
                ThreadLocalRandom.current().nextLong(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                fixVersions,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void shouldAddVersionToAllIssues() throws Exception {
        final ExtendedVersion newVersion =
                new ExtendedVersion(new URI("self"), 10L, "v4.0", null, false, false, null, null);
        List<ExtendedVersion> myVersions = List.of(newVersion);
        when(jiraSession.getVersions(PROJECT_KEY)).thenReturn(myVersions);

        List<Issue> issues = new ArrayList<>();
        issues.add(getIssue(Arrays.asList("v1.0"), 1L));
        issues.add(getIssue(Arrays.asList("v2.0", "v3.0"), 2L));
        when(service.getIssuesFromJqlSearch(QUERY, JiraSite.MAX_ALLOWED_ISSUES_FROM_JQL)).thenReturn(issues);

        jiraSession.addFixVersion(PROJECT_KEY, newVersion.getName(), QUERY);

        ArgumentCaptor<String> issueKeys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> versionList = ArgumentCaptor.forClass(List.class);
        verify(service, times(issues.size())).updateIssue(issueKeys.capture(), versionList.capture());

        // First issue: should have original + new version
        List<Version> firstIssueVersions = versionList.getAllValues().get(0);
        assertThat(firstIssueVersions, hasItem(hasProperty("name", equalTo("v1.0"))));
        assertThat(firstIssueVersions, hasItem(hasProperty("name", equalTo("v4.0"))));
        assertThat(firstIssueVersions.size(), equalTo(2));

        // Second issue: should have both original + new version
        List<Version> secondIssueVersions = versionList.getAllValues().get(1);
        assertThat(secondIssueVersions, hasItem(hasProperty("name", equalTo("v2.0"))));
        assertThat(secondIssueVersions, hasItem(hasProperty("name", equalTo("v3.0"))));
        assertThat(secondIssueVersions, hasItem(hasProperty("name", equalTo("v4.0"))));
        assertThat(secondIssueVersions.size(), equalTo(3));
    }

    @Test
    void shouldAddVersionWhenNoFixVersions() throws Exception {
        final ExtendedVersion newVersion =
                new ExtendedVersion(new URI("self"), 11L, "v5.0", null, false, false, null, null);
        List<ExtendedVersion> myVersions = List.of(newVersion);
        when(jiraSession.getVersions(PROJECT_KEY)).thenReturn(myVersions);

        List<Issue> issues = List.of(getIssue(null, 3L));
        when(service.getIssuesFromJqlSearch(QUERY, JiraSite.MAX_ALLOWED_ISSUES_FROM_JQL)).thenReturn(issues);

        jiraSession.addFixVersion(PROJECT_KEY, newVersion.getName(), QUERY);

        ArgumentCaptor<String> issueKeys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> versionList = ArgumentCaptor.forClass(List.class);
        verify(service).updateIssue(issueKeys.capture(), versionList.capture());

        List<Version> updatedVersions = versionList.getValue();
        assertThat(updatedVersions.size(), equalTo(1));
        assertThat(updatedVersions.get(0).getName(), equalTo("v5.0"));
    }

    @Test
    void shouldNotCallUpdateIfNoIssues() throws Exception {
        final ExtendedVersion newVersion =
                new ExtendedVersion(new URI("self"), 12L, "v6.0", null, false, false, null, null);
        List<ExtendedVersion> myVersions = List.of(newVersion);
        when(jiraSession.getVersions(PROJECT_KEY)).thenReturn(myVersions);

        when(service.getIssuesFromJqlSearch(QUERY, JiraSite.MAX_ALLOWED_ISSUES_FROM_JQL)).thenReturn(new ArrayList<>());

        jiraSession.addFixVersion(PROJECT_KEY, newVersion.getName(), QUERY);

        verify(service, times(0)).updateIssue(anyString(), anyList());
    }

    @Test
    void shouldReturnIfVersionNotFound() throws Exception {
        when(jiraSession.getVersions(PROJECT_KEY)).thenReturn(List.of());

        jiraSession.addFixVersion(PROJECT_KEY, "nonexistent", QUERY);

        verify(service, times(0)).getIssuesFromJqlSearch(anyString(), anyInt());
        verify(service, times(0)).updateIssue(anyString(), anyList());
    }
}
