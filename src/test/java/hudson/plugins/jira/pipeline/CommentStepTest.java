package hudson.plugins.jira.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.pipeline.CommentStep.CommentStepExecution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

@WithJenkins
class CommentStepTest {

    private JenkinsRule jenkinsRule;

    @Inject
    CommentStep.DescriptorImpl descriptor;

    @BeforeEach
    void setUp(JenkinsRule jenkinsRule) {
        this.jenkinsRule = jenkinsRule;
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    void configRoundTrip() throws Exception {
        configRoundTrip("EXAMPLE-1", "comment");
    }

    private void configRoundTrip(String issueKey, String body) throws Exception {
        CommentStep configRoundTrip =
                new StepConfigTester(jenkinsRule).configRoundTrip(new CommentStep(issueKey, body));

        assertEquals(issueKey, configRoundTrip.getIssueKey());
        assertEquals(body, configRoundTrip.getBody());
    }

    @Test
    void callSessionAddComment() throws Exception {
        JiraSession session = mock(JiraSession.class);
        final String issueKey = "KEY";
        final String body = "dsgsags";

        AbstractProject mockProject = mock(FreeStyleProject.class);
        Run mockRun = mock(Run.class);
        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);
        JiraSite site = mock(JiraSite.class);

        when(jiraProjectProperty.getSite()).thenReturn(site);
        when(site.getSession(mockProject)).thenReturn(session);
        when(mockRun.getParent()).thenReturn(mockProject);
        when(mockRun.getParent().getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);

        final List<Object> assertCalledParams = new ArrayList<>();

        Mockito.doAnswer(invocation -> {
                    String issueId = invocation.getArgument(0, String.class);
                    String comment = invocation.getArgument(1, String.class);
                    System.out.println("issueId: " + issueId);
                    System.out.println("comment: " + comment);
                    assertThat(issueId, equalTo(issueKey));
                    assertThat(comment, equalTo(body));
                    assertCalledParams.addAll(Arrays.asList(invocation.getArguments()));
                    return null;
                })
                .when(session)
                .addComment(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Map<String, Object> r = new HashMap<>();
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
