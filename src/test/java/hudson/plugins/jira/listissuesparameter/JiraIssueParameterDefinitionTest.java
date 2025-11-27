package hudson.plugins.jira.listissuesparameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.jira.Messages;
import hudson.util.ListBoxModel;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JiraIssueParameterDefinitionTest {

    private JiraIssueParameterDefinition definition =
            new JiraIssueParameterDefinition("PARAM_NAME", "desc", "jqlQuery");

    static Stream<Arguments> createValueInvalidParameters() {
        return Stream.of(
                Arguments.of((Object) new String[] {}),
                Arguments.of((Object) new String[] {"a", "b"}),
                Arguments.of((Object) new String[] {""}));
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("createValueInvalidParameters")
    void shouldCreateNullParameterForInvalidValues(String[] values, @Mock StaplerRequest2 req) {
        when(req.getParameterValues(any())).thenReturn(values);

        ParameterValue result = definition.createValue(req);

        assertNull(result);
    }

    @Test
    void shouldCreateValue(@Mock StaplerRequest2 req) {
        when(req.getParameterValues(any())).thenReturn(new String[] {"value"});

        ParameterValue result = definition.createValue(req);

        assertNotNull(result);
        assertEquals("PARAM_NAME", result.getName());
        assertEquals("value", result.getValue());
    }

    @Nested
    class DescriptorImplTest {

        private JiraIssueParameterDefinition.DescriptorImpl uut = new JiraIssueParameterDefinition.DescriptorImpl();

        @Test
        void shouldFillValueItems(
                @Mock Job<?, ?> job,
                @Mock ParametersDefinitionProperty propertyDef,
                @Mock JiraIssueParameterDefinition paramDef,
                @Mock Issue issue) {
            when(job.hasPermission(Item.BUILD)).thenReturn(true);
            when(job.getProperty(ParametersDefinitionProperty.class)).thenReturn(propertyDef);
            when(propertyDef.getParameterDefinition("PARAM_NAME")).thenReturn(paramDef);
            when(issue.getKey()).thenReturn("JIRA-1234");
            when(issue.getSummary()).thenReturn("Summary");
            JiraIssueParameterDefinition.Result item = new JiraIssueParameterDefinition.Result(issue, null);
            when(paramDef.getIssues(any())).thenReturn(List.of(item));

            ListBoxModel result = uut.doFillValueItems(job, "PARAM_NAME");

            assertThat(result, hasSize(1));
            ListBoxModel.Option option = result.get(0);
            assertEquals("JIRA-1234", option.value);
            assertEquals("JIRA-1234: Summary", option.name);
            verify(job).hasPermission(Item.BUILD);
        }

        @Test
        void shouldNotFillValueItemsIfPermissionMissing(@Mock Job<?, ?> job) {
            ListBoxModel result = uut.doFillValueItems(job, "PARAM_NAME");

            assertThat(result, hasSize(1));
            ListBoxModel.Option option = result.get(0);
            assertEquals("", option.value);
            assertEquals(Messages.JiraIssueParameterDefinition_NoIssueMatchedSearch(), option.name);
            verify(job).hasPermission(Item.BUILD);
        }

        @Test
        void shouldHaveNoSearchMatchesItemIfSearchMatchesNoItem(
                @Mock Job<?, ?> job,
                @Mock ParametersDefinitionProperty propertyDef,
                @Mock JiraIssueParameterDefinition paramDef,
                @Mock Issue issue) {
            when(job.hasPermission(Item.BUILD)).thenReturn(true);
            when(job.getProperty(ParametersDefinitionProperty.class)).thenReturn(propertyDef);
            when(propertyDef.getParameterDefinition("PARAM_NAME")).thenReturn(paramDef);

            ListBoxModel result = uut.doFillValueItems(job, "PARAM_NAME");

            assertThat(result, hasSize(1));
            ListBoxModel.Option option = result.get(0);
            assertEquals("", option.value);
            assertEquals(Messages.JiraIssueParameterDefinition_NoIssueMatchedSearch(), option.name);
            verify(job).hasPermission(Item.BUILD);
        }
    }
}
