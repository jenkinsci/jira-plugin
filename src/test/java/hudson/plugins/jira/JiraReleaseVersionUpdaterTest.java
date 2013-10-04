package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.rpc.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JiraReleaseVersionUpdaterTest {
	private static final String JIRA_VER = Long.toString(System.currentTimeMillis());
	private static final String JIRA_PRJ = "TEST_PRJ";
	
    AbstractBuild build;
    Launcher launcher;
    BuildListener listener;
    EnvVars env;
    AbstractProject project;
    JiraSite site;
	
	@Before
	public void createMocks() {
		build = mock(AbstractBuild.class);;
		launcher = mock(Launcher.class);
		listener = mock(BuildListener.class);
		env = mock(EnvVars.class);
		project = mock(AbstractProject.class);
		site = mock(JiraSite.class);
	}
	
	@Test
	public void jiraApiCalledWithSpecifiedParameters() throws InterruptedException, IOException, ServiceException {
		JiraReleaseVersionUpdater jvu = spy(new JiraReleaseVersionUpdater(JIRA_PRJ, JIRA_VER));
		doReturn(site).when(jvu).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
		
		when(build.getProject()).thenReturn(project);
		when(build.getEnvironment(listener)).thenReturn(env);
		when(env.expand(Mockito.anyString())).thenReturn(JIRA_VER);
		when(site.getVersions(JIRA_PRJ)).thenReturn(new HashSet<JiraVersion>());
		
		Set<JiraVersion> existingVersions = new HashSet<JiraVersion>();
		existingVersions.add(new JiraVersion(JIRA_VER, null, false, false));
		when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);
		
		boolean result = jvu.perform(build, launcher, listener);
		verify(site).releaseVersion(JIRA_PRJ, JIRA_VER);
		assertThat(result, is(true));
	}	
	
	@Test
	public void buildDidNotFailWhenVersionExists() throws IOException, InterruptedException, ServiceException {
		JiraReleaseVersionUpdater jvu = spy(new JiraReleaseVersionUpdater(JIRA_PRJ, JIRA_VER));
		doReturn(site).when(jvu).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
		
		when(build.getProject()).thenReturn(project);
		when(build.getEnvironment(listener)).thenReturn(env);
		when(env.expand(Mockito.anyString())).thenReturn(JIRA_VER);
		
		Set<JiraVersion> existingVersions = new HashSet<JiraVersion>();
		existingVersions.add(new JiraVersion(JIRA_VER, null, true, false));
		
		when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);
		
		PrintStream logger = mock(PrintStream.class);
		when(listener.getLogger()).thenReturn(logger);
		
		boolean result = jvu.perform(build, launcher, listener);
		verify(site, times(0)).releaseVersion(JIRA_PRJ, JIRA_VER);
		assertThat(result, is(true));
	}
}
