package hudson.plugins.jira;

import static ch.lambdaj.Lambda.filter;
import static hudson.plugins.jira.JiraVersionMatcher.hasName;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.model.JiraVersion;

/**
 * Performs an action which creates new jira version.
 */
class VersionCreator {

	static boolean perform(JiraSite site, String jiraVersion, String jiraProjectKey, Run<?, ?> build, TaskListener listener) {
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

			List<JiraVersion> sameNamedVersions = filter(hasName(equalTo(realVersion)),
					site.getVersions(realProjectKey));

			if (sameNamedVersions.size() == 0) {
				listener.getLogger().println(Messages.JiraVersionCreator_CreatingVersion(realVersion, realProjectKey));
				site.addVersion(realVersion, realProjectKey);
			} else {
				listener.getLogger().println(Messages.JiraVersionCreator_VersionExists(realVersion, realProjectKey));
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
}
