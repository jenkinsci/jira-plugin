package hudson.plugins.jira.pipeline;

import static org.junit.Assert.assertEquals;

import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.plugins.jira.JiraSite;

public class FindIssuesAtCommitLogsStepTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void configRoundTripDefault() throws Exception {
        FindIssuesAtCommitLogsStep step = new StepConfigTester(jenkinsRule).configRoundTrip(new FindIssuesAtCommitLogsStep());
        assertEquals(JiraSite.DEFAULT_ISSUE_PATTERN.toString(), step.getIssuePattern());
    }
    
}
