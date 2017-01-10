package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class JiraIssuesSizeEnvironmentContributingActionTest {
    private static final Integer NUMBER_OF_ISSUES = 0;
    private static final String ENV_VARIABLE_NAME = "JIRA_ISSUES_SIZE_ENV_VARIABLE";

    @Test
    public void buildEnvVarsEnvIsNull() {
        JiraIssuesSizeEnvironmentContributingAction action = new JiraIssuesSizeEnvironmentContributingAction(NUMBER_OF_ISSUES, ENV_VARIABLE_NAME);
        AbstractBuild build = mock(AbstractBuild.class);
        
        action.buildEnvVars(build, null);
        // just expecting no exception
    }
    
    @Test
    public void buildEnvVarsAddsVariableWithSpecifiedName() {
        JiraIssuesSizeEnvironmentContributingAction action = new JiraIssuesSizeEnvironmentContributingAction(NUMBER_OF_ISSUES, ENV_VARIABLE_NAME);
        AbstractBuild build = mock(AbstractBuild.class);
        EnvVars envVars = mock(EnvVars.class);
        
        action.buildEnvVars(build, envVars);
        
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
        verify(envVars, times(1)).put(keys.capture(), values.capture());
        
        assertThat(keys.getAllValues().get(0), is(ENV_VARIABLE_NAME));
        assertThat(values.getAllValues().get(0), is(NUMBER_OF_ISSUES.toString()));
    }
}
