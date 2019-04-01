package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class JiraIssueUpdateBuilderTest {

	private Launcher launcher;
	private FilePath workspace;
	private TaskListener listener;
	private AbstractBuild build;
	private EnvVars env;
	private AbstractProject project;
	private PrintStream logger;
	
	private Result result;
	private JiraSite site;
	
	@Before
	public void createMocks() throws IOException, InterruptedException {
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
        doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				result = (Result) args[0];
				return null;
			}
			
		}).when(build).setResult(Mockito.any());
	}
	
	@Test
	public void performNoSite() throws InterruptedException, IOException  {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(null).when(builder).getSiteForJob(Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.FAILURE));
	}
	
	@Test
	public void performTimeout() throws InterruptedException, IOException, TimeoutException {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(site).when(builder).getSiteForJob(Mockito.any());
		doThrow(new TimeoutException()).when(site).progressMatchingIssues(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (PrintStream) Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.FAILURE));
	}
	
	@Test
	public void performProgressFails() throws InterruptedException, IOException, TimeoutException {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(site).when(builder).getSiteForJob(Mockito.any());
		doReturn(false).when(site).progressMatchingIssues(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (PrintStream) Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.UNSTABLE));
	}
	
	@Test
	public void performProgressOK() throws InterruptedException, IOException, TimeoutException {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(site).when(builder).getSiteForJob(Mockito.any());
		doReturn(true).when(site).progressMatchingIssues(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (PrintStream) Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.SUCCESS));
	}
}
