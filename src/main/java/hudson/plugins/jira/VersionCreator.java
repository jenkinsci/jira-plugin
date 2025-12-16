package hudson.plugins.jira;

import static org.apache.commons.lang.StringUtils.isEmpty;

import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Performs an action which creates new Jira version.
 */
class VersionCreator {

    private static final Logger LOGGER = Logger.getLogger(VersionCreator.class.getName());

    private boolean failIfAlreadyExists = true;

    @DataBoundSetter
    public void setFailIfAlreadyExists(boolean failIfAlreadyExists) {
        this.failIfAlreadyExists = failIfAlreadyExists;
    }

    public boolean isFailIfAlreadyExists() {
        return failIfAlreadyExists;
    }

    protected boolean perform(
            Job<?, ?> project, String jiraVersion, String jiraProjectKey, Run<?, ?> build, TaskListener listener) {
        String realVersion = null;
        String realProjectKey = null;

        try {
            realVersion = build.getEnvironment(listener).expand(jiraVersion);
            realProjectKey = build.getEnvironment(listener).expand(jiraProjectKey);

            if (isEmpty(realVersion)) {
                throw new IllegalArgumentException("No version specified");
            }
            if (isEmpty(realProjectKey)) {
                throw new IllegalArgumentException("No project specified");
            }

            String finalRealVersion = realVersion;
            JiraSession session = getSiteForProject(project).getSession(project);

            List<ExtendedVersion> existingVersions =
                    Optional.ofNullable(session.getVersions(realProjectKey)).orElse(Collections.emptyList());

            // check if version already exists
            if (existingVersions.stream().anyMatch(v -> v.getName().equals(finalRealVersion))) {
                listener.getLogger().println(Messages.JiraVersionCreator_VersionExists(realVersion, realProjectKey));
                if (failIfAlreadyExists) {
                    if (listener instanceof BuildListener) {
                        ((BuildListener) listener).finished(Result.FAILURE);
                    }
                    return false;
                }
                // version exists but we don't fail the build
                return true;
            }

            listener.getLogger().println(Messages.JiraVersionCreator_CreatingVersion(realVersion, realProjectKey));
            addVersion(realVersion, realProjectKey, session);
            return true;
        } catch (Exception e) {
            e.printStackTrace(
                    listener.fatalError("Unable to add version %s to Jira project %s", realVersion, realProjectKey, e));
        }

        if (listener instanceof BuildListener) {
            ((BuildListener) listener).finished(Result.FAILURE);
        }
        return false;
    }

    /**
     * Creates given version in given project
     * @param version version name
     * @param projectKey project key
     * @param session session
     */
    protected void addVersion(String version, String projectKey, JiraSession session) {
        if (session == null) {
            LOGGER.warning("Jira session could not be established");
            return;
        }

        session.addVersion(version, projectKey);
    }

    protected JiraSite getSiteForProject(Job<?, ?> project) {
        return JiraSite.get(project);
    }
}
