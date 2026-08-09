package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class JiraIssueUpdateBuilderTest {

    private Launcher launcher;
    private FilePath workspace;
    private TaskListener listener;
    private AbstractBuild build;
    private EnvVars env;
    private AbstractProject project;
    private PrintStream logger;

    private Result result;
    private JiraSite site;

    @BeforeEach
    void createMocks() throws IOException, InterruptedException {
        launcher = mock(Launcher.class);
        listener = mock(TaskListener.class);
        env = mock(EnvVars.class);
        project = mock(AbstractProject.class);
        logger = mock(PrintStream.class);
        build = mock(AbstractBuild.class);
        site = mock(JiraSite.class);

        when(build.getEnvironment(listener)).thenReturn(env);
        when(build.getParent()).thenReturn(project);

        when(listener.getLogger()).thenReturn(logger);
        // No variable substitution needed for these tests: expand() just echoes its input back,
        // so builder arguments can be asserted verbatim on the mocked progressMatchingIssues call.
        when(env.expand(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        result = Result.SUCCESS;
        doAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    result = (Result) args[0];
                    return null;
                })
                .when(build)
                .setResult(any());
    }

    @Test
    void performNoSite() throws InterruptedException, IOException {
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
        doReturn(null).when(builder).getSiteForJob(any());
        builder.perform(build, workspace, launcher, listener);
        assertThat(result, is(Result.FAILURE));
    }

    @Test
    void validateFailureResult() throws InterruptedException, IOException {
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
        Throwable throwable = mock(Throwable.class);
        doReturn(site).when(builder).getSiteForJob(any());
        doThrow(new RestClientException("Verify failure result", throwable))
                .when(site)
                .progressMatchingIssues(any(), any(), any(), any());
        builder.perform(build, workspace, launcher, listener);
        assertThat(result, is(Result.FAILURE));
    }

    @Test
    void performProgressFails() throws InterruptedException, IOException {
        String jql = "project = TEST";
        String action = "Resolve Issue";
        String comment = "closing via CI";
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(jql, action, comment));
        doReturn(site).when(builder).getSiteForJob(any());
        doReturn(false).when(site).progressMatchingIssues(anyString(), anyString(), anyString(), any());
        builder.perform(build, workspace, launcher, listener);
        assertThat(result, is(Result.UNSTABLE));
        // Not just "was progressMatchingIssues called" - the jql/action/comment configured on
        // the builder must be the exact ones that reach the site, unmangled.
        verify(site).progressMatchingIssues(jql, action, comment, logger);
    }

    @Test
    void performProgressOK() throws InterruptedException, IOException {
        String jql = "project = TEST";
        String action = "Resolve Issue";
        String comment = "closing via CI";
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(jql, action, comment));
        doReturn(site).when(builder).getSiteForJob(any());
        doReturn(true).when(site).progressMatchingIssues(any(), any(), any(), any());
        builder.perform(build, workspace, launcher, listener);
        assertThat(result, is(Result.SUCCESS));
        verify(site).progressMatchingIssues(jql, action, comment, logger);
    }

    @WithJenkins
    @Test
    void testPipelineWithJiraSite(JenkinsRule r) throws Exception {
        JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
        jiraGlobalConfiguration.setSites(Collections.singletonList(site));
        doReturn(true).when(site).progressMatchingIssues(anyString(), anyString(), anyString(), any());
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("""
                        jiraExecuteWorkflow(jqlSearch: 'search', workflowActionName: 'action', comment: 'comment')
                """, true));
        WorkflowRun b = r.buildAndAssertStatus(Result.SUCCESS, job);
        r.assertLogContains("[Jira] Updating issues using workflow action action.", b);
        verify(site).progressMatchingIssues(eq("search"), eq("action"), eq("comment"), any());
    }

    @Test
    void testIssueUpdateBuilderRestException() throws InterruptedException, IOException {
        Throwable throwable = mock(Throwable.class);
        PrintStream logger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(logger);
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
        doReturn(site).when(builder).getSiteForJob(any());
        doThrow(new RestClientException("[Jira] Jira REST progressMatchingIssues error. Cause: 401 error", throwable))
                .when(site)
                .progressMatchingIssues(any(), any(), any(), any());
        builder.perform(build, workspace, launcher, listener);
        verify(logger).println("[Jira] Jira REST progressMatchingIssues error. Cause: 401 error");
    }
}
