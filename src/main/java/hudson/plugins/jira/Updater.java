package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.Util;
import hudson.model.*;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.RepositoryBrowser;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Actual JIRA update logic.
 *
 * @author Kohsuke Kawaguchi
 */
class Updater {
    static boolean perform(AbstractBuild<?, ?> build, BuildListener listener, UpdaterIssueSelector selector) {
        PrintStream logger = listener.getLogger();
        List<JiraIssue> issues = null;

        try {
            JiraSite site = JiraSite.get(build.getProject());
            if (site == null) {
                logger.println(Messages.Updater_NoJiraSite());
                build.setResult(Result.FAILURE);
                return true;
            }

            String rootUrl = Hudson.getInstance().getRootUrl();
            if (rootUrl == null) {
                logger.println(Messages.Updater_NoJenkinsUrl());
                build.setResult(Result.FAILURE);
                return true;
            }

            Set<String> ids = selector.findIssueIds(build, site, listener);

            if (ids.isEmpty()) {
                if (debug)
                    logger.println("No JIRA issues found.");
                return true;    // nothing found here.
            }

            JiraSession session = null;
            try {
                session = site.getSession();
            } catch (IOException e) {
                listener.getLogger().println(Messages.Updater_FailedToConnect());
                e.printStackTrace(listener.getLogger());
            }
            if (session == null) {
                logger.println(Messages.Updater_NoRemoteAccess());
                build.setResult(Result.FAILURE);
                return true;
            }

            boolean doUpdate = false;
            if (site.updateJiraIssueForAllStatus) {
                doUpdate = true;
            } else {
                doUpdate = build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
            }
            boolean useWikiStyleComments = site.supportsWikiStyleComment;

            issues = getJiraIssues(ids, session, logger);
            build.getActions().add(new JiraBuildAction(build, issues));

            if (doUpdate) {
                submitComments(build, logger, rootUrl, issues,
                        session, useWikiStyleComments, site.recordScmChanges, site.groupVisibility, site.roleVisibility);
            } else {
                // this build didn't work, so carry forward the issues to the next build
                build.addAction(new JiraCarryOverAction(issues));
            }
        } catch (Exception e) {
            logger.println("Error updating JIRA issues. Saving issues for next build.\n" + e);
            if (issues != null && !issues.isEmpty()) {
                // updating issues failed, so carry forward issues to the next build
                build.addAction(new JiraCarryOverAction(issues));
            }
        }

        return true;
    }


    /**
     * Submits comments for the given issues.
     * Remvoes from <code>issues</code> issues which have been successfully updated or are invalid
     *
     * @param build
     * @param logger
     * @param jenkinsRootUrl
     * @param session
     * @param useWikiStyleComments
     * @param recordScmChanges
     * @param groupVisibility
     * @throws RestClientException
     */
    static void submitComments(
            AbstractBuild<?, ?> build, PrintStream logger, String jenkinsRootUrl,
            List<JiraIssue> issues, JiraSession session,
            boolean useWikiStyleComments, boolean recordScmChanges, String groupVisibility, String roleVisibility) throws RestClientException {

        // copy to prevent ConcurrentModificationException
        List<JiraIssue> copy = new ArrayList<JiraIssue>(issues);

        for (JiraIssue issue : copy) {
            logger.println(Messages.Updater_Updating(issue.id));

            try {
                session.addComment(
                        issue.id,
                        createComment(build, useWikiStyleComments, jenkinsRootUrl, recordScmChanges, issue),
                        groupVisibility, roleVisibility
                );

            } catch (RestClientException e) {

                if (e.getStatusCode().or(0).equals(404)) {
                    logger.println(issue.id + " - JIRA issue not found. Dropping comment from update queue.");
                }

                if (e.getStatusCode().or(0).equals(403)) {
                    logger.println(issue.id + " - Jenkins JIRA user does not have permissions to comment on this issue. Preserving comment for future update.");
                    continue;
                }

                if (e.getStatusCode().or(0).equals(401)) {
                    logger.println(issue.id + " - Jenkins JIRA authentication problem. Preserving comment for future update.");
                    continue;
                }

                logger.println(Messages.Updater_FailedToCommentOnIssue(issue.id));
                logger.println(e.getLocalizedMessage());
            }

            // if no exception is thrown during update, remove from the list as succesfully updated
            issues.remove(issue);
        }

    }

    private static List<JiraIssue> getJiraIssues(
            Set<String> ids, JiraSession session, PrintStream logger) throws RemoteException {
        List<JiraIssue> issues = new ArrayList<JiraIssue>(ids.size());
        for (String id : ids) {
            if (!session.existsIssue(id)) {
                if (debug) {
                    logger.println(id + " looked like a JIRA issue but it wasn't");
                }
                continue;   // token looked like a JIRA issue but it's actually not.
            }

            issues.add(new JiraIssue(session.getIssue(id)));
        }
        return issues;
    }


    /**
     * Creates a comment to be used in JIRA for the build.
     * For example:
     * <pre>
     *  SUCCESS: Integrated in Job #nnnn (See [http://jenkins.domain/job/Job/nnnn/])\r
     *  JIRA-XXXX: Commit message. (Author _author@email.domain_:
     *  [https://bitbucket.org/user/repo/changeset/9af8e4c4c909/])\r
     * </pre>
     */
    private static String createComment(AbstractBuild<?, ?> build,
                                        boolean wikiStyle, String jenkinsRootUrl, boolean recordScmChanges, JiraIssue jiraIssue) {
        return format(
                wikiStyle ?
                        "%6$s: Integrated in !%1$simages/16x16/%3$s! [%2$s|%4$s]\n%5$s" :
                        "%6$s: Integrated in Jenkins build %2$s (See [%4$s])\n%5$s",
                jenkinsRootUrl,
                build,
                build.getResult().color.getImage(),
                Util.encode(jenkinsRootUrl + build.getUrl()),
                getScmComments(wikiStyle, build, recordScmChanges, jiraIssue),
                build.getResult().toString());
    }

