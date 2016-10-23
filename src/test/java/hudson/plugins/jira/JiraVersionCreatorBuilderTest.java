package hudson.plugins.jira;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import hudson.plugins.jira.model.JiraVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;

public class JiraVersionCreatorBuilderTest {

	private static final String JIRA_VER = Long.toString(System.currentTimeMillis());
	private static final String JIRA_PRJ = "TEST_PRJ";
	private static final String JIRA_VER_PARAM = "${JIRA_VER}";
	private static final String JIRA_PRJ_PARAM = "${JIRA_PRJ}";

	@Mock
	private AbstractBuild build;
	@Mock
	private Launcher launcher;
	@Mock
	private BuildListener listener;
	@Mock
	private PrintStream logger;
	@Mock
	private EnvVars env;
	@Mock
	private AbstractProject project;
	@Mock
	private JiraSite site;
	@Spy
	private JiraVersionCreatorBuilder jvc = new JiraVersionCreatorBuilder(JIRA_VER, JIRA_PRJ);

	@Before
	public void createCommonMocks() throws IOException, InterruptedException {
		MockitoAnnotations.initMocks(this);

		doReturn(site).when(jvc).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

		when(build.getProject()).thenReturn(project);
		when(build.getEnvironment(listener)).thenReturn(env);
		when(env.expand(Mockito.anyString())).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocationOnMock) throws Throwable {
				Object[] args = invocationOnMock.getArguments();
				String expanded = (String) args[0];
				if (JIRA_PRJ_PARAM.equals(expanded))
					return JIRA_PRJ;
				else if (JIRA_VER_PARAM.equals(expanded))
					return JIRA_VER;
				else
					return expanded;
			}
		});
		when(listener.getLogger()).thenReturn(logger);
		when(listener.fatalError(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mock(PrintWriter.class));
	}

	@Test
	public void jiraApiCalledWithSpecifiedParameters() throws InterruptedException, IOException {
		// given
		when(site.getVersions(JIRA_PRJ)).thenReturn(new HashSet<JiraVersion>());

		// when
		jvc.perform(build, null, launcher, listener);

		// then
		verify(site).addVersion(JIRA_VER, JIRA_PRJ);
	}

	@Test
	public void jiraApiCalledWithInvalidParameters() throws InterruptedException, IOException {
		// given
		jvc.setJiraProjectKey("");
		jvc.setJiraVersion("");

		// when
		jvc.perform(build, null, launcher, listener);

		// then
		verify(site, never()).addVersion(JIRA_VER, JIRA_PRJ);
		verify(listener).finished(Result.FAILURE);
	}

	@Test
	public void buildDidNotFailWhenVersionExists() throws IOException, InterruptedException {
		// given
		Set<JiraVersion> existingVersions = new HashSet<JiraVersion>();
		existingVersions.add(new JiraVersion(JIRA_VER, null, false, false));
		when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);

		// when
		jvc.perform(build, null, launcher, listener);

		// then
		verify(site, never()).addVersion(JIRA_VER, JIRA_PRJ);
	}

	@Test
	public void buildDidNotFailWhenVersionExistsExpanded() throws IOException, InterruptedException {
		// Same test as the previous one but here the version and project are contained in a Jenkins parameter
		jvc = spy(new JiraVersionCreatorBuilder(JIRA_PRJ_PARAM, JIRA_VER_PARAM));
		doReturn(site).when(jvc).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

		// given
		Set<JiraVersion> existingVersions = new HashSet<JiraVersion>();
		existingVersions.add(new JiraVersion(JIRA_VER, null, false, false));
		when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);

		// when
		jvc.perform(build, null, launcher, listener);

		// then
		verify(site, never()).addVersion(JIRA_VER, JIRA_PRJ);
	}
}
