package hudson.plugins.jira.updater;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.Lists;

import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;

public class JqlIssueSelectorTest {

    private final static String TEST_JQL = "key='EXAMPLE-1'";

    @Test
    public void testDontDependOnRunAndTaskListener() throws IOException {
        JiraSession session = mock(JiraSession.class);
        JiraSite site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);

        JqlUpdaterIssueSelector jqlUpdaterIssueSelector = new JqlUpdaterIssueSelector(TEST_JQL);
        Set<String> findedIssueIds = jqlUpdaterIssueSelector.findIssueIds(null, site, null);
        assertThat(findedIssueIds, empty());
    }

    @Test
    public void testCallGetIssuesFromJqlSearch() throws IOException {
        JiraSession session = mock(JiraSession.class);
        Issue issue = mock(Issue.class);
        when(issue.getKey()).thenReturn("EXAMPLE-1");
        when(session.getIssuesFromJqlSearch(TEST_JQL)).thenReturn(Lists.newArrayList(issue));
        JiraSite site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);

        JqlUpdaterIssueSelector jqlUpdaterIssueSelector = new JqlUpdaterIssueSelector(TEST_JQL);
        Set<String> findedIssueIds = jqlUpdaterIssueSelector.findIssueIds(null, site, null);
        assertThat(findedIssueIds, hasSize(1));
        assertThat(findedIssueIds.iterator().next(), equalTo("EXAMPLE-1"));
    }

}
