package hudson.plugins.jira.selector;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Run;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JqlIssueSelectorTest {

    private final static String TEST_JQL = "key='EXAMPLE-1'";

    @Mock private JiraSite site;
    @Mock private JiraSession session;
    @Mock private AbstractProject project;
    @Mock private Run run;

    @Before
    public void prepare() throws IOException {
        when(run.getParent()).thenReturn(project);
        when(site.getSession(project)).thenReturn(session);
    }

    @Test
    public void dontDependOnRunAndTaskListener() {
        JqlIssueSelector jqlUpdaterIssueSelector = new JqlIssueSelector(TEST_JQL);
        Set<String> findedIssueIds = jqlUpdaterIssueSelector.findIssueIds(run, site, null);
        assertThat(findedIssueIds, empty());
    }

    @Test
    public void callGetIssuesFromJqlSearch() throws IOException, TimeoutException {
        Issue issue = mock(Issue.class);
        when(issue.getKey()).thenReturn("EXAMPLE-1");
        when(session.getIssuesFromJqlSearch(TEST_JQL)).thenReturn( Collections.singletonList(issue));

        JqlIssueSelector jqlUpdaterIssueSelector = new JqlIssueSelector(TEST_JQL);
        Set<String> foundIssueIds = jqlUpdaterIssueSelector.findIssueIds(run, site, null);
        assertThat(foundIssueIds, hasSize(1));
        assertThat(foundIssueIds.iterator().next(), equalTo("EXAMPLE-1"));
    }

}
