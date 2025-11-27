package hudson.plugins.jira.versionparameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

import com.atlassian.jira.rest.client.api.domain.Version;
import hudson.cli.CLICommand;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import hudson.plugins.jira.extension.ExtendedVersion;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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
        JiraVersionParameterDefinition definition =
                new JiraVersionParameterDefinition("pname", "pdesc", "JIRAKEY", null, "false", "true", "true");

        assertEquals("JIRAKEY", definition.getJiraProjectKey());
        assertEquals("false", definition.getJiraShowReleased());
        assertEquals("true", definition.getJiraShowArchived());
        assertEquals("true", definition.getJiraShowUnreleased());

        assertEquals("pdesc", definition.getDescription());

        CLICommand cliCommand = mock(CLICommand.class);

        ParameterValue value = definition.createValue(cliCommand, "Jira Version 1.2.3");
        assertEquals(definition.getName(), value.getName());
        assertEquals("Jira Version 1.2.3", value.getValue());
    }

    static Stream<Arguments> createValueInvalidParameters() {
        return Stream.of(
                Arguments.of((Object) new String[] {}),
                Arguments.of((Object) new String[] {"a", "b"}),
                Arguments.of((Object) new String[] {""}));
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("createValueInvalidParameters")
    void shouldCreateNullParameterForInvalidValues(String[] values) {
        JiraVersionParameterDefinition definition =
                new JiraVersionParameterDefinition("pname", "pdesc", "JIRAKEY", null, "false", "true", "true");

        when(request2.getParameterValues(any())).thenReturn(values);

        ParameterValue result = definition.createValue(request2);

        assertNull(result);
    }

    @Test
    void shouldCreateValue() {
        JiraVersionParameterDefinition definition =
                new JiraVersionParameterDefinition("pname", "pdesc", "JIRAKEY", null, "false", "true", "true");

        when(request2.getParameterValues(any())).thenReturn(new String[] {"value"});

        ParameterValue result = definition.createValue(request2);

        assertNotNull(result);
        assertEquals("pname", result.getName());
        assertEquals("value", result.getValue());
    }

    @Test
    void showReleasedVersions() {
        withJiraStaticMocks(() -> {
            JiraVersionParameterDefinition def =
                    new JiraVersionParameterDefinition("name", "desc", "PROJ", null, "true", "false", "false");
            List<JiraVersionParameterDefinition.Result> result;
            try {
                result = def.getVersions();
            } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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

    @Nested
    @ExtendWith(MockitoExtension.class)
    class DescriptorImplTest {

        private JiraVersionParameterDefinition.DescriptorImpl uut = new JiraVersionParameterDefinition.DescriptorImpl();

        @Test
        void shouldFillVersionItems(
                @Mock Job<?, ?> job,
                @Mock ParametersDefinitionProperty propertyDef,
                @Mock JiraVersionParameterDefinition paramDef) {
            when(job.hasPermission(Item.BUILD)).thenReturn(true);
            when(job.getProperty(ParametersDefinitionProperty.class)).thenReturn(propertyDef);
            when(propertyDef.getParameterDefinition("PARAM_NAME")).thenReturn(paramDef);
            Version version = new Version(null, 1L, "1.0.0", "", false, false, null);
            JiraVersionParameterDefinition.Result item = new JiraVersionParameterDefinition.Result(version);
            when(paramDef.getVersions(any())).thenReturn(List.of(item));

            ListBoxModel result = uut.doFillVersionItems(job, "PARAM_NAME");

            assertThat(result, hasSize(1));
            ListBoxModel.Option option = result.get(0);
            assertEquals("1.0.0", option.value);
            assertEquals("1.0.0", option.name);
            verify(job).hasPermission(Item.BUILD);
        }

        @Test
        void shouldNotFillVersionsItemsIfPermissionMissing(@Mock Job<?, ?> job) {
            ListBoxModel result = uut.doFillVersionItems(job, "PARAM_NAME");

            assertThat(result, hasSize(1));
            ListBoxModel.Option option = result.get(0);
            assertEquals("", option.value);
            assertEquals(Messages.JiraVersionParameterDefinition_NoIssueMatchedSearch(), option.name);
            verify(job).hasPermission(Item.BUILD);
        }

        @Test
        void shouldHaveNoSearchMatchesItemIfSearchMatchesNoItem(
                @Mock Job<?, ?> job,
                @Mock ParametersDefinitionProperty propertyDef,
                @Mock JiraVersionParameterDefinition paramDef) {
            when(job.hasPermission(Item.BUILD)).thenReturn(true);
            when(job.getProperty(ParametersDefinitionProperty.class)).thenReturn(propertyDef);
            when(propertyDef.getParameterDefinition("PARAM_NAME")).thenReturn(paramDef);

            ListBoxModel result = uut.doFillVersionItems(job, "PARAM_NAME");

            assertThat(result, hasSize(1));
            ListBoxModel.Option option = result.get(0);
            assertEquals("", option.value);
            assertEquals(Messages.JiraVersionParameterDefinition_NoIssueMatchedSearch(), option.name);
            verify(job).hasPermission(Item.BUILD);
        }
    }
}
