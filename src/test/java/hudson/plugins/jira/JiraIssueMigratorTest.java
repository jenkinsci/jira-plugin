package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JiraIssueMigratorTest {

    private static final String PROJECT_KEY = "PROJECT";
    private static final String RELEASE = "release";
    private static final String RELEASE_TO_REPLACE = "releaseToReplace";
    private static final String QUERY = "query";
    AbstractBuild build;
    Launcher launcher;
    BuildListener listener;
    EnvVars envVars;
    JiraSite jiraSite;
    AbstractProject project;
    JiraIssueMigrator jiraIssueMigrator;

    @BeforeEach
    void prepareMocks() throws IOException, TimeoutException, InterruptedException {
        build = mock(AbstractBuild.class);
        launcher = mock(Launcher.class);
        listener = mock(BuildListener.class);
        envVars = mock(EnvVars.class);
        jiraSite = mock(JiraSite.class);
        project = mock(AbstractProject.class);

        when(build.getEnvironment(listener)).thenReturn(envVars);
        when(envVars.expand(PROJECT_KEY)).thenReturn(PROJECT_KEY);
        when(envVars.expand(RELEASE)).thenReturn(RELEASE);
        when(envVars.expand(QUERY)).thenReturn(QUERY);
        when(envVars.expand(RELEASE_TO_REPLACE)).thenReturn(RELEASE_TO_REPLACE);
        when(envVars.expand(null)).thenReturn(null);
        when(build.getProject()).thenReturn(project);
    }

    @Test
    void addingVersion() throws IOException, TimeoutException {
        boolean addRelease = true;
        jiraIssueMigrator = spy(new JiraIssueMigrator(PROJECT_KEY, RELEASE, QUERY, null, addRelease));
        doReturn(jiraSite).when(jiraIssueMigrator).getJiraSiteForProject(project);
        boolean result = jiraIssueMigrator.perform(build, launcher, listener);
        verify(jiraSite, never()).migrateIssuesToFixVersion(anyString(), anyString(), anyString());
        verify(jiraSite, never()).replaceFixVersion(anyString(), anyString(), anyString(), anyString());
        verify(jiraSite, times(1)).addFixVersionToIssue(PROJECT_KEY, RELEASE, QUERY);
        assertTrue(result);
    }

    @Test
    void migratingToVersion() throws IOException, TimeoutException {
        jiraIssueMigrator = spy(new JiraIssueMigrator(PROJECT_KEY, RELEASE, QUERY, null, false));
        doReturn(jiraSite).when(jiraIssueMigrator).getJiraSiteForProject(project);
        boolean result = jiraIssueMigrator.perform(build, launcher, listener);
        verify(jiraSite, never()).addFixVersionToIssue(anyString(), anyString(), anyString());
        verify(jiraSite, never()).replaceFixVersion(anyString(), anyString(), anyString(), anyString());
        verify(jiraSite, times(1)).migrateIssuesToFixVersion(PROJECT_KEY, RELEASE, QUERY);
        assertTrue(result);
    }

    @Test
    void replacingVersion() throws IOException, TimeoutException {
        jiraIssueMigrator = spy(new JiraIssueMigrator(PROJECT_KEY, RELEASE, QUERY, RELEASE_TO_REPLACE, false));
        doReturn(jiraSite).when(jiraIssueMigrator).getJiraSiteForProject(project);
        boolean result = jiraIssueMigrator.perform(build, launcher, listener);
        verify(jiraSite, never()).addFixVersionToIssue(anyString(), anyString(), anyString());
        verify(jiraSite, never()).migrateIssuesToFixVersion(anyString(), anyString(), anyString());
        verify(jiraSite, times(1)).replaceFixVersion(PROJECT_KEY, RELEASE_TO_REPLACE, RELEASE, QUERY);
        assertTrue(result);
    }
}
