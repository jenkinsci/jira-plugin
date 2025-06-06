package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void performTimeout() throws InterruptedException, IOException, TimeoutException {
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
        doReturn(site).when(builder).getSiteForJob(any());
        doThrow(new TimeoutException()).when(site).progressMatchingIssues(any(), any(), any(), any());
        builder.perform(build, workspace, launcher, listener);
        assertThat(result, is(Result.FAILURE));
    }

    @Test
    void performProgressFails() throws InterruptedException, IOException, TimeoutException {
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
        doReturn(site).when(builder).getSiteForJob(any());
        doReturn(false).when(site).progressMatchingIssues(anyString(), anyString(), anyString(), any());
        builder.perform(build, workspace, launcher, listener);
        assertThat(result, is(Result.UNSTABLE));
    }

    @Test
    void performProgressOK() throws InterruptedException, IOException, TimeoutException {
        JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
        doReturn(site).when(builder).getSiteForJob(any());
        doReturn(true).when(site).progressMatchingIssues(any(), any(), any(), any());
        builder.perform(build, workspace, launcher, listener);
        assertThat(result, is(Result.SUCCESS));
    }
}
