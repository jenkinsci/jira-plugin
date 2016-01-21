package hudson.plugins.jira.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.inject.Inject;

import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.pipeline.CommentStep.CommentStepExecution;

public class CommentStepTest {

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Inject
    CommentStep.DescriptorImpl descriptor;

    @Before
    public void setUp() {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    public void configRoundTrip() throws Exception {
        configRoundTrip("EXAMPLE-1", "comment");
    }

    private void configRoundTrip(String issueKey, String body) throws Exception {
        CommentStep configRoundTrip = new StepConfigTester(jenkinsRule)
                .configRoundTrip(new CommentStep(issueKey, body));

        assertEquals(issueKey, configRoundTrip.getIssueKey());
        assertEquals(body, configRoundTrip.getBody());
    }

    @Test
    public void testCallSessionAddComment() throws Exception {
        JiraSession session = mock(JiraSession.class);
        final String issueKey = "KEY";
        final String body = "dsgsags";

        final List<Object> assertCalledParams = new ArrayList<Object>();

        Mockito.doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String issueId = invocation.getArgumentAt(0, String.class);
                String comment = invocation.getArgumentAt(1, String.class);
                System.out.println("issueId: " + issueId);
                System.out.println("comment: " + comment);
                assertThat(issueId, equalTo(issueKey));
                assertThat(comment, equalTo(body));
                assertCalledParams.addAll(Arrays.asList(invocation.getArguments()));
                return null;
            }
        }).when(session).addComment(Mockito.<String> anyObject(), Mockito.<String> anyObject(),
                Mockito.<String> anyObject(), Mockito.<String> anyObject());
        JiraSite site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);

        Run mockRun = mock(Run.class);
        Job mockJob = mock(Job.class);
        when(mockRun.getParent()).thenReturn(mockJob);

        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);
        when(jiraProjectProperty.getSite()).thenReturn(site);
        when(mockJob.getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);

        Map<String, Object> r = new HashMap<String, Object>();
        r.put("issueKey", issueKey);
        r.put("body", body);
        CommentStep step = (CommentStep) descriptor.newInstance(r);

        StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(Run.class)).thenReturn(mockRun);

        assertThat(assertCalledParams, hasSize(0));

        CommentStepExecution start = (CommentStepExecution) step.start(ctx);
        start.run();

        assertThat(assertCalledParams, hasSize(4));
    }

}
