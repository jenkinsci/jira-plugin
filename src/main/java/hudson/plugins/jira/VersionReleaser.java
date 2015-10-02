package hudson.plugins.jira;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static hudson.plugins.jira.JiraVersionMatcher.hasName;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created by Reda on 18/12/2014.
 */
public class VersionReleaser {

    private static final String VERSION_ALREADY_RELEASED =
            "The version %s is already released in project %s, so nothing to do.";

    static boolean perform(JiraSite site, String jiraProjectKey, String jiraRelease, AbstractBuild<?, ?> build, BuildListener listener) {
        String realRelease = "NOT_SET";

        try {
            realRelease = build.getEnvironment(listener).expand(jiraRelease);

            if (realRelease == null || realRelease.isEmpty()) {
                throw new IllegalArgumentException("Release is Empty");
            }

            List<JiraVersion> sameNamedVersions = filter(
                    hasName(equalTo(realRelease)),
                    site.getVersions(jiraProjectKey));

            if (sameNamedVersions.size() == 1 && sameNamedVersions.get(0).isReleased()) {
                listener.getLogger().println(String.format(VERSION_ALREADY_RELEASED, realRelease, jiraProjectKey));
            } else {
                site.releaseVersion(jiraProjectKey, realRelease);
            }
        } catch (Exception e) {
            e.printStackTrace(listener.fatalError(
                    "Unable to release jira version %s/%s: %s",
                    realRelease,
                    jiraProjectKey,
                    e));
            listener.finished(Result.FAILURE);
            return false;
        }
        return true;
    }

}
