package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.model.JiraIssue;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Actual JIRA update logic.
 *
 * @author Kohsuke Kawaguchi
 */
class Updater {

    private SCM scm;
    private List<String> labels;

    private static final Logger LOGGER = Logger.getLogger(Updater.class.getName());

    /**
     * Debug flag.
     */
    public static boolean debug = false;

    public Updater(SCM scm) {
        this(scm, new ArrayList<>());
    }

    public Updater(SCM scm, List<String> labels) {
        super();
        this.scm = scm;
        if (labels == null) {
            this.labels = new ArrayList<>();
        } else {
            this.labels = labels;
        }
    }

    boolean perform(Run<?, ?> build, TaskListener listener, AbstractIssueSelector selector) {
        PrintStream logger = listener.getLogger();
        Set<JiraIssue> issues = null;

        try {
            JiraSite site = JiraSite.get(build.getParent());
            if (site == null) {
                logger.println(Messages.NoJiraSite());
                build.setResult(Result.FAILURE);
                return true;
            }

            String rootUrl = Hudson.getInstance().getRootUrl();
            if (rootUrl == null) {
                logger.println(Messages.NoJenkinsUrl());
                build.setResult(Result.FAILURE);
                return true;
            }

            Set<String> ids = selector.findIssueIds(build, site, listener);

            if (ids.isEmpty()) {
                if (debug)
                    logger.println("No JIRA issues found.");
                return true;    // nothing found here.
            }

            JiraSession session = site.getSession();
            if (session == null) {
                logger.println(Messages.NoRemoteAccess());
                build.setResult(Result.FAILURE);
                return true;
            }

            boolean doUpdate = false;
            //in case of workflow, it may be null
            if (site.updateJiraIssueForAllStatus || build.getResult() == null) {
                doUpdate = true;
            } else {
                doUpdate = build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
            }
            boolean useWikiStyleComments = site.supportsWikiStyleComment;

            issues = getJiraIssues(ids, session, logger);
            build.addAction(new JiraBuildAction(build, issues));

            if (doUpdate) {
                submitComments(build, logger, rootUrl, issues,
                        session, useWikiStyleComments, site.recordScmChanges, site.groupVisibility, site.roleVisibility);
            } else {
                // this build didn't work, so carry forward the issues to the next build
                build.addAction(new JiraCarryOverAction(issues));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating JIRA issues. Saving issues for next build.", e);
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
     * Removes from <code>issues</code> issues which have been successfully updated or are invalid
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
    void submitComments(
            Run<?, ?> build, PrintStream logger, String jenkinsRootUrl,
            Set<JiraIssue> issues, JiraSession session,
            boolean useWikiStyleComments, boolean recordScmChanges, String groupVisibility, String roleVisibility) throws RestClientException {

        // copy to prevent ConcurrentModificationException
        Set<JiraIssue> copy = ImmutableSet.copyOf(issues);

        for (JiraIssue issue : copy) {
            logger.println(Messages.UpdatingIssue(issue.getKey()));

            try {
                session.addComment(
                        issue.getKey(),
                        createComment(build, useWikiStyleComments, jenkinsRootUrl, recordScmChanges, issue),
                        groupVisibility, roleVisibility
                );
                if (!labels.isEmpty()) {
                    session.addLabels(issue.getKey(), labels);
                }

            } catch (RestClientException e) {

                if (e.getStatusCode().or(0).equals(404)) {
                    logger.println(issue.getKey() + " - JIRA issue not found. Dropping comment from update queue.");
                }

                if (e.getStatusCode().or(0).equals(403)) {
                    logger.println(issue.getKey() + " - Jenkins JIRA user does not have permissions to comment on this issue. Preserving comment for future update.");
                    continue;
                }

                if (e.getStatusCode().or(0).equals(401)) {
                    logger.println(issue.getKey() + " - Jenkins JIRA authentication problem. Preserving comment for future update.");
                    continue;
                }

                logger.println(Messages.FailedToUpdateIssueWithCarryOver(issue.getKey()));
                logger.println(e.getLocalizedMessage());
            }

            // if no exception is thrown during update, remove from the list as successfully updated
            issues.remove(issue);
        }

    }

    private static Set<JiraIssue> getJiraIssues(Set<String> ids, JiraSession session, PrintStream logger) throws RemoteException {
        Set<JiraIssue> issues = new LinkedHashSet<>(ids.size());
        for (String id : ids) {
            Issue issue = session.getIssue(id);
            if (issue == null) {
                logger.println(id + " issue doesn't exist in JIRA");
                continue;
            }
            
            issues.add(new JiraIssue(issue));
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
    private String createComment(Run<?, ?> build, boolean wikiStyle, String jenkinsRootUrl, boolean recordScmChanges, JiraIssue jiraIssue) {
        Result result = build.getResult();
        //if we run from workflow we dont known final result  
        if(result == null)
            return format(
                    wikiStyle ?
                            "Integrated in [%2$s|%3$s]\n%4$s" :
                            "Integrated in Jenkins build %2$s (See [%3$s])\n%4$s",
                    jenkinsRootUrl,
                    build,
                    Util.encode(jenkinsRootUrl + build.getUrl()),
                    getScmComments(wikiStyle, build, recordScmChanges, jiraIssue));
        else
            return format(
                wikiStyle ?
                        "%6$s: Integrated in !%1$simages/16x16/%3$s! [%2$s|%4$s]\n%5$s" :
                        "%6$s: Integrated in Jenkins build %2$s (See [%4$s])\n%5$s",
                jenkinsRootUrl,
                build,
                result != null ? result.color.getImage() : null,
                Util.encode(jenkinsRootUrl + build.getUrl()),
                getScmComments(wikiStyle, build, recordScmChanges, jiraIssue),
                result.toString());
    }

    private String getScmComments(boolean wikiStyle, Run<?, ?> run, boolean recordScmChanges, JiraIssue jiraIssue) {
        StringBuilder comment = new StringBuilder();
        for (ChangeLogSet<? extends Entry> set : RunScmChangeExtractor.getChanges(run)) {
            for (Entry change : set) {
                if (jiraIssue != null && !StringUtils.containsIgnoreCase(change.getMsg(), jiraIssue.getKey())) {
                    continue;
                }
                comment.append(createScmChangeEntryDescription(run, change, wikiStyle, recordScmChanges));
            }
        }

        if (jiraIssue != null) {
            final Run<?, ?> prev = run.getPreviousBuild();
            if (prev != null) {
                final JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
                if (a != null && a.getIDs().contains(jiraIssue.getKey())) {
                    comment.append(getScmComments(wikiStyle, prev, recordScmChanges, jiraIssue));
                }
            }
        }

        return comment.toString();
    }

    protected String createScmChangeEntryDescription(Run<?, ?> run, Entry change, boolean wikiStyle,
            boolean recordScmChanges) {
        StringBuilder description = new StringBuilder();
        RepositoryBrowser repoBrowser = getRepositoryBrowser(run);
        JiraSite site = JiraSite.get(run.getParent());

        if(change.getMsg() != null)
            description.append(change.getMsg());
        String revision = getRevision(change);
        if (revision != null) {
            description.append(" (");
            appendAuthorToDescription(change, description);
            if (site.isAppendChangeTimestamp() && change.getTimestamp() > 0) {
                appendChangeTimestampToDescription(description, site, change.getTimestamp());
                description.append(" ");
            }
            appendRevisionToDescription(change, wikiStyle, description, repoBrowser, revision);
            description.append(")");
        }
        description.append("\n");
        if (recordScmChanges) {
            appendAffectedFilesToDescription(change, description);
        }
        return description.toString();
    }

    protected void appendAuthorToDescription(Entry change, StringBuilder description) {
        if (change.getAuthor() != null) {
            change.getAuthor();
            String uid = change.getAuthor().getId();
            if (StringUtils.isNotBlank(uid)) {
                description.append(uid).append(": ");
            }
        }
    }

    protected void appendRevisionToDescription(Entry change, boolean wikiStyle, StringBuilder description,
            RepositoryBrowser repoBrowser, String revision) {
        URL url = null;
        if (repoBrowser != null) {
            try {
                url = repoBrowser.getChangeSetLink(change);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to calculate SCM repository browser link", e);
            }
        }
        if (url != null && StringUtils.isNotBlank(url.toExternalForm())) {
            if (wikiStyle) {
                description.append("[").append(revision).append("|");
                description.append(url.toExternalForm()).append("]");
            } else {
                description.append("[").append(url.toExternalForm()).append("]");
            }
        } else {
            description.append("rev ").append(revision);
        }
    }

    protected void appendAffectedFilesToDescription(Entry change, StringBuilder description) {
        // see http://issues.jenkins-ci.org/browse/JENKINS-2508
        // added additional try .. catch; getAffectedFiles is not supported
        // by all SCM implementations
        try {
            for (AffectedFile affectedFile : change.getAffectedFiles()) {
                description.append("* ");
                if(affectedFile.getEditType() != null)
                    description.append("(").append(affectedFile.getEditType().getName()).append(") ");
                if(affectedFile.getPath() != null)
                    description.append(affectedFile.getPath());
                description.append("\n");
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.warning("Unsupported SCM operation 'getAffectedFiles'. Fall back to getAffectedPaths.");
            for (String affectedPath : change.getAffectedPaths()) {
                description.append("* ").append(affectedPath).append("\n");
            }
        }
    }

    protected void appendChangeTimestampToDescription(StringBuilder description, JiraSite site, long timestamp) {
        DateFormat df = null;
        if (!Strings.isNullOrEmpty(site.getDateTimePattern())) {
            df = new SimpleDateFormat(site.getDateTimePattern());
        } else {
            // default format for current locale
            df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        }
        Date changeDate = new Date(timestamp);
        String dateTimeString = df.format(changeDate);
        description.append(dateTimeString);
    }

    private RepositoryBrowser<?> getRepositoryBrowser(Run<?, ?> run) {
        SCM scm = getScm();
        if (scm != null) {
            return scm.getEffectiveBrowser();
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

    private SCM getScm() {
        return scm;
    }

}
