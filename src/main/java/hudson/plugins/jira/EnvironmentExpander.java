package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;

public class EnvironmentExpander {
    public static EnvVars getEnvVars(Run<?, ?> run, TaskListener listener) {
        if (run == null || listener == null) {
            return null;
        }

        try {
            return run.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    public static String expandVariable(String variable, Run<?, ?> run, TaskListener listener) {
        EnvVars envVars = getEnvVars(run, listener);

        return expandVariable(variable, envVars);
    }

    public static String expandVariable(String variable, EnvVars envVars) {
        if (envVars == null) {
            return variable;
        }

        return envVars.expand(variable);
    }
}
