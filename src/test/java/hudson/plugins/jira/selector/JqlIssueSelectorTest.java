package hudson.plugins.jira.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.Lists;

import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;

public class JqlIssueSelectorTest {

    private final static String TEST_JQL = "key='EXAMPLE-1'";

    private JiraSite site;
    private JiraSession session;

    @Before
    public void prepare() throws IOException {
        session = mock(JiraSession.class);
        site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);
    }

    @Test
    public void testDontDependOnRunAndTaskListener() {
        JqlIssueSelector jqlUpdaterIssueSelector = new JqlIssueSelector(TEST_JQL);
        Set<String> findedIssueIds = jqlUpdaterIssueSelector.findIssueIds(null, site, null);
        assertThat(findedIssueIds, empty());
    }

    @Test
    public void testCallGetIssuesFromJqlSearch() throws IOException, TimeoutException {
        Issue issue = mock(Issue.class);
        when(issue.getKey()).thenReturn("EXAMPLE-1");
        when(session.getIssuesFromJqlSearch(TEST_JQL)).thenReturn(Lists.newArrayList(issue));

        JqlIssueSelector jqlUpdaterIssueSelector = new JqlIssueSelector(TEST_JQL);
        Set<String> foundIssueIds = jqlUpdaterIssueSelector.findIssueIds(null, site, null);
        assertThat(foundIssueIds, hasSize(1));
        assertThat(foundIssueIds.iterator().next(), equalTo("EXAMPLE-1"));
    }

}
