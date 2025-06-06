package hudson.plugins.jira.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JqlIssueSelectorTest {

    private static final String TEST_JQL = "key='EXAMPLE-1'";

    @Mock
    private JiraSite site;

    @Mock
    private JiraSession session;

    @Mock
    private AbstractProject project;

    @Mock
    private Run run;

    @BeforeEach
    void prepare() throws IOException {
        when(run.getParent()).thenReturn(project);
        when(site.getSession(project)).thenReturn(session);
    }

    @Test
    void dontDependOnRunAndTaskListener() {
        JqlIssueSelector jqlUpdaterIssueSelector = new JqlIssueSelector(TEST_JQL);
        Set<String> foundIssues = jqlUpdaterIssueSelector.findIssueIds(run, site, null);
        assertThat(foundIssues, empty());
    }

    @Test
    void callGetIssuesFromJqlSearch() throws IOException, TimeoutException {
        Issue issue = mock(Issue.class);
        when(issue.getKey()).thenReturn("EXAMPLE-1");
        when(session.getIssuesFromJqlSearch(TEST_JQL)).thenReturn(Collections.singletonList(issue));

        JqlIssueSelector jqlUpdaterIssueSelector = new JqlIssueSelector(TEST_JQL);
        Set<String> foundIssueIds = jqlUpdaterIssueSelector.findIssueIds(run, site, null);
        assertThat(foundIssueIds, hasSize(1));
        assertThat(foundIssueIds.iterator().next(), equalTo("EXAMPLE-1"));
    }
}
