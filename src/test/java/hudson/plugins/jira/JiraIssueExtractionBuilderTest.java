package hudson.plugins.jira;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 *
 * @author sfhardma
 */
public class JiraIssueExtractionBuilderTest {
    
    private static final String WORKSPACE_ENVIRONMENT_VARIABLE = "${WORKSPACE}";
    private static final String WORKSPACE = "/home/user";
    private static final String PROPERTIES_FILE_PATH = WORKSPACE + "/file.properties";
    private static final String PROPERTIES_FILE_PATH_UNEXPANDED = WORKSPACE_ENVIRONMENT_VARIABLE + "/file.properties";
    private static final String ISSUES_PROPERTY_NAME_ENVIRONMENT_VARIABLE = "${ISSUE_PROPERTY}";    
    private static final String ISSUES_PROPERTY_NAME = "ISSUES";  
    private static final String ISSUE_ID_1 = "ISS-1";
    private static final String ISSUE_ID_2 = "ISS-2";
    
    // Ordering of set created from collection intializer seems to depend on which JDK is used
    // but isn't important for this purpose
    private static final String EXPECTED_FILE_CONTENT_MATCHER = "("+ISSUES_PROPERTY_NAME+"="+ISSUE_ID_1+","+ISSUE_ID_2+")|("
                +ISSUES_PROPERTY_NAME+"="+ISSUE_ID_2+","+ISSUE_ID_1+")";
    
    AbstractBuild build;
    Launcher launcher;
    BuildListener listener;
    EnvVars env;
    AbstractProject project;
    JiraSite site;
    PrintWriter printWriter;
    UpdaterIssueSelector issueSelector;
    PrintStream logger;

    @Before
    public void createMocks() throws IOException, InterruptedException {
        build = mock(AbstractBuild.class);
        launcher = mock(Launcher.class);
        listener = mock(BuildListener.class);
        env = mock(EnvVars.class);
        project = mock(AbstractProject.class);
        site = mock(JiraSite.class);
        printWriter = mock(PrintWriter.class);
        issueSelector = mock(UpdaterIssueSelector.class);
        logger = mock(PrintStream.class);
        
        when(listener.getLogger()).thenReturn(logger);
        
        when(issueSelector.findIssueIds(build, site, listener))
                .thenReturn(new HashSet<String>(Arrays.asList(ISSUE_ID_1, ISSUE_ID_2)));

        when(env.expand(Mockito.anyString())).thenAnswer(new Answer<String>() {
                        @Override
                        public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                                Object[] args = invocationOnMock.getArguments();
                                String expanded = (String) args[0];
                                return expanded
                                        .replace(WORKSPACE_ENVIRONMENT_VARIABLE, WORKSPACE)
                                        .replace(ISSUES_PROPERTY_NAME_ENVIRONMENT_VARIABLE, ISSUES_PROPERTY_NAME);
                        }
        });
        
        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
    }
    
    @Test
    public void testIssueSelectorDefaultsToDefault() {
        final JiraIssueExtractionBuilder builder = new JiraIssueExtractionBuilder(null,ISSUES_PROPERTY_NAME,PROPERTIES_FILE_PATH);
        assertThat(builder.getIssueSelector(), instanceOf(DefaultUpdaterIssueSelector.class));
    }

    @Test
    public void testSetIssueSelectorPersists() {
        final JiraIssueExtractionBuilder builder = new JiraIssueExtractionBuilder(issueSelector,ISSUES_PROPERTY_NAME,PROPERTIES_FILE_PATH);
        assertThat(builder.getIssueSelector(), is(issueSelector));
    }
    
    @Test(expected = AbortException.class)
    public void testPerformWithNoSiteFailsBuild() throws InterruptedException, IOException
    {
        JiraIssueExtractionBuilder builder = spy(new JiraIssueExtractionBuilder(issueSelector,ISSUES_PROPERTY_NAME,PROPERTIES_FILE_PATH));
        doReturn(null).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        builder.perform(build, launcher, listener);
    }
    
    @Test
    public void testPerformWritesPropertiesFile() throws FileNotFoundException, InterruptedException, IOException
    {
        JiraIssueExtractionBuilder builder = spy(new JiraIssueExtractionBuilder(issueSelector,ISSUES_PROPERTY_NAME,PROPERTIES_FILE_PATH));
        doReturn(site).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        doReturn(printWriter).when(builder).getPrintWriter((String) Mockito.any());
        boolean result = builder.perform(build, launcher, listener);
        assertThat(result, is(true));
        verify(builder).getPrintWriter(PROPERTIES_FILE_PATH);
        verify(printWriter).println(Mockito.matches(EXPECTED_FILE_CONTENT_MATCHER));
        verify(printWriter).close();
    }
    
    @Test
    public void testPerformParameterExpansionWorks() throws InterruptedException, FileNotFoundException, IOException
    {
        JiraIssueExtractionBuilder builder = spy(new JiraIssueExtractionBuilder(
                issueSelector,
                ISSUES_PROPERTY_NAME_ENVIRONMENT_VARIABLE,
                PROPERTIES_FILE_PATH_UNEXPANDED));
        doReturn(site).when(builder).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        doReturn(printWriter).when(builder).getPrintWriter((String) Mockito.any());
        boolean result = builder.perform(build, launcher, listener);
        assertThat(result, is(true));
        verify(builder).getPrintWriter(PROPERTIES_FILE_PATH);
        verify(printWriter).println(Mockito.matches(EXPECTED_FILE_CONTENT_MATCHER));
        verify(printWriter).close();
    }
}
