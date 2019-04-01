package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Status;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JiraCreateReleaseNotesTest {

    private static final String JIRA_RELEASE = Long.toString(System.currentTimeMillis());
    private static final String JIRA_PRJ = "TEST_PRJ";
    private static final String JIRA_RELEASE_PARAM = "${JIRA_RELEASE}";
    private static final String JIRA_PRJ_PARAM = "${JIRA_PRJ}";
    private static final String JIRA_VARIABLE = "ReleaseNotes";
    private static final String JIRA_OTHER_FILTER = "status in (Resolved, Done, Closed)";

    @Mock
    AbstractBuild build;
    @Mock
    Launcher launcher;
    @Mock
    BuildListener buildListener;
    @Mock
    EnvVars env;
    @Mock
    AbstractProject project;
    @Mock
    JiraSite site;
    @Mock
    private PrintWriter printWriter;

    @Before
    public void createCommonMocks() throws IOException, InterruptedException {
        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(buildListener)).thenReturn(env);
        when(buildListener.fatalError(Mockito.anyString(), Mockito.anyVararg())).thenReturn(printWriter);
        when(build.getResult()).thenCallRealMethod();

        when(env.expand(Mockito.anyString())).thenAnswer(invocationOnMock -> {
                Object[] args = invocationOnMock.getArguments();
                String expanded = (String) args[0];
                if (expanded.equals(JIRA_PRJ_PARAM))
                    return JIRA_PRJ;
                else if (expanded.equals(JIRA_RELEASE_PARAM))
                    return JIRA_RELEASE;
                else
                    return expanded;
        });

    }

    @Test
    public void defaults(){
        JiraCreateReleaseNotes jcrn = new JiraCreateReleaseNotes(JIRA_PRJ,JIRA_RELEASE,"");
        assertEquals(JiraCreateReleaseNotes.DEFAULT_ENVVAR_NAME, jcrn.getJiraEnvironmentVariable());
        assertEquals(JiraCreateReleaseNotes.DEFAULT_FILTER, jcrn.getJiraFilter());
    }

    @Test
    public void jiraApiCallDefaultFilter() throws InterruptedException, IOException, TimeoutException {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes(JIRA_PRJ,JIRA_RELEASE,JIRA_VARIABLE));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        jcrn.setUp(build, launcher, buildListener);
        verify(site).getReleaseNotesForFixVersion(JIRA_PRJ, JIRA_RELEASE, JiraCreateReleaseNotes.DEFAULT_FILTER);
    }

    @Test
    public void jiraApiCallOtherFilter() throws InterruptedException, IOException, TimeoutException {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes(JIRA_PRJ,JIRA_RELEASE,JIRA_VARIABLE, JIRA_OTHER_FILTER));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        BuildListenerResultMethodMock finishedListener = new BuildListenerResultMethodMock();
        Mockito.doAnswer(finishedListener).when(buildListener).finished(Mockito.anyObject());
        jcrn.setUp(build, launcher, buildListener);
        verify(site).getReleaseNotesForFixVersion(JIRA_PRJ, JIRA_RELEASE, JIRA_OTHER_FILTER);
        //assert that build not fail
        assertThat(finishedListener.getResult(), Matchers.nullValue());
    }

    @Test
    public void failBuildOnErrorEmptyProjectKey() throws InterruptedException, IOException {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes("",JIRA_RELEASE,JIRA_VARIABLE, JIRA_OTHER_FILTER));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        BuildListenerResultMethodMock finishedListener = new BuildListenerResultMethodMock();
        Mockito.doAnswer(finishedListener).when(buildListener).finished(Mockito.anyObject());
        jcrn.setUp(build, launcher, buildListener);        
        assertThat(finishedListener.getResult(), Matchers.equalTo(Result.FAILURE));
    }

    @Test
    public void failBuildOnErrorEmptyRelease() throws InterruptedException, IOException {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes(JIRA_PRJ,"",JIRA_VARIABLE, JIRA_OTHER_FILTER));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        BuildListenerResultMethodMock finishedListener = new BuildListenerResultMethodMock();
        Mockito.doAnswer(finishedListener).when(buildListener).finished(Mockito.anyObject());
        jcrn.setUp(build, launcher, buildListener);        
        assertThat(finishedListener.getResult(), Matchers.equalTo(Result.FAILURE));
    }
    
    @Test
    public void releaseNotesContent() throws InterruptedException, IOException, TimeoutException {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes(JIRA_PRJ,JIRA_RELEASE,JIRA_VARIABLE));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        when(site.getReleaseNotesForFixVersion(JIRA_PRJ, JIRA_RELEASE, JiraCreateReleaseNotes.DEFAULT_FILTER)).thenCallRealMethod();
        JiraSession session = Mockito.mock(JiraSession.class);
        doReturn(session).when(site).getSession();
        Issue issue1 = Mockito.mock(Issue.class);
        IssueType issueType1 = Mockito.mock(IssueType.class);
        Status issueStatus = Mockito.mock(Status.class);
        when(issue1.getIssueType()).thenReturn(issueType1);
        when(issue1.getStatus()).thenReturn(issueStatus);
        when(issueType1.getName()).thenReturn("Bug");
        Issue issue2 = Mockito.mock(Issue.class);
        IssueType issueType2 = Mockito.mock(IssueType.class);
        when(issue2.getIssueType()).thenReturn(issueType2);
        when(issue2.getStatus()).thenReturn(issueStatus);
        when(issueType2.getName()).thenReturn("Feature");
        when(session.getIssuesWithFixVersion(JIRA_PRJ, JIRA_RELEASE, JiraCreateReleaseNotes.DEFAULT_FILTER)).
           thenReturn(Arrays.asList(issue1, issue2));

        BuildWrapper.Environment environment = jcrn.setUp(build, launcher, buildListener);
        Map<String, String> envVars = new HashMap<>();
        environment.buildEnvVars(envVars);
        String releaseNotes = envVars.get(jcrn.getJiraEnvironmentVariable());
        assertNotNull(releaseNotes);
        assertThat(releaseNotes, Matchers.containsString(issueType1.getName()));
        assertThat(releaseNotes, Matchers.containsString(issueType2.getName()));
        assertThat(releaseNotes, Matchers.not(Matchers.containsString("UNKNOWN")));
    }
}
