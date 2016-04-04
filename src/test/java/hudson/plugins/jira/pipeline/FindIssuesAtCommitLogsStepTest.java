package hudson.plugins.jira.pipeline;

import static org.junit.Assert.assertEquals;

import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.google.inject.Inject;

public class FindIssuesAtCommitLogsStepTest {

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();
    
    @Inject
    FindIssuesAtCommitLogsStep.DescriptorImpl descriptor;

    @Before
    public void setUp() {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    public void configRoundTripDefault() throws Exception {
        FindIssuesAtCommitLogsStep step = new StepConfigTester(jenkinsRule).configRoundTrip(new FindIssuesAtCommitLogsStep());
        assertEquals(FindIssuesAtCommitLogsStep.DEFAULT_ISSUE_PATTERN, step.getIssuePattern());
    }
    
}
