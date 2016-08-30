package hudson.plugins.jira.pipeline;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.google.inject.Inject;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.PrintStream;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by aleksandr on 04.07.16.
 */
public class IssueWorkflowActionStepTest {
    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Inject
    IssueWorkflowActionStep.DescriptorImpl descriptor;

    @Before
    public void setUp() {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    public void configRoundTrip() throws Exception {
        configRoundTrip("EXAMPLE-1", "action 1");
    }

    private void configRoundTrip(String issueKey, String workflowActionName) throws Exception {
        IssueWorkflowActionStep configRoundTrip = new StepConfigTester(jenkinsRule)
                .configRoundTrip(new IssueWorkflowActionStep(issueKey, workflowActionName));

        assertEquals(issueKey, configRoundTrip.getIssueKey());
        assertEquals(workflowActionName, configRoundTrip.getWorkflowActionName());
    }

    @Test
    public void testProgressIssue() throws Exception {
        JiraSession session = mock(JiraSession.class);
        JiraSite site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);

        final String issueKey = "EXAMPLE-1";
        final String workflowActionName = "action 1";
        final long originalActionId = 1L;
        final long targetActionId = 2L;

        final List<Object> assertCalledParams = new ArrayList<Object>();

        Mockito.doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String issueKeyFromArgs = invocation.getArgumentAt(0, String.class);
                Integer actionIdFromArgs = invocation.getArgumentAt(1, Integer.class);
                System.out.println("issueKey: " + issueKeyFromArgs);
                System.out.println("actionId: " + actionIdFromArgs);
                assertThat(issueKeyFromArgs, equalTo(issueKey));
                assertThat(actionIdFromArgs, equalTo(Long.valueOf(targetActionId).intValue()));
                assertCalledParams.addAll(Arrays.asList(invocation.getArguments()));
                return null;
            }
        }).when(session).progressWorkflowAction(
                Mockito.<String> anyObject(),
                Mockito.<Integer> anyObject()
        );

        Run mockRun = mock(Run.class);
        Job mockJob = mock(Job.class);
        when(mockRun.getParent()).thenReturn(mockJob);

        TaskListener mockTaskListener = mock(TaskListener.class);
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));

        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);
        when(jiraProjectProperty.getSite()).thenReturn(site);
        when(mockJob.getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);

        when(session.getActionIdForIssue(Mockito.eq(issueKey), Mockito.eq(workflowActionName))).thenReturn(2);

        Issue issue = mock(Issue.class);
        Status status = mock(Status.class);
        when(session.getIssue(Mockito.eq(issueKey))).thenReturn(issue);
        when(issue.getStatus()).thenReturn(status);

        when(status.getId()).then(new Answer<Long>() {
            private boolean firstTime = true;

            @Override
            public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (firstTime) {
                    firstTime = false;
                    return originalActionId;
                }
                return targetActionId;
            }
        });

        Map<String, Object> r = new HashMap<String, Object>();
        r.put("issueKey", issueKey);
        r.put("workflowActionName", workflowActionName);
        IssueWorkflowActionStep step = (IssueWorkflowActionStep) descriptor.newInstance(r);

        StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(Run.class)).thenReturn(mockRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);

        assertThat(assertCalledParams, hasSize(0));

        IssueWorkflowActionStep.StepExecution start = (IssueWorkflowActionStep.StepExecution) step.start(ctx);
        start.run();

        assertThat(assertCalledParams, hasSize(2));
    }
}
