package hudson.plugins.jira.pipeline;

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
 * Created by aleksandr on 14.06.16.
 */
public class IssueUpdateStepTest {
    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Inject
    IssueUpdateStep.DescriptorImpl descriptor;

    @Before
    public void setUp() {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    public void configRoundTrip() throws Exception {
        configRoundTrip("id=EXAMPLE-1", "Action 1", "comment");
    }

    private void configRoundTrip(String jqlSearch, String workflowActionName, String comment) throws Exception {
        IssueUpdateStep configRoundTrip = new StepConfigTester(jenkinsRule)
                .configRoundTrip(new IssueUpdateStep(jqlSearch, workflowActionName, comment));

        assertEquals(jqlSearch, configRoundTrip.getJqlSearch());
        assertEquals(workflowActionName, configRoundTrip.getWorkflowActionName());
        assertEquals(comment, configRoundTrip.getComment());
    }

    @Test
    public void testUpdateIssuesByJQL() throws Exception {
        JiraSession session = mock(JiraSession.class);
        JiraSite site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);

        final String jqlSearch = "id=KEY";
        final String workflowActionName = "Action 1";
        final String comment = "dsgsags";

        final List<Object> assertCalledParams = new ArrayList<Object>();

        Mockito.doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String jqlSearchFromArgs = invocation.getArgumentAt(0, String.class);
                String workflowActionNameFromArgs = invocation.getArgumentAt(1, String.class);
                String commentFromArgs = invocation.getArgumentAt(2, String.class);
                System.out.println("jqlSearch: " + jqlSearchFromArgs);
                System.out.println("workflowActionName: " + workflowActionNameFromArgs);
                System.out.println("comment: " + commentFromArgs);
                assertThat(jqlSearchFromArgs, equalTo(jqlSearch));
                assertThat(workflowActionNameFromArgs, equalTo(workflowActionName));
                assertThat(commentFromArgs, equalTo(comment));
                assertCalledParams.addAll(Arrays.asList(invocation.getArguments()));
                return null;
            }
        }).when(site).progressMatchingIssues(Mockito.<String> anyObject(), Mockito.<String> anyObject(),
                Mockito.<String> anyObject(), Mockito.<PrintStream> anyObject());

        Run mockRun = mock(Run.class);
        Job mockJob = mock(Job.class);
        when(mockRun.getParent()).thenReturn(mockJob);

        TaskListener mockTaskListener = mock(TaskListener.class);
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));

        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);
        when(jiraProjectProperty.getSite()).thenReturn(site);
        when(mockJob.getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);

        Map<String, Object> r = new HashMap<String, Object>();
        r.put("jqlSearch", jqlSearch);
        r.put("workflowActionName", workflowActionName);
        r.put("comment", comment);
        IssueUpdateStep step = (IssueUpdateStep) descriptor.newInstance(r);

        StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(Run.class)).thenReturn(mockRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);

        assertThat(assertCalledParams, hasSize(0));

        IssueUpdateStep.StepExecution start = (IssueUpdateStep.StepExecution) step.start(ctx);
        start.run();

        assertThat(assertCalledParams, hasSize(4));
    }
}
