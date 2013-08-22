package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.IOException;

import javax.xml.rpc.ServiceException;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * 
 * @author Artem Koshelev artkoshelev@gmail.com
 *
 */
public class VersionCreatorTest {
	@Test
	public void jiraApiCalledWithSpecifiedParameters() throws InterruptedException, IOException, ServiceException {
		final String jiraVersion = Long.toString(System.currentTimeMillis());
		final String jiraProject = "TEST_PRJ";
		
		AbstractBuild build = mock(AbstractBuild.class);
		Launcher launcher = mock(Launcher.class);
		BuildListener listener = mock(BuildListener.class);
		EnvVars env = mock(EnvVars.class);
		
		AbstractProject project = mock(AbstractProject.class);
		JiraSite site = mock(JiraSite.class);
		
		JiraVersionCreator jvc = spy(new JiraVersionCreator(jiraVersion, jiraProject));
		doReturn(site).when(jvc).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
		
		when(build.getProject()).thenReturn(project);
		when(build.getEnvironment(listener)).thenReturn(env);
		when(env.expand(Mockito.anyString())).thenReturn(jiraVersion);
		
		boolean result = jvc.perform(build, launcher, listener);
		verify(site).addVersion(jiraVersion, jiraProject);
		assertThat(result, is(true));
	}
	
	@Test
	public void buildDidNotFailWhenVersionExists() {
		
	}
}

