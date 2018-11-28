package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class JiraCreateReleaseNotes extends SimpleBuildWrapper {

    @Extension
    public final static class Descriptor extends BuildWrapperDescriptor {

        @Override
        public String getDisplayName() {
            return "Generate Release Notes";
        }

        @Override
        public boolean isApplicable(final AbstractProject<?, ?> item) {
            return true;
        }

    }

    public static final String DEFAULT_FILTER = "status in (Resolved, Closed)";
    public static final String DEFAULT_ENVVAR_NAME = "RELEASE_NOTES";

    private String jiraEnvironmentVariable;
    private String jiraProjectKey;
    private String jiraRelease;
    private String jiraFilter;

    // not in use anymore, as it always resets the given filter with the DEFAULT_FILTER
    public JiraCreateReleaseNotes(final String jiraProjectKey,
                                  final String jiraRelease,
                                  final String jiraEnvironmentVariable) {
        this(jiraProjectKey, jiraRelease, jiraEnvironmentVariable, DEFAULT_FILTER);
    }

    @DataBoundConstructor
    public JiraCreateReleaseNotes(final String jiraProjectKey,
                                  final String jiraRelease,
                                  final String jiraEnvironmentVariable,
                                  final String jiraFilter) {
        this.jiraRelease = jiraRelease;
        this.jiraProjectKey = jiraProjectKey;
        this.jiraEnvironmentVariable = defaultIfEmpty(jiraEnvironmentVariable, DEFAULT_ENVVAR_NAME);
        this.jiraFilter = defaultIfEmpty(jiraFilter, DEFAULT_FILTER);
    }

    public String getJiraEnvironmentVariable() {
        return jiraEnvironmentVariable;
    }

    public String getJiraFilter() {
        return jiraFilter;
    }

    public String getJiraProjectKey() {
        return jiraProjectKey;
    }

    public String getJiraRelease() {
        return jiraRelease;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public void setJiraEnvironmentVariable(final String jiraEnvironmentVariable) {
        this.jiraEnvironmentVariable = jiraEnvironmentVariable;
    }

    public void setJiraFilter(final String jiraFilter) {
        this.jiraFilter = jiraFilter;
    }

    public void setJiraProjectKey(final String jiraProjectKey) {
        this.jiraProjectKey = jiraProjectKey;
    }

    public void setJiraRelease(final String jiraRelease) {
        this.jiraRelease = jiraRelease;
    }


    JiraSite getSiteForProject(Job<?, ?> project) {
        return JiraSite.get(project);
    }

    @Override
    public void setUp(Context context, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {

        final JiraSite site = getSiteForProject(run.getParent());

        String realRelease = null;
        String realProjectKey = null;
        String releaseNotes = "No Release Notes";
        String realFilter = DEFAULT_FILTER;

        try {
            realRelease = run.getEnvironment(listener).expand(jiraRelease);
            realProjectKey = run.getEnvironment(listener).expand(jiraProjectKey);
            realFilter = run.getEnvironment(listener).expand(jiraFilter);

            if (isEmpty(realRelease)) {
                throw new IllegalArgumentException("No version specified");
            }
            if (isEmpty(realProjectKey)) {
                throw new IllegalArgumentException("No project specified");
            }

            if ((realRelease != null) && !realRelease.isEmpty()) {
                releaseNotes = site.getReleaseNotesForFixVersion(realProjectKey, realRelease, realFilter);
            } else {
                listener.getLogger().printf("No release version found, skipping Release Notes generation\n");
            }

        } catch (Exception e) {
            e.printStackTrace(listener.fatalError("Unable to generate release notes for JIRA version %s/%s: %s",
                    realRelease,
                    realProjectKey,
                    e
            ));
            if(listener instanceof BuildListener)
                ((BuildListener)listener).finished(Result.FAILURE);
        }

        final Map<String, String> envMap = new HashMap<>();
        envMap.put(jiraEnvironmentVariable, releaseNotes);
        context.getEnv().putAll(envMap);
    }
}
