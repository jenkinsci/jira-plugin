package hudson.plugins.jira;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import hudson.plugins.jira.model.JiraVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;

public class JiraReleaseVersionUpdaterBuilderTest {
    private static final String JIRA_VER = Long.toString(System.currentTimeMillis());
    private static final String JIRA_PRJ = "TEST_PRJ";
    private static final String JIRA_VER_PARAM = "${JIRA_VER}";
    private static final String JIRA_PRJ_PARAM = "${JIRA_PRJ}";

    AbstractBuild build;
    Launcher launcher;
    BuildListener listener;
    PrintStream logger;
    EnvVars env;
    AbstractProject project;
    JiraSite site;

    @Before
    public void createMocks() {
        build = mock(AbstractBuild.class);
        ;
        launcher = mock(Launcher.class);
        listener = mock(BuildListener.class);
        env = mock(EnvVars.class);
        project = mock(AbstractProject.class);
        site = mock(JiraSite.class);
        logger = mock(PrintStream.class);

        when(env.expand(Mockito.anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                String expanded = (String) args[0];
                if (expanded.equals(JIRA_PRJ_PARAM))
                    return JIRA_PRJ;
                else if (expanded.equals(JIRA_VER_PARAM))
                    return JIRA_VER;
                else
                    return expanded;
            }
        });
        when(listener.getLogger()).thenReturn(logger);
    }

    @Test
    public void jiraApiCalledWithSpecifiedParameters() throws InterruptedException, IOException {
        JiraReleaseVersionUpdaterBuilder jvu = spy(new JiraReleaseVersionUpdaterBuilder(JIRA_PRJ, JIRA_VER));
        doReturn(site).when(jvu).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
        when(site.getVersions(JIRA_PRJ)).thenReturn(new LinkedHashSet<JiraVersion>());

        Set<JiraVersion> existingVersions = new LinkedHashSet<JiraVersion>();
        existingVersions.add(new JiraVersion(JIRA_VER, null, false, false));
        when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);

        jvu.perform(build, null, launcher, listener);
        verify(site).releaseVersion(JIRA_PRJ, JIRA_VER);
    }

    @Test
    public void jiraApiCalledWithSpecifiedParametersExpanded() throws InterruptedException, IOException {
        JiraReleaseVersionUpdaterBuilder jvu = spy(new JiraReleaseVersionUpdaterBuilder(JIRA_PRJ_PARAM, JIRA_VER_PARAM));
        doReturn(site).when(jvu).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
        when(site.getVersions(JIRA_PRJ)).thenReturn(new LinkedHashSet<JiraVersion>());

        Set<JiraVersion> existingVersions = new LinkedHashSet<JiraVersion>();
        existingVersions.add(new JiraVersion(JIRA_VER, null, false, false));
        when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);

        jvu.perform(build, null, launcher, listener);
        verify(site).releaseVersion(JIRA_PRJ, JIRA_VER);
    }

    @Test
    public void buildDidNotFailWhenVersionExists() throws IOException, InterruptedException {
        JiraReleaseVersionUpdaterBuilder jvu = spy(new JiraReleaseVersionUpdaterBuilder(JIRA_PRJ, JIRA_VER));
        doReturn(site).when(jvu).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
        when(env.expand(Mockito.anyString())).thenReturn(JIRA_VER);

        Set<JiraVersion> existingVersions = new LinkedHashSet<JiraVersion>();
        existingVersions.add(new JiraVersion(JIRA_VER, null, true, false));

        when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);

        PrintStream logger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(logger);

        jvu.perform(build, null, launcher, listener);
        verify(site, times(0)).releaseVersion(JIRA_PRJ, JIRA_VER);
    }

    @Test
    public void buildDidNotFailWhenVersionExistsExpanded() throws IOException, InterruptedException {
        JiraReleaseVersionUpdaterBuilder jvu = spy(new JiraReleaseVersionUpdaterBuilder(JIRA_PRJ_PARAM, JIRA_VER_PARAM));
        doReturn(site).when(jvu).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
        when(env.expand(Mockito.anyString())).thenReturn(JIRA_VER);

        Set<JiraVersion> existingVersions = new LinkedHashSet<JiraVersion>();
        existingVersions.add(new JiraVersion(JIRA_VER, null, true, false));

        when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);

        PrintStream logger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(logger);

        jvu.perform(build, null, launcher, listener);
        verify(site, times(0)).releaseVersion(JIRA_PRJ, JIRA_VER);
    }
}
