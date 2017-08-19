package hudson.plugins.jira;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.TaskListener;

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
			
		}).when(build).setResult((Result) Mockito.any());
	}
	
	@Test
	public void testPerformNoSite() throws InterruptedException, IOException  {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(null).when(builder).getSiteForJob((Job<?,?>) Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.FAILURE));
	}
	
	@Test
	public void testPerformTimeout() throws InterruptedException, IOException, TimeoutException {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(site).when(builder).getSiteForJob((Job<?,?>) Mockito.any());
		doThrow(new TimeoutException()).when(site).progressMatchingIssues(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (PrintStream) Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.FAILURE));
	}
	
	@Test
	public void testPerformProgressFails() throws InterruptedException, IOException, TimeoutException {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(site).when(builder).getSiteForJob((Job<?,?>) Mockito.any());
		doReturn(false).when(site).progressMatchingIssues(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (PrintStream) Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.UNSTABLE));
	}
	
	@Test
	public void testPerformProgressOK() throws InterruptedException, IOException, TimeoutException {
		JiraIssueUpdateBuilder builder = spy(new JiraIssueUpdateBuilder(null, null, null));
		doReturn(site).when(builder).getSiteForJob((Job<?,?>) Mockito.any());
		doReturn(true).when(site).progressMatchingIssues(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (PrintStream) Mockito.any());
		builder.perform(build, workspace, launcher, listener);
		assertThat(result, is(Result.SUCCESS));
	}
}
