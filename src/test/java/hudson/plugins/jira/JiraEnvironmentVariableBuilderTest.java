package hudson.plugins.jira;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;
import hudson.plugins.jira.selector.ExplicitIssueSelector;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.ArgumentCaptor;

@WithJenkins
class JiraEnvironmentVariableBuilderTest {

    private static final String JIRA_URL = "http://example.com";
    private static final String ISSUE_ID_1 = "ISS-1";
    private static final String ISSUE_ID_2 = "ISS-2";

    // Ordering of set created from collection intializer seems to depend on which JDK is used
    // but isn't important for this purpose
    private static final String EXPECTED_JIRA_ISSUES_1 = ISSUE_ID_1 + "," + ISSUE_ID_2;
    private static final String EXPECTED_JIRA_ISSUES_2 = ISSUE_ID_2 + "," + ISSUE_ID_1;

    AbstractBuild build;
    Launcher launcher;
    BuildListener listener;
    EnvVars env;
    AbstractProject<?, ?> project;
    JiraSite site;
    AbstractIssueSelector issueSelector;
    PrintStream logger;
    Node node;

    @BeforeEach
    void createMocks() throws IOException, InterruptedException {
        build = mock(AbstractBuild.class);
        launcher = mock(Launcher.class);
        listener = mock(BuildListener.class);
        env = mock(EnvVars.class);
        project = mock(AbstractProject.class);
        site = mock(JiraSite.class);
        issueSelector = mock(AbstractIssueSelector.class);
        logger = mock(PrintStream.class);

        when(site.getName()).thenReturn(JIRA_URL);

        when(listener.getLogger()).thenReturn(logger);

        when(issueSelector.findIssueIds(build, site, listener))
                .thenReturn(new LinkedHashSet<>(Arrays.asList(ISSUE_ID_1, ISSUE_ID_2)));

        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
    }

    @Test
    @WithoutJenkins
    public void testIssueSelectorDefaultsToDefault() {
        final JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(null);
        assertThat(builder.getIssueSelector(), instanceOf(DefaultIssueSelector.class));
    }

    @Test
    @WithoutJenkins
    public void testSetIssueSelectorPersists() {
        final JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(issueSelector);
        assertThat(builder.getIssueSelector(), is(issueSelector));
    }

    @Test
    @WithoutJenkins
    public void testPerformWithNoSiteFailsBuild() throws InterruptedException, IOException {
        JiraEnvironmentVariableBuilder builder = spy(new JiraEnvironmentVariableBuilder(issueSelector));
        doReturn(null).when(builder).getSiteForProject(project);
        assertThat(builder.perform(build, launcher, listener), is(false));
        verify(logger, times(1)).println(Messages.JiraEnvironmentVariableBuilder_NoJiraSite());
    }

    @Test
    @WithoutJenkins
    public void testPerformAddsAction() throws InterruptedException, IOException {
        JiraEnvironmentVariableBuilder builder = spy(new JiraEnvironmentVariableBuilder(issueSelector));
        doReturn(site).when(builder).getSiteForProject(project);
        boolean result = builder.perform(build, launcher, listener);

        assertThat(result, is(true));

        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(build).addAction(captor.capture());

        assertThat(captor.getValue(), instanceOf(JiraEnvironmentContributingAction.class));

        JiraEnvironmentContributingAction action = (JiraEnvironmentContributingAction) (captor.getValue());

        assertThat(action.getJiraUrl(), is(JIRA_URL));
        assertThat(action.getIssuesList(), anyOf(is(EXPECTED_JIRA_ISSUES_1), is(EXPECTED_JIRA_ISSUES_2)));
        assertThat(action.getNumberOfIssues(), is(2));
    }

    @Test
    public void testHasIssueSelectors_HasDefaultSelector(JenkinsRule r) {
        JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(null);
        assertThat(builder.getIssueSelector(), instanceOf(DefaultIssueSelector.class));
        JiraEnvironmentVariableBuilder.DescriptorImpl descriptor =
                (JiraEnvironmentVariableBuilder.DescriptorImpl) r.jenkins.getDescriptor(builder.getClass());
        assertTrue(descriptor.hasIssueSelectors());
    }

    @Test
    public void testHasIssueSelectors(JenkinsRule r) {
        ExplicitIssueSelector explicitIssueSelector = new ExplicitIssueSelector();
        JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(explicitIssueSelector);
        assertEquals(explicitIssueSelector, builder.getIssueSelector());
        JiraEnvironmentVariableBuilder.DescriptorImpl descriptor =
                (JiraEnvironmentVariableBuilder.DescriptorImpl) r.jenkins.getDescriptor(builder.getClass());
        assertTrue(descriptor.hasIssueSelectors());
    }

    @Test
    @WithoutJenkins
    public void testEnvBuilderExceptionLogging(JenkinsRule r) throws IOException, InterruptedException {
        Throwable throwable = mock(Throwable.class);
        PrintStream logger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(logger);
        doThrow(new RestClientException("[Jira] Jira REST findIssueIds error. Cause: 401 error", throwable))
                .when(issueSelector)
                .findIssueIds(build, site, listener);
        JiraEnvironmentVariableBuilder builder = spy(new JiraEnvironmentVariableBuilder(issueSelector));
        doReturn(site).when(builder).getSiteForProject(project);
        assertThat(builder.perform(build, launcher, listener), is(false));
        verify(logger).println("[Jira] Jira REST findIssueIds error. Cause: 401 error");
    }
}
