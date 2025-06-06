package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Status;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JiraCreateReleaseNotesTest {

    private static final String JIRA_RELEASE = Long.toString(System.currentTimeMillis());
    private static final String JIRA_PRJ = "TEST_PRJ";
    private static final String JIRA_RELEASE_PARAM = "${JIRA_RELEASE}";
    private static final String JIRA_PRJ_PARAM = "${JIRA_PRJ}";
    private static final String JIRA_VARIABLE = "ReleaseNotes";
    private static final String JIRA_OTHER_FILTER = "status in (Resolved, Done, Closed)";

    @Mock(strictness = Mock.Strictness.LENIENT)
    AbstractBuild build;

    @Mock
    Launcher launcher;

    @Mock(strictness = Mock.Strictness.LENIENT)
    BuildListener buildListener;

    @Mock(strictness = Mock.Strictness.LENIENT)
    EnvVars env;

    @Mock
    AbstractProject project;

    @Mock
    JiraSite site;

    private final PrintWriter printWriter = new PrintWriter(OutputStream.nullOutputStream());

    @Mock
    JiraSession session;

    @Mock
    Item mockItem;

    @BeforeEach
    void createCommonMocks() throws IOException, InterruptedException {
        when(build.getEnvironment(buildListener)).thenReturn(env);
        when(buildListener.fatalError(Mockito.anyString(), Mockito.any(Object[].class)))
                .thenReturn(printWriter);

        when(env.expand(Mockito.anyString())).thenAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            String expanded = (String) args[0];
            if (expanded.equals(JIRA_PRJ_PARAM)) {
                return JIRA_PRJ;
            } else if (expanded.equals(JIRA_RELEASE_PARAM)) {
                return JIRA_RELEASE;
            } else {
                return expanded;
            }
        });

        when(site.createSession(any(), anyBoolean())).thenReturn(session);
        when(site.getSession(any(), anyBoolean())).thenCallRealMethod();
        site.getSession(mockItem, false);
    }

    @Test
    void defaults() {
        JiraCreateReleaseNotes jcrn = new JiraCreateReleaseNotes(JIRA_PRJ, JIRA_RELEASE, "");
        assertEquals(JiraCreateReleaseNotes.DEFAULT_ENVVAR_NAME, jcrn.getJiraEnvironmentVariable());
        assertEquals(JiraCreateReleaseNotes.DEFAULT_FILTER, jcrn.getJiraFilter());
    }

    @Test
    void jiraApiCallDefaultFilter() throws InterruptedException, IOException, TimeoutException {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes(JIRA_PRJ, JIRA_RELEASE, JIRA_VARIABLE));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        jcrn.setUp(build, launcher, buildListener);
        verify(site).getReleaseNotesForFixVersion(JIRA_PRJ, JIRA_RELEASE, JiraCreateReleaseNotes.DEFAULT_FILTER);
    }

    @Test
    void jiraApiCallOtherFilter() throws InterruptedException, IOException, TimeoutException {
        JiraCreateReleaseNotes jcrn =
                spy(new JiraCreateReleaseNotes(JIRA_PRJ, JIRA_RELEASE, JIRA_VARIABLE, JIRA_OTHER_FILTER));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        BuildListenerResultMethodMock finishedListener = new BuildListenerResultMethodMock();
        jcrn.setUp(build, launcher, buildListener);
        verify(site).getReleaseNotesForFixVersion(JIRA_PRJ, JIRA_RELEASE, JIRA_OTHER_FILTER);
        // assert that build not fail
        assertThat(finishedListener.getResult(), Matchers.nullValue());
    }

    @Test
    void failBuildOnErrorEmptyProjectKey() throws InterruptedException, IOException {
        JiraCreateReleaseNotes jcrn =
                spy(new JiraCreateReleaseNotes("", JIRA_RELEASE, JIRA_VARIABLE, JIRA_OTHER_FILTER));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        BuildListenerResultMethodMock finishedListener = new BuildListenerResultMethodMock();
        Mockito.doAnswer(finishedListener).when(buildListener).finished(Mockito.any());
        jcrn.setUp(build, launcher, buildListener);
        assertThat(finishedListener.getResult(), Matchers.equalTo(Result.FAILURE));
    }

    @Test
    void failBuildOnErrorEmptyRelease() throws InterruptedException, IOException {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes(JIRA_PRJ, "", JIRA_VARIABLE, JIRA_OTHER_FILTER));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        BuildListenerResultMethodMock finishedListener = new BuildListenerResultMethodMock();
        Mockito.doAnswer(finishedListener).when(buildListener).finished(Mockito.any());
        jcrn.setUp(build, launcher, buildListener);
        assertThat(finishedListener.getResult(), Matchers.equalTo(Result.FAILURE));
    }

    @Test
    void releaseNotesContent() throws Exception {
        JiraCreateReleaseNotes jcrn = spy(new JiraCreateReleaseNotes(JIRA_PRJ, JIRA_RELEASE, JIRA_VARIABLE));
        doReturn(site).when(jcrn).getSiteForProject(Mockito.any());
        when(site.getReleaseNotesForFixVersion(JIRA_PRJ, JIRA_RELEASE, JiraCreateReleaseNotes.DEFAULT_FILTER))
                .thenCallRealMethod();

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
        when(session.getIssuesWithFixVersion(JIRA_PRJ, JIRA_RELEASE, JiraCreateReleaseNotes.DEFAULT_FILTER))
                .thenReturn(Arrays.asList(issue1, issue2));

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
