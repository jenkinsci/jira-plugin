package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

/**
 * @author Artem Koshelev artkoshelev@gmail.com
 */
public class JiraVersionCreatorTest {
    private static final String JIRA_VER = Long.toString(System.currentTimeMillis());
    private static final String JIRA_PRJ = "TEST_PRJ";
    private static final String JIRA_VER_PARAM = "${JIRA_VER}";
    private static final String JIRA_PRJ_PARAM = "${JIRA_PRJ}";

    AbstractBuild build;
    Launcher launcher;
    BuildListener listener;
    EnvVars env;
    AbstractProject project;
    JiraSite site;
    JiraVersionCreator jvc;

    @Before
    public void createCommonMocks() throws IOException, InterruptedException {
        build = mock(AbstractBuild.class);
        launcher = mock(Launcher.class);
        listener = mock(BuildListener.class);
        env = mock(EnvVars.class);
        project = mock(AbstractProject.class);
        site = mock(JiraSite.class);
        jvc = spy(new JiraVersionCreator(JIRA_VER, JIRA_PRJ));

        doReturn(site).when(jvc).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
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
    }

    @Test
    public void jiraApiCalledWithSpecifiedParameters() throws InterruptedException, IOException {
        when(site.getVersions(JIRA_PRJ)).thenReturn(new HashSet<JiraVersion>());

        boolean result = jvc.perform(build, launcher, listener);
        verify(site).addVersion(JIRA_VER, JIRA_PRJ);
        assertThat(result, is(true));
    }

    @Test
    public void buildDidNotFailWhenVersionExists() throws IOException, InterruptedException {
        Set<JiraVersion> existingVersions = new HashSet<JiraVersion>();
        existingVersions.add(new JiraVersion(JIRA_VER, null, false, false));

        when(site.getVersions(JIRA_PRJ)).thenReturn(existingVersions);

        PrintStream logger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(logger);

        boolean result = jvc.perform(build, launcher, listener);
        verify(site, times(0)).addVersion(JIRA_VER, JIRA_PRJ);
        assertThat(result, is(true));
    }

    @Test
    public void buildDidNotFailWhenVersionExistsExpanded() throws IOException, InterruptedException {
        // Same test as the previous one but here the version and project are contained in a Jenkins parameter
        JiraVersionCreator jvc = spy(new JiraVersionCreator(JIRA_VER_PARAM, JIRA_PRJ_PARAM));
        doReturn(site).when(jvc).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
        when(env.expand(JIRA_VER_PARAM)).thenReturn(JIRA_VER);

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