    private static String getScmComments(boolean wikiStyle,
                                         AbstractBuild<?, ?> build, boolean recordScmChanges, JiraIssue jiraIssue) {
        StringBuilder comment = new StringBuilder();
        RepositoryBrowser repoBrowser = getRepositoryBrowser(build);
        for (Entry change : build.getChangeSet()) {
            if (jiraIssue != null && !StringUtils.containsIgnoreCase(change.getMsg(), jiraIssue.id)) {
                continue;
            }
            comment.append(change.getMsg());
            String revision = getRevision(change);
            if (revision != null) {
                URL url = null;
                if (repoBrowser != null) {
                    try {
                        url = repoBrowser.getChangeSetLink(change);
                    } catch (IOException e) {
                        LOGGER.warning("Failed to calculate SCM repository browser link " + e.getMessage());
                    }
                }
                comment.append(" (");
                String uid = change.getAuthor().getId();
                if (StringUtils.isNotBlank(uid)) {
                    comment.append(uid).append(": ");
                }
                if (url != null && StringUtils.isNotBlank(url.toExternalForm())) {
                    if (wikiStyle) {
                        comment.append("[").append(revision).append("|");
                        comment.append(url.toExternalForm()).append("]");
                    } else {
                        comment.append("[").append(url.toExternalForm()).append("]");
                    }
                } else {
                    comment.append("rev ").append(revision);
                }
                comment.append(")");
            }
            comment.append("\n");
            if (recordScmChanges) {
                // see http://issues.jenkins-ci.org/browse/JENKINS-2508
                // added additional try .. catch; getAffectedFiles is not supported by all SCM implementations
                try {
                    for (AffectedFile affectedFile : change.getAffectedFiles()) {
                        comment.append("* ").append(affectedFile.getPath()).append("\n");
                    }
                } catch (UnsupportedOperationException e) {
                    LOGGER.warning("Unsupported SCM operation 'getAffectedFiles'. Fall back to getAffectedPaths.");
                    for (String affectedPath : change.getAffectedPaths()) {
                        comment.append("* ").append(affectedPath).append("\n");
                    }
                }
                comment.append("\n");
            }
        }
        return comment.toString();
    }

    private static RepositoryBrowser<?> getRepositoryBrowser(AbstractBuild<?, ?> build) {
        if (build.getProject().getScm() != null) {
            return build.getProject().getScm().getEffectiveBrowser();
        }
        return null;
    }

    private static String getRevision(Entry entry) {
        String commitId = entry.getCommitId();
        if (commitId != null) {
            return commitId;
        }

        // fall back to old SVN-specific solution, if we have only installed an old subversion-plugin
        // which doesn't implement getCommitId, yet
        try {
            Class<?> clazz = entry.getClass();
            Method method = clazz.getMethod("getRevision", (Class[]) null);
            if (method == null) {
                return null;
            }
            Object revObj = method.invoke(entry, (Object[]) null);
            return (revObj != null) ? revObj.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Finds the strings that match JIRA issue ID patterns.
     * This method returns all likely candidates and doesn't check
     * if such ID actually exists or not. We don't want to use
     * {@link JiraSite#existsIssue(String)} here so that new projects
     * in JIRA can be detected.
     */
    static Set<String> findIssueIdsRecursive(AbstractBuild<?, ?> build, Pattern pattern,
                                                     TaskListener listener) {
        Set<String> ids = new HashSet<String>();

        // first, issues that were carried forward.
        Run<?, ?> prev = build.getPreviousBuild();
        if (prev != null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if (a != null) {
                ids.addAll(a.getIDs());
            }
        }

        // then issues in this build
        findIssues(build, ids, pattern, listener);

        // check for issues fixed in dependencies
        for (DependencyChange depc : build.getDependencyChanges(build.getPreviousBuild()).values()) {
            for (AbstractBuild<?, ?> b : depc.getBuilds()) {
                findIssues(b, ids, pattern, listener);
            }
        }
        return ids;
    }

    /**
     * @param pattern pattern to use to match issue ids
     */
    static void findIssues(AbstractBuild<?, ?> build, Set<String> ids, Pattern pattern,
                           TaskListener listener) {
        for (Entry change : build.getChangeSet()) {
            LOGGER.fine("Looking for JIRA ID in " + change.getMsg());
            Matcher m = pattern.matcher(change.getMsg());

            while (m.find()) {
                if (m.groupCount() >= 1) {
                    String content = StringUtils.upperCase(m.group(1));
                    ids.add(content);
                } else {
                    listener.getLogger().println("Warning: The JIRA pattern " + pattern + " doesn't define a capturing group!");
                }
            }

        }

        // Now look for any JiraIssueParameterValue's set in the build
        // Implements JENKINS-12312
        ParametersAction parameters = build.getAction(ParametersAction.class);

        if (parameters != null) {
            for (ParameterValue val : parameters.getParameters()) {
                if (val instanceof JiraIssueParameterValue) {
                    ids.add(((JiraIssueParameterValue) val).getValue().toString());
                }
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Updater.class.getName());

    /**
     * Debug flag.
     */
    public static boolean debug = false;
}
