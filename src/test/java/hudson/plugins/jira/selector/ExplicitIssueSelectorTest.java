package hudson.plugins.jira.selector;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

public class ExplicitIssueSelectorTest {

    private final static String TEST_KEY = "EXAMPLE-1";

    @Test
    public void returnsExplicitCollections() {
        ExplicitIssueSelector jqlUpdaterIssueSelector = new ExplicitIssueSelector(Collections.singletonList(TEST_KEY));
        Set<String> foundIssueIds = jqlUpdaterIssueSelector.findIssueIds(null, null, null);
        assertThat(foundIssueIds, hasSize(1));
        assertThat(foundIssueIds.iterator().next(), equalTo(TEST_KEY));
    }
}
