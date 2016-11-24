package hudson.plugins.jira;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import static org.hamcrest.Matchers.instanceOf;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class JiraEnvironmentVariableBuilderTest {
     
    private static final String JIRA_URL = "http://example.com";
    private static final String JIRA_URL_PROPERTY_NAME = "JIRA_URL";  
    private static final String ISSUES_PROPERTY_NAME = "JIRA_ISSUES";  
    private static final String ISSUE_ID_1 = "ISS-1";
    private static final String ISSUE_ID_2 = "ISS-2";
    
    // Ordering of set created from collection intializer seems to depend on which JDK is used
    // but isn't important for this purpose
    private static final String EXPECTED_JIRA_ISSUES_1 = ISSUE_ID_1+","+ISSUE_ID_2;
    private static final String EXPECTED_JIRA_ISSUES_2 = ISSUE_ID_2+","+ISSUE_ID_1;
    
    AbstractBuild build;
    Launcher launcher;
    BuildListener listener;
    EnvVars env;
    AbstractProject project;
    JiraSite site;
    AbstractIssueSelector issueSelector;
    PrintStream logger;
    Node node;

    @Before
    public void createMocks() throws IOException, InterruptedException {
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
    public void testIssueSelectorDefaultsToDefault() {
        final JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(null);
        assertThat(builder.getIssueSelector(), instanceOf(DefaultIssueSelector.class));
    }

    @Test
    public void testSetIssueSelectorPersists() {
        final JiraEnvironmentVariableBuilder builder = new JiraEnvironmentVariableBuilder(issueSelector);
        assertThat(builder.getIssueSelector(), is(issueSelector));
    }
    
    @Test(expected = AbortException.class)
    public void testPerformWithNoSiteFailsBuild() throws InterruptedException, IOException {
        JiraEnvironmentVariableBuilder builder = spy(new JiraEnvironmentVariableBuilder(issueSelector));
        doReturn(null).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        builder.perform(build, launcher, listener);
    }
    
    @Test
    public void testPerformAddsAction() throws InterruptedException, IOException {
        JiraEnvironmentVariableBuilder builder = spy(new JiraEnvironmentVariableBuilder(issueSelector));
        doReturn(site).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        
        boolean result = builder.perform(build, launcher, listener);
        
        assertThat(result, is(true));
        
        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(build).addAction(captor.capture());
        
        assertThat(captor.getValue(),instanceOf(JiraEnvironmentContributingAction.class));
        
        JiraEnvironmentContributingAction action = (JiraEnvironmentContributingAction)(captor.getValue());
        
        assertThat(action.getJiraUrl(), is(JIRA_URL));
        assertThat(action.getIssuesList(), anyOf(is(EXPECTED_JIRA_ISSUES_1), is(EXPECTED_JIRA_ISSUES_2)));
    }
    
}
