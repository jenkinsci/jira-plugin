package hudson.plugins.jira.versionparameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.atlassian.jira.rest.client.api.domain.Version;
import hudson.model.Job;
import hudson.plugins.jira.JiraGlobalConfiguration;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.MockedStatic;

public class JiraVersionParameterFilterTest {

    List<ExtendedVersion> versions = new ArrayList<>();
    JiraVersionParameterDefinition.Result r1;
    JiraVersionParameterDefinition.Result r2;
    JiraVersionParameterDefinition.Result r3;
    private Job<?, ?> job;
    private JiraSite site;
    private JiraSession session;
    private StaplerRequest2 request2;
    Jenkins jenkins;
    private JiraGlobalConfiguration globalConfig;

    @BeforeEach
    void createMocksAndVersions() {
        versions.add(new ExtendedVersion(null, null, "1.0", "", false, true, null, null));
        versions.add(new ExtendedVersion(null, null, "1.1", "", false, false, null, null));
        versions.add(new ExtendedVersion(null, null, "1.2", "", true, false, null, null));
        r1 = new JiraVersionParameterDefinition.Result(new Version(null, null, "1.0", "", false, true, null));
        r2 = new JiraVersionParameterDefinition.Result(new Version(null, null, "1.1", "", false, false, null));
        r3 = new JiraVersionParameterDefinition.Result(new Version(null, null, "1.2", "", true, false, null));
        job = mock(Job.class);
        site = mock(JiraSite.class);
        session = mock(JiraSession.class);
        request2 = mock(StaplerRequest2.class);
        jenkins = mock(Jenkins.class);
        globalConfig = mock(JiraGlobalConfiguration.class);

        when(request2.findAncestorObject(Job.class)).thenReturn(job);
        when(site.getSession(job)).thenReturn(session);
        when(session.getVersions("PROJ")).thenReturn(versions);
    }

    @Test
    void showReleaseTest() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "true", "false", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected = new ArrayList<>();
            expected.add(r2);
            expected.add(r1);
            assertEquals(expected, result);
        });
    }

    @Test
    void showOnlyReleaseTest() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "false", "true");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected = new ArrayList<>();
            expected.add(r1);
            assertEquals(expected, result);
        });
    }

    @Test
    void showArchiveTest() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "true", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected = new ArrayList<>();
            expected.add(r3);
            assertEquals(expected, result);
        });
    }

    @Test
    void showUnreleasedTest() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "false", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected = new ArrayList<>();
            expected.add(r2);
            assertEquals(expected, result);
        });
    }

    @Test
    void showAllTest() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "true", "true", "true");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected = new ArrayList<>();
            expected.add(r3);
            expected.add(r2);
            expected.add(r1);
            assertEquals(expected, result);
        });
    }

    private void withJiraStaticMocks(Runnable testLogic) {
        try (MockedStatic<Stapler> staplerMock = mockStatic(Stapler.class);
                MockedStatic<Jenkins> jenkinsMock = mockStatic(Jenkins.class);
                MockedStatic<JiraGlobalConfiguration> configMock = mockStatic(JiraGlobalConfiguration.class);
                MockedStatic<JiraSite> siteMock = mockStatic(JiraSite.class)) {
            staplerMock.when(Stapler::getCurrentRequest2).thenReturn(request2);
            jenkinsMock.when(Jenkins::get).thenReturn(jenkins);
            configMock.when(JiraGlobalConfiguration::get).thenReturn(globalConfig);
            siteMock.when(() -> JiraSite.get(job)).thenReturn(site);

            when(request2.findAncestorObject(Job.class)).thenReturn(job);
            when(site.getSession(job)).thenReturn(session);
            when(session.getVersions("PROJ")).thenReturn(versions);

            testLogic.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
