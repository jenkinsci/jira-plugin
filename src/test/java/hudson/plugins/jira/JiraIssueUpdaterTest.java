package hudson.plugins.jira;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;

import java.util.Set;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class JiraIssueUpdaterTest {

    @Test
    public void testIssueSelectorDefaultsToDefault() {
        final JiraIssueUpdater updater = new JiraIssueUpdater(null, null, null);
        assertThat(updater.getIssueSelector(), instanceOf(DefaultIssueSelector.class));
    }

    @Test
    public void testSetIssueSelectorPersists() {
        class TestSelector extends AbstractIssueSelector {

            @Override
            public Set<String> findIssueIds(Run<?, ?> run, JiraSite site, TaskListener listener) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        }

        final JiraIssueUpdater updater = new JiraIssueUpdater(new TestSelector(), null, null);
        assertThat(updater.getIssueSelector(), instanceOf(TestSelector.class));
    }

}
