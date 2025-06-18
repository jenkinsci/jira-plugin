package hudson.plugins.jira.versionparameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.cli.CLICommand;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.MockedStatic;

class JiraVersionParameterDefinitionTest {

    List<ExtendedVersion> versions = new ArrayList<>();
    JiraVersionParameterDefinition.Result resReleasedVer;
    JiraVersionParameterDefinition.Result resUnreleasedVer;
    JiraVersionParameterDefinition.Result resArchivedVer;
    JiraVersionParameterDefinition.Result resReleasedArchivedVer;
    private Job<?, ?> job;
    private JiraSite site;
    private JiraSession session;
    private StaplerRequest2 request2;
    ExtendedVersion extReleasedVer = new ExtendedVersion(null, 1l, "1.0", "", false, true, null, null);
    ExtendedVersion extUnReleasedVer = new ExtendedVersion(null, 2l, "1.1", "", false, false, null, null);
    ExtendedVersion extArchivedVer = new ExtendedVersion(null, 3l, "1.2", "", true, false, null, null);
    ExtendedVersion extReleasedArchivedVer = new ExtendedVersion(null, 4l, "1.3", "", true, true, null, null);

    @BeforeEach
    void createMocksAndVersions() {
        versions.add(extReleasedVer);
        versions.add(extUnReleasedVer);
        versions.add(extArchivedVer);
        versions.add(extReleasedArchivedVer);
        resReleasedVer = new JiraVersionParameterDefinition.Result(extReleasedVer);
        resUnreleasedVer = new JiraVersionParameterDefinition.Result(extUnReleasedVer);
        resArchivedVer = new JiraVersionParameterDefinition.Result(extArchivedVer);
        resReleasedArchivedVer = new JiraVersionParameterDefinition.Result(extReleasedArchivedVer);
        job = mock(Job.class);
        site = mock(JiraSite.class);
        session = mock(JiraSession.class);
        request2 = mock(StaplerRequest2.class);
    }

    @Test
    void parameterValueMethodOverrides() throws Exception {
        ParameterDefinition definition =
                new JiraVersionParameterDefinition("pname", "pdesc", "JIRAKEY", null, "false", "false", "false");
        CLICommand cliCommand = mock(CLICommand.class);

        ParameterValue value = definition.createValue(cliCommand, "Jira Version 1.2.3");
        assertEquals("pname", value.getName());
        assertEquals("Jira Version 1.2.3", value.getValue());
    }

    @Test
    void showReleasedVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "true", "false", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException | RestClientException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected = new ArrayList<>(List.of(resReleasedVer));
            assertEquals(expected, result);
        });
    }

    @Test
    void showUnreleasedVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "false", "true");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException | RestClientException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected = new ArrayList<>(List.of(resUnreleasedVer));
            assertEquals(expected, result);
        });
    }

    @Test
    void showArchivedVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "true", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException | RestClientException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected =
                    new ArrayList<>(List.of(resReleasedArchivedVer, resArchivedVer));
            assertEquals(expected, result);
        });
    }

    @Test
    void showAllVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "false", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException | RestClientException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected =
                    new ArrayList<>(List.of(resReleasedArchivedVer, resArchivedVer, resUnreleasedVer, resReleasedVer));
            assertEquals(expected, result);
        });
    }

    @Test
    void showReleasedUnreleasedVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "true", "false", "true");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException | RestClientException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected =
                    new ArrayList<>(List.of(resUnreleasedVer, resReleasedVer));
            assertEquals(expected, result);
        });
    }

    @Test
    void showReleasedArchivedVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "true", "true", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException | RestClientException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected =
                    new ArrayList<>(List.of(resReleasedArchivedVer, resArchivedVer, resReleasedVer));
            assertEquals(expected, result);
        });
    }

    @Test
    void showUnreleasedArchivedVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "true", "true");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException | RestClientException e) {
                throw new RuntimeException(e);
            }
            List<JiraVersionParameterDefinition.Result> expected =
                    new ArrayList<>(List.of(resReleasedArchivedVer, resArchivedVer, resUnreleasedVer));
            assertEquals(expected, result);
        });
    }

    @Test
    void equalResults() {
        JiraVersionParameterDefinition.Result res1 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        JiraVersionParameterDefinition.Result res2 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        assertTrue(res1.equals(res2));
    }

    @Test
    void diffResults() {
        JiraVersionParameterDefinition.Result res1 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        JiraVersionParameterDefinition.Result res2 = new JiraVersionParameterDefinition.Result(extUnReleasedVer);
        assertFalse(res1.equals(res2));
    }

    @Test
    void nullResultCompare() {
        JiraVersionParameterDefinition.Result res1 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        JiraVersionParameterDefinition.Result res2 = null;
        assertFalse(res1.equals(res2));
    }

    @Test
    void diffClassResultCompare() {
        JiraVersionParameterDefinition.Result res1 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        assertFalse(res1.equals(extReleasedVer));
    }

    @Test
    void sameNameDiffIdResultCompare() {
        JiraVersionParameterDefinition.Result res1 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        ExtendedVersion version = new ExtendedVersion(null, 2l, "1.0", "", false, true, null, null);
        JiraVersionParameterDefinition.Result res2 = new JiraVersionParameterDefinition.Result(version);
        assertFalse(res1.equals(res2));
    }

    @Test
    void compareResultEqHashCode() {
        JiraVersionParameterDefinition.Result res1 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        JiraVersionParameterDefinition.Result res2 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        assertEquals(res1.hashCode(), res2.hashCode());
    }

    @Test
    void compareResultNotEqHashCode() {
        JiraVersionParameterDefinition.Result res1 = new JiraVersionParameterDefinition.Result(extReleasedVer);
        JiraVersionParameterDefinition.Result res2 = new JiraVersionParameterDefinition.Result(extArchivedVer);
        assertNotEquals(res1.hashCode(), res2.hashCode());
    }

    @Test
    void getJiraShowUnreleasedOn() {
        JiraVersionParameterDefinition def =
                new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "false", "true", "true");
        assertEquals("true", def.getJiraShowUnreleased());
    }

    private void withJiraStaticMocks(Runnable testLogic) {
        try (MockedStatic<Stapler> staplerMock = mockStatic(Stapler.class);
                MockedStatic<JiraSite> siteMock = mockStatic(JiraSite.class)) {
            staplerMock.when(Stapler::getCurrentRequest2).thenReturn(request2);
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
