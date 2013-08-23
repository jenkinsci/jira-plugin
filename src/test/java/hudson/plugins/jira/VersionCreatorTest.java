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

import org.junit.Test;
import org.mockito.Mockito;

/**
 * 
 * @author Artem Koshelev artkoshelev@gmail.com
 *
 */
public class VersionCreatorTest {
	AbstractBuild build = mock(AbstractBuild.class);;
	Launcher launcher = mock(Launcher.class);
	BuildListener listener = mock(BuildListener.class);
	EnvVars env = mock(EnvVars.class);
	AbstractProject project = mock(AbstractProject.class);
	JiraSite site = mock(JiraSite.class);

	private static final String JIRA_VER = Long.toString(System.currentTimeMillis());
	private static final String JIRA_PRJ = "TEST_PRJ";
	
	@Test
	public void jiraApiCalledWithSpecifiedParameters() throws InterruptedException, IOException, ServiceException {
		JiraVersionCreator jvc = spy(new JiraVersionCreator(JIRA_VER, JIRA_PRJ));
		doReturn(site).when(jvc).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
		
		when(build.getProject()).thenReturn(project);
		when(build.getEnvironment(listener)).thenReturn(env);
		when(env.expand(Mockito.anyString())).thenReturn(JIRA_VER);
		when(site.getVersions(JIRA_PRJ)).thenReturn(new HashSet<JiraVersion>());
		
		boolean result = jvc.perform(build, launcher, listener);
		verify(site).addVersion(JIRA_VER, JIRA_PRJ);
		assertThat(result, is(true));
	}
	
	@Test
	public void buildDidNotFailWhenVersionExists() throws IOException, InterruptedException, ServiceException {
		JiraVersionCreator jvc = spy(new JiraVersionCreator(JIRA_VER, JIRA_PRJ));
		doReturn(site).when(jvc).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
		
		when(build.getProject()).thenReturn(project);
		when(build.getEnvironment(listener)).thenReturn(env);
		when(env.expand(Mockito.anyString())).thenReturn(JIRA_VER);
		
		Set<JiraVersion> existingVersions = new HashSet<JiraVersion>();
		existingVersions.add(new JiraVersion(JIRA_VER, null, false, false));
		
		when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);
		
		PrintStream logger = mock(PrintStream.class);
		when(listener.getLogger()).thenReturn(logger);
		
		boolean result = jvc.perform(build, launcher, listener);
		verify(site, times(0)).addVersion(JIRA_VER, JIRA_PRJ);
		assertThat(result, is(true));
	}
}

