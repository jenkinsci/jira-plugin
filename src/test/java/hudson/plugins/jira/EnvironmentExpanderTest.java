package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class EnvironmentExpanderTest {

    private final static String VARIABLE = "${ISSUE_ID}";
    private final static String ENVIRONMENT_KEY = "ISSUE_ID";
    private final static String ENVIRONMENT_VALUE = "EXAMPLE-1";

    EnvVars env;

    BuildListener buildListener = mock(BuildListener.class);
    AbstractBuild currentBuild = mock(FreeStyleBuild.class);

    @Before
    public void createCharacteristicEnvironment() throws IOException, InterruptedException {
        env = new EnvVars();
        env.put("BUILD_NUMBER", "1");
        env.put("BUILD_URL", "/some/url/to/job");
        env.put("JOB_NAME", "EnvironmentExpander Test Job");

        doReturn(env).when(currentBuild).getEnvironment(Mockito.any());
    }

    @Test
    public void returnVariableWhenValueNotFound() {
        String value = EnvironmentExpander.expandVariable(VARIABLE, env);
        assertThat(value, equalTo(VARIABLE));
    }

    @Test
    public void returnValueWhenFound() {
        env.put(ENVIRONMENT_KEY, ENVIRONMENT_VALUE);

        String value = EnvironmentExpander.expandVariable(VARIABLE, env);
        assertThat(value, equalTo(ENVIRONMENT_VALUE));

        env.remove(ENVIRONMENT_KEY);
    }

    @Test
    public void returnVariableFromNullRunEnvironment(){
        String value = EnvironmentExpander.expandVariable(VARIABLE, null, null);
        assertThat(value, equalTo(VARIABLE));
    }

    @Test
    public void returnValueFromRunEnvironment(){
        env.put(ENVIRONMENT_KEY, ENVIRONMENT_VALUE);

        String value = EnvironmentExpander.expandVariable(VARIABLE, currentBuild, buildListener);
        assertThat(value, equalTo(ENVIRONMENT_VALUE));

        env.remove(ENVIRONMENT_KEY);
    }
}
