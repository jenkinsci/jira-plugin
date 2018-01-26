package hudson.plugins.jira;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
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

    @Test
    public void testSetScmPersists() {
        class TestSCM extends SCM {

            @Override
            public ChangeLogParser createChangeLogParser() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        }

        final JiraIssueUpdater updater = new JiraIssueUpdater(null, new TestSCM(), null);
        assertThat(updater.getScm(), instanceOf(TestSCM.class));
    }

    @Test
    public void testSetLabelsPersists() {
        List<String> testLabels = Arrays.asList("testLabel1", "testLabel2");

        final JiraIssueUpdater updater = new JiraIssueUpdater(null, null, testLabels);
        assertThat(updater.getLabels(), is(testLabels));
    }

}
