package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JiraEnvironmentContributingActionTest {
    private static final String JIRA_URL = "http://example.com";
    private static final String ISSUES_LIST = "ISS-1,ISS-2";
    private static final Integer ISSUES_SIZE = 2;

    @Test
    public void buildEnvVarsEnvIsNull() {
        JiraEnvironmentContributingAction action =
                new JiraEnvironmentContributingAction(ISSUES_LIST, ISSUES_SIZE, JIRA_URL);
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);

        action.buildEnvVars(build, null);
        // just expecting no exception
    }

    @Test
    public void passedVariablesAreNull() {
        JiraEnvironmentContributingAction action =
                new JiraEnvironmentContributingAction(ISSUES_LIST, ISSUES_SIZE, JIRA_URL);
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);

        action.buildEnvVars(build, null);
        // just expecting no exception
    }

    @Test
    public void buildEnvVarsAddVariables() {
        JiraEnvironmentContributingAction action =
                new JiraEnvironmentContributingAction(ISSUES_LIST, ISSUES_SIZE, JIRA_URL);
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        EnvVars envVars = mock(EnvVars.class);

        action.buildEnvVars(build, envVars);

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
        verify(envVars, times(3)).put(keys.capture(), values.capture());

        assertThat(keys.getAllValues().get(0), is(JiraEnvironmentContributingAction.ISSUES_VARIABLE_NAME));
        assertThat(values.getAllValues().get(0), is(ISSUES_LIST));

        assertThat(keys.getAllValues().get(1), is(JiraEnvironmentContributingAction.ISSUES_SIZE_VARIABLE_NAME));
        assertThat(values.getAllValues().get(1), is(ISSUES_SIZE.toString()));

        assertThat(keys.getAllValues().get(2), is(JiraEnvironmentContributingAction.JIRA_URL_VARIABLE_NAME));
        assertThat(values.getAllValues().get(2), is(JIRA_URL));
    }

    @Test
    public void noExceptionWhenNullsPassed() {
        JiraEnvironmentContributingAction action = new JiraEnvironmentContributingAction(null, null, null);
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        EnvVars envVars = mock(EnvVars.class);

        action.buildEnvVars(build, envVars);

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
        verify(envVars, times(3)).put(keys.capture(), values.capture());

        assertThat(keys.getAllValues().get(0), is(JiraEnvironmentContributingAction.ISSUES_VARIABLE_NAME));
        assertThat(values.getAllValues().get(0), nullValue());

        assertThat(keys.getAllValues().get(1), is(JiraEnvironmentContributingAction.ISSUES_SIZE_VARIABLE_NAME));
        assertThat(values.getAllValues().get(1), is("0"));

        assertThat(keys.getAllValues().get(2), is(JiraEnvironmentContributingAction.JIRA_URL_VARIABLE_NAME));
        assertThat(values.getAllValues().get(2), nullValue());
    }
}
