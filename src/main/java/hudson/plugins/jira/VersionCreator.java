package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Version;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.Optional;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Performs an action which creates new jira version.
 */
class VersionCreator {

    private static final Logger LOGGER = Logger.getLogger(VersionCreator.class.getName());

	protected boolean perform(Job<?, ?> project, String jiraVersion, String jiraProjectKey, Run<?, ?> build, TaskListener listener) {
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
			JiraSite site = getSiteForProject(project);
			Optional<Version> sameNamedVersion = site.getVersions(realProjectKey).stream()
					.filter(version -> version.getName().equals(finalRealVersion) && version.isReleased()).findFirst();

			if (sameNamedVersion.isPresent()) {
				listener.getLogger().println(Messages.JiraVersionCreator_VersionExists(realVersion, realProjectKey));
			} else {
				listener.getLogger().println(Messages.JiraVersionCreator_CreatingVersion(realVersion, realProjectKey));
				addVersion(realVersion, realProjectKey, site.getSession());
			}

		} catch (Exception e) {
			e.printStackTrace(
					listener.fatalError("Unable to add version %s to JIRA project %s", realVersion, realProjectKey, e));
			if (listener instanceof BuildListener)
				((BuildListener) listener).finished(Result.FAILURE);
			return false;
		}
		return true;
	}

    /**
     * Creates given version in given project
     * @param version
     * @param projectKey
     * @param session
     */
    protected void addVersion(String version, String projectKey, JiraSession session) {
        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            return;
        }

        session.addVersion(version, projectKey);
    }

	protected JiraSite getSiteForProject(Job<?, ?> project) {
		return JiraSite.get(project);
	}

}
