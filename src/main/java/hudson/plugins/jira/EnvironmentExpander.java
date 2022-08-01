package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;

public class EnvironmentExpander
{
    public static EnvVars GetEnvVars(Run<?, ?> run, TaskListener listener)
    {
        EnvVars envVars;
        try {
            envVars = run.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Can't expand variables to environment", e);
        }

        return envVars;
    }

    public static String expandVariable(String variable, Run<?, ?> run, TaskListener listener)
    {
        EnvVars envVars = GetEnvVars(run, listener);

        return expandVariable(variable, envVars);
    }

    public static String expandVariable(String variable, EnvVars envVars)
    {
        return envVars.expand(variable);
    }
}
