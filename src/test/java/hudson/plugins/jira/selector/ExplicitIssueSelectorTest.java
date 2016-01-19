package hudson.plugins.jira.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;

public class ExplicitIssueSelectorTest {

    private final static String TEST_KEY = "EXAMPLE-1";

    @Test
    public void testReturnsExplicitCollections() throws IOException {
        JiraSession session = mock(JiraSession.class);
        JiraSite site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);

        ExplicitIssueSelector jqlUpdaterIssueSelector = new ExplicitIssueSelector(
                Lists.newArrayList(TEST_KEY));
        Set<String> foundIssueIds = jqlUpdaterIssueSelector.findIssueIds(null, site, null);
        assertThat(foundIssueIds, hasSize(1));
        assertThat(foundIssueIds.iterator().next(), equalTo(TEST_KEY));
    }

}
