package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class JiraCreateReleaseNotes extends BuildWrapper {

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
        this.jiraEnvironmentVariable = jiraEnvironmentVariable;
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


    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }

    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException {

        final JiraSite site = getSiteForProject(build.getProject());

        String realRelease = null;
        String realProjectKey = null;
        String releaseNotes = "No Release Notes";
        String realFilter = DEFAULT_FILTER;

        try {
            realRelease = build.getEnvironment(listener).expand(jiraRelease);
            realProjectKey = build.getEnvironment(listener).expand(jiraProjectKey);
            realFilter = build.getEnvironment(listener).expand(jiraFilter);

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
            listener.finished(Result.FAILURE);
            return new Environment() {};
        }

        final Map<String, String> envMap = new HashMap<String, String>();
        envMap.put(jiraEnvironmentVariable, releaseNotes);

        return new Environment() {
            @Override
            public void buildEnvVars(final Map<String, String> env) {
                env.putAll(envMap);
            }
        };
    }
}
