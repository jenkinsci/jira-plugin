package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class VersionReleaserTest {
    private static final String JIRA_VER = Long.toString(System.currentTimeMillis());
    private static final String JIRA_PRJ = "TEST_PRJ";
    private static final String JIRA_DES = "TEST_DES";
    private static final String JIRA_VER_PARAM = "${JIRA_VER}";
    private static final String JIRA_PRJ_PARAM = "${JIRA_PRJ}";
    private static final String JIRA_DES_PARAM = "${JIRA_DES}";
    private static final Long ANY_ID = System.currentTimeMillis();
    private static final DateTime ANY_DATE = new DateTime();

    @Mock
    AbstractBuild build;

    @Mock
    BuildListener listener;

    @Mock
    PrintStream logger;

    @Mock
    EnvVars env;

    @Mock
    AbstractProject project;

    @Mock(strictness = Mock.Strictness.LENIENT)
    JiraSite site;

    @Mock
    JiraSession session;

    @Captor
    ArgumentCaptor<ExtendedVersion> versionCaptor;

    @Captor
    ArgumentCaptor<String> projectCaptor;

    private VersionReleaser versionReleaser = spy(VersionReleaser.class);
    private ExtendedVersion existingVersion =
            new ExtendedVersion(null, ANY_ID, JIRA_VER, JIRA_DES, false, false, ANY_DATE, ANY_DATE);

    @BeforeEach
    void createMocks() throws Exception {
        when(site.getSession(any())).thenReturn(session);

        when(build.getEnvironment(listener)).thenReturn(env);
        when(env.expand(Mockito.anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            String expanded = (String) args[0];
            if (expanded.equals(JIRA_PRJ_PARAM)) {
                return JIRA_PRJ;
            } else if (expanded.equals(JIRA_VER_PARAM)) {
                return JIRA_VER;
            } else if (expanded.equals(JIRA_DES_PARAM)) {
                return JIRA_DES;
            } else {
                return expanded;
            }
        });
        when(listener.getLogger()).thenReturn(logger);
        doReturn(site).when(versionReleaser).getSiteForProject(any());
    }

    @Test
    void callsJiraWithSpecifiedParameters() {
        when(session.getVersions(JIRA_PRJ)).thenReturn(Collections.singletonList(existingVersion));
        when(site.getVersions(JIRA_PRJ)).thenReturn(new HashSet<>(Arrays.asList(existingVersion)));
        when(site.getSession(any())).thenReturn(session);

        versionReleaser.perform(project, JIRA_PRJ, JIRA_VER, JIRA_DES, build, listener);

        verify(session).releaseVersion(projectCaptor.capture(), versionCaptor.capture());
        assertThat(projectCaptor.getValue(), is(JIRA_PRJ));
        assertThat(versionCaptor.getValue().getName(), is(JIRA_VER));
        assertThat(versionCaptor.getValue().getDescription(), is(JIRA_DES));
    }

    @Test
    void expandsEnvParameters() {
        when(session.getVersions(JIRA_PRJ)).thenReturn(Collections.singletonList(existingVersion));
        when(site.getVersions(JIRA_PRJ)).thenReturn(new HashSet<>(Arrays.asList(existingVersion)));
        when(site.getSession(any())).thenReturn(session);

        versionReleaser.perform(project, JIRA_PRJ_PARAM, JIRA_VER_PARAM, JIRA_DES_PARAM, build, listener);

        verify(session).releaseVersion(projectCaptor.capture(), versionCaptor.capture());
        assertThat(projectCaptor.getValue(), is(JIRA_PRJ));
        assertThat(versionCaptor.getValue().getName(), is(JIRA_VER));
        assertThat(versionCaptor.getValue().getDescription(), is(JIRA_DES));
    }

    @Test
    void buildDidNotFailWhenVersionExists() {
        ExtendedVersion releasedVersion =
                new ExtendedVersion(null, ANY_ID, JIRA_VER, JIRA_DES, false, true, ANY_DATE, ANY_DATE);
        when(site.getVersions(JIRA_PRJ)).thenReturn(new HashSet<>(Arrays.asList(releasedVersion)));

        versionReleaser.perform(project, JIRA_PRJ_PARAM, JIRA_VER_PARAM, JIRA_DES_PARAM, build, listener);
        verify(session, times(0)).releaseVersion(projectCaptor.capture(), versionCaptor.capture());
    }
}
