package hudson.plugins.jira;

import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.extension.ExtendedVersion;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * used by JiraReleaseVersionUpdaterBuilder to mark a version as released
 */
public class VersionReleaser {

    private static final Logger LOGGER = Logger.getLogger(VersionReleaser.class.getName());

    protected boolean perform(Job<?, ?> project, String jiraProjectKey, String jiraRelease, String jiraDescription, Run<?, ?> run, TaskListener listener) {
        String realRelease = "NOT_SET";
        String realProjectKey = null;
        String realDescription;

        try {
            realProjectKey = run.getEnvironment(listener).expand(jiraProjectKey);
            realRelease = run.getEnvironment(listener).expand(jiraRelease);
            realDescription = run.getEnvironment(listener).expand(jiraDescription);

            if (StringUtils.isEmpty(realRelease)) {
                throw new IllegalArgumentException("Release is Empty");
            }

            if (StringUtils.isEmpty(realProjectKey)) {
                throw new IllegalArgumentException("No project specified");
            }

            String finalRealRelease = realRelease;
            JiraSite site = getSiteForProject(project);
            Optional<ExtendedVersion> sameNamedVersion = site.getVersions(realProjectKey).stream()
                    .filter(version -> version.getName().equals(finalRealRelease) && version.isReleased()).findFirst();

            if (sameNamedVersion.isPresent()) {
                listener.getLogger().println(Messages.VersionReleaser_AlreadyReleased(realRelease, realProjectKey));
            } else {
                listener.getLogger().println(Messages.VersionReleaser_MarkingReleased(realRelease, realProjectKey));
                releaseVersion(realProjectKey, realRelease, realDescription, site.getSession());
            }
        } catch (Exception e) {
            listener.fatalError(
                    "Unable to release jira version %s/%s: %s",
                    realRelease,
                    realProjectKey,
                    e);
            if (listener instanceof BuildListener) {
                ((BuildListener) listener).finished(Result.FAILURE);
            }
            return false;
        }
        return true;
    }

    /**
     * Release a given version.
     *
     * @param projectKey  The Project Key
     * @param versionName The name of the version
     * @param versionDescription The description of the version
     */
    protected void releaseVersion(String projectKey, String versionName, String versionDescription, JiraSession session) {
        if (session == null) {
            LOGGER.warning("Jira session could not be established");
            return;
        }

        List<ExtendedVersion> versions = session.getVersions(projectKey);
        java.util.Optional<ExtendedVersion> matchingVersion = versions.stream()
                .filter(version -> version.getName().equals(versionName))
                .findFirst();

        if (matchingVersion.isPresent()) {
            ExtendedVersion version = matchingVersion.get();
            ExtendedVersion releaseVersion = new ExtendedVersion(version.getSelf(), version.getId(), version.getName(),
                    !StringUtils.isEmpty(versionDescription) ? versionDescription : version.getDescription(),
                    version.isArchived(), true, version.getStartDate(), new DateTime());
            session.releaseVersion(projectKey, releaseVersion);
        }

    }

    protected JiraSite getSiteForProject(Job<?, ?> project) {
        return JiraSite.get(project);
    }

}
