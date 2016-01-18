package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JiraEnvironmentContributingActionTest {
    private static final String JIRA_URL = "http://example.com";
    private static final String JIRA_URL_PROPERTY_NAME = "JIRA_URL";  
    private static final String ISSUES_PROPERTY_NAME = "JIRA_ISSUES";  
    private static final String ISSUES_LIST = "ISS-1,ISS-2";  
    
    @Test
    public void buildEnvVarsEnvIsNull() {
        JiraEnvironmentContributingAction action = new JiraEnvironmentContributingAction(ISSUES_LIST, JIRA_URL);
        AbstractBuild build = mock(AbstractBuild.class);
        
        action.buildEnvVars(build, null);
        // just expecting no exception
    }
    
    @Test
    public void buildEnvVarsAddVariables() {
        JiraEnvironmentContributingAction action = new JiraEnvironmentContributingAction(ISSUES_LIST, JIRA_URL);
        AbstractBuild build = mock(AbstractBuild.class);
        EnvVars envVars = mock(EnvVars.class);
        
        action.buildEnvVars(build, envVars);
        
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
        verify(envVars, times(2)).put(keys.capture(), values.capture());
        
        assertThat(keys.getAllValues().get(0), is(ISSUES_PROPERTY_NAME));
        assertThat(values.getAllValues().get(0), is(ISSUES_LIST));
        
        assertThat(keys.getAllValues().get(1), is(JIRA_URL_PROPERTY_NAME));
        assertThat(values.getAllValues().get(1), is(JIRA_URL));
    }
}
