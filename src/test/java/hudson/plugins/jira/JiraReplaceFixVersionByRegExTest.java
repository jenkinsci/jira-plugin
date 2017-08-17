package hudson.plugins.jira;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Version;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JiraReplaceFixVersionByRegExTest {

	private static final String PROJECT_KEY = "myKey";
	private static final String TO_VERSION = "toVersion";
	private static final String QUERY = "query";

	private JiraSession jiraSession = null;

	@Mock
	private JiraSite site;

	@Mock
	private JiraRestService service = null;

	@Before
	public void prepareMocks() throws IOException, InterruptedException {
		jiraSession = spy(new JiraSession(site, service));
	}

	@Test
	public void testReplaceWithFixVersionByRegex() throws URISyntaxException, TimeoutException {

		List<Version> myVersions = new ArrayList<Version>();
		myVersions.add(new Version(new URI("self"), 0L, TO_VERSION, null, false, false, null));
		when(jiraSession.getVersions(PROJECT_KEY)).thenReturn(myVersions);

		ArrayList<Issue> issues = new ArrayList<Issue>();
		issues.add(getIssue("abcXXXXefg", 1L));
		issues.add(getIssue("dgcXXXXefg", 2L));
		when(service.getIssuesFromJqlSearch(QUERY, Integer.MAX_VALUE)).thenReturn(issues);

		jiraSession.replaceFixVersion(PROJECT_KEY, "/abc.*efg/", TO_VERSION, QUERY);

		ArgumentCaptor<String> issueKeys = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List> versionList = ArgumentCaptor.forClass(List.class);
		verify(service, times(2)).updateIssue(issueKeys.capture(), versionList.capture());

		// First Issue, current FixVersion replaced by new one
		assertThat(issueKeys.getAllValues().get(0), equalTo(issues.get(0).getKey()));
		List firstIssueUpdatedFixVersions = versionList.getAllValues().get(0);
		assertThat(firstIssueUpdatedFixVersions.size(), equalTo(1));
		assertThat((Version) firstIssueUpdatedFixVersions.get(0), equalTo(myVersions.get(0)));

		// Second Issue, current FixVersion stays, new fixVersion added.
		assertThat(issueKeys.getAllValues().get(1), equalTo(issues.get(1).getKey()));
		List secondIssueUpdatedFixVersions = versionList.getAllValues().get(1);
		assertThat(secondIssueUpdatedFixVersions.size(), equalTo(2));
		assertThat(secondIssueUpdatedFixVersions.get(0), equalTo(CollectionUtils.get(issues.get(1).getFixVersions(), 0)));
		assertThat((Version) secondIssueUpdatedFixVersions.get(1), equalTo(myVersions.get(0)));
	}


	private Issue getIssue(String fixVersion, long id) throws URISyntaxException {
		List<Version> fixVersions = new ArrayList<Version>();
		fixVersions.add(new Version(new URI("self"), 0L, fixVersion, null, false, false, null));
		return new Issue("", new URI(""), PROJECT_KEY+id, id, null, null, null, null, null, null, null, null, null, null, null, null, null, fixVersions, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
	}
}
