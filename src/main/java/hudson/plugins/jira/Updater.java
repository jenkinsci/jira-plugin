package hudson.plugins.jira;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet.Entry;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Actual JIRA update logic.
 *
 * 
 * @author Kohsuke Kawaguchi
 */
class Updater {
    static boolean perform(AbstractBuild build, BuildListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();

        JiraSite site = JiraSite.get(build.getProject());
        if(site==null) {
            logger.println(Messages.Updater_NoJiraSite());
            build.setResult(Result.FAILURE);
            return true;
        }

        String rootUrl = Hudson.getInstance().getRootUrl();
        if(rootUrl==null) {
            logger.println(Messages.Updater_NoHudsonUrl());
            build.setResult(Result.FAILURE);
            return true;
        }

        Set<String> ids = findIssueIdsRecursive(build);

        if(ids.isEmpty()) {
            if(debug)
                logger.println("No JIRA issues found.");
            return true;    // nothing found here.
        }

        boolean noUpdate = build.getResult().isWorseThan(Result.SUCCESS);

        List<JiraIssue> issues = new ArrayList<JiraIssue>();
        try {
            JiraSession session = site.createSession();
            if(session==null) {
                logger.println(Messages.Updater_NoRemoteAccess());
                build.setResult(Result.FAILURE);
                return true;
            }
            for (String id : ids) {
                if(!session.existsIssue(id)) {
                    if(debug)
                        logger.println(id+" looked like a JIRA issue but it wans't");
                    continue;   // token looked like a JIRA issue but it's actually not.
                }
                if(!noUpdate) {
                    logger.println(Messages.Updater_Updating(id));
                    session.addComment(id,
                        String.format(
                            site.supportsWikiStyleComment?
                            "Integrated in !%1$snocacheImages/16x16/%3$s! [%2$s|%4$s]":
                            "Integrated in %2$s (See [%4$s])",
                            rootUrl, build, build.getResult().color.getImage(),
                            Util.encode(rootUrl+build.getUrl())));
                }

                issues.add(new JiraIssue(session.getIssue(id)));

            }
        } catch (ServiceException e) {
            listener.getLogger().println(Messages.Updater_FailedToConnect());
            e.printStackTrace(listener.getLogger());
        }
        build.getActions().add(new JiraBuildAction(build,issues));

        if(noUpdate)
            // this build didn't work, so carry forward the issue to the next build
            build.addAction(new JiraCarryOverAction(ids));

        return true;
    }

    /**
     * Finds the strings that match JIRA issue ID patterns.
     *
     * This method returns all likely candidates and doesn't check
     * if such ID actually exists or not. We don't want to use
     * {@link JiraSite#existsIssue(String)} here so that new projects
     * in JIRA can be detected.
     */
    private static Set<String> findIssueIdsRecursive(AbstractBuild<?,?> build) {
        Set<String> ids = new HashSet<String>();

        // first, issues that were carried forward.
        Run prev = build.getPreviousBuild();
        if(prev!=null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if(a!=null)
                ids.addAll(a.getIDs());
        }

        // then issues in this build
        findIssues(build,ids);

        // check for issues fixed in dependencies
        for( DependencyChange depc : build.getDependencyChanges(build.getPreviousBuild()).values())
            for(AbstractBuild b : depc.getBuilds())
                findIssues(b,ids);

        return ids;
    }

    private static void findIssues(AbstractBuild<?,?> build, Set<String> ids) {
        for (Entry change : build.getChangeSet()) {
            LOGGER.fine("Looking for JIRA ID in "+change.getMsg());
            Matcher m = ISSUE_PATTERN.matcher(change.getMsg());
            while (m.find())
                ids.add(m.group());
        }
    }

    /**
     * Regexp pattern that identifies JIRA issue token.
     *
     * <p>
     * At least two upper alphabetic.
     * Numbers are also allowed as project keys (see issue #729)
     */
    public static final Pattern ISSUE_PATTERN = Pattern.compile("\\b[A-Z]([A-Z0-9]+)-[1-9][0-9]*\\b");

    private static final Logger LOGGER = Logger.getLogger(Updater.class.getName());

    /**
     * Debug flag.
     */
    public static boolean debug = false;
}
