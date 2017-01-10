package hudson.plugins.jira;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.PrintStream;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class JiraIssuesSizeEnvironmentVariableBuilderTest {
     
    private static final String JIRA_URL = "http://example.com";
    private static final String JQL_QUERY = "example jql query";
    private static final String ENV_VARIABLE_NAME = "JIRA_ISSUES_SIZE_ENV_VARIABLE";
    private static final Integer NUMBER_OF_ISSUES = 1;

    Run run;
    Launcher launcher;
    BuildListener listener;
    EnvVars env;
    AbstractProject project;
    JiraSite site;
    JiraSession session;
    PrintStream logger;

    @Before
    public void createMocks() throws IOException, InterruptedException {
        run = mock(Run.class);
        launcher = mock(Launcher.class);
        listener = mock(BuildListener.class);
        env = mock(EnvVars.class);
        project = mock(AbstractProject.class);
        site = mock(JiraSite.class);
        session = mock(JiraSession.class);
        logger = mock(PrintStream.class);

        when(site.getName()).thenReturn(JIRA_URL);

        when(listener.getLogger()).thenReturn(logger);
        
        when(run.getParent()).thenReturn(project);
        when(run.getEnvironment(listener)).thenReturn(env);
        when(site.getSession()).thenReturn(session);
    }
    
    @Test(expected = AbortException.class)
    public void testPerformWithNoSiteFailsBuild() throws InterruptedException, IOException {
        JiraIssuesSizeEnvironmentVariableBuilder builder = spy(new JiraIssuesSizeEnvironmentVariableBuilder("", ""));
        doReturn(null).when(builder).getSiteForProject((Job<?, ?>) Mockito.any());
        builder.perform(run, null, launcher, listener);
    }

    @Test(expected = AbortException.class)
    public void testPerformWithNoSessionFailsBuild() throws InterruptedException, IOException {
        JiraIssuesSizeEnvironmentVariableBuilder builder = spy(new JiraIssuesSizeEnvironmentVariableBuilder("", ""));
        doReturn(site).when(builder).getSiteForProject((Job<?, ?>) Mockito.any());
        doReturn(null).when(site).getSession();
        builder.perform(run, null, launcher, listener);
    }

    @Test
    public void testPerformAddsAction() throws InterruptedException, IOException {
        JiraIssuesSizeEnvironmentVariableBuilder builder = spy(new JiraIssuesSizeEnvironmentVariableBuilder(JQL_QUERY, ENV_VARIABLE_NAME));
        doReturn(site).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        doReturn(NUMBER_OF_ISSUES).when(builder).getNumberOfIssuesByJqlQuery(Mockito.anyString(), (JiraSite) Mockito.any());

        builder.perform(run, null, launcher, listener);

        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(run).addAction(captor.capture());

        assertThat(captor.getValue(),instanceOf(JiraIssuesSizeEnvironmentContributingAction.class));

        JiraIssuesSizeEnvironmentContributingAction action = (JiraIssuesSizeEnvironmentContributingAction)(captor.getValue());

        assertThat(action.getIssuesNumber(), is(NUMBER_OF_ISSUES));
        assertThat(action.getVariableName(), is(ENV_VARIABLE_NAME));
    }
}
