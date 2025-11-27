package hudson.plugins.jira.listissuesparameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.ListBoxModel;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class JiraIssueParameterDefinitionTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
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
            assertEquals("Summary", option.name);
            verify(job).hasPermission(Item.BUILD);
        }

        @Test
        void shouldNotFillValueItemsIfPermissionMissing(@Mock Job<?, ?> job) {
            ListBoxModel result = uut.doFillValueItems(job, "PARAM_NAME");

            assertThat(result, hasSize(0));
            verify(job).hasPermission(Item.BUILD);
            verifyNoMoreInteractions(job);
        }
    }
}
