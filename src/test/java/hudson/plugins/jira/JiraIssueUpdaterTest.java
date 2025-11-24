package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class JiraIssueUpdaterTest {

    @Test
    void issueSelectorDefaultsToDefault() {
        final JiraIssueUpdater updater = new JiraIssueUpdater(null, null, null);
        assertThat(updater.getIssueSelector(), instanceOf(DefaultIssueSelector.class));
    }

    @Test
    void setIssueSelectorPersists() {
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
    void setScmPersists() {
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
    void setLabelsPersists() {
        List<String> testLabels = Arrays.asList("testLabel1", "testLabel2");

        final JiraIssueUpdater updater = new JiraIssueUpdater(null, null, testLabels);
        assertThat(updater.getLabels(), is(testLabels));
    }

    @WithJenkins
    @Test
    void testPipeline(JenkinsRule r) throws Exception {
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(
                """
                        jiraCommentIssues(issueSelector: DefaultSelector(), scm: null)
                """,
                true));
        WorkflowRun b = r.buildAndAssertStatus(Result.FAILURE, job);
        r.assertLogContains(" Unsupported run type", b);
    }
}
