package hudson.plugins.jira;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.scm.ChangeLogSet.Entry;

import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

/**
 * Actual JIRA update logic.
 *
 * 
 * @author Kohsuke Kawaguchi
 */
class Updater {
    static boolean perform(AbstractBuild<?, ?> build, BuildListener listener) throws InterruptedException, IOException {
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
        
        JiraSession session = null;
		try {
			session = site.createSession();
		} catch (ServiceException e) {
			listener.getLogger().println(Messages.Updater_FailedToConnect());
            e.printStackTrace(listener.getLogger());
		}
        if(session==null) {
            logger.println(Messages.Updater_NoRemoteAccess());
            build.setResult(Result.FAILURE);
            return true;
        }

        boolean noUpdate = build.getResult().isWorseThan(Result.UNSTABLE);
        
        boolean useWikiStyleComments = site.supportsWikiStyleComment;

        List<JiraIssue> issues = getJiraIssuesAndSubmitComments(build, logger,
				rootUrl, ids, session, noUpdate, useWikiStyleComments);
            
        build.getActions().add(new JiraBuildAction(build,issues));

        if(noUpdate)
            // this build didn't work, so carry forward the issue to the next build
            build.addAction(new JiraCarryOverAction(ids));

        return true;
    }

    // TODO: the method name implies that this method should be refactored...
	static List<JiraIssue> getJiraIssuesAndSubmitComments(
			AbstractBuild<?, ?> build, PrintStream logger, String hudsonRootUrl,
			Set<String> ids, JiraSession session, boolean noUpdate,
			boolean useWikiStyleComments) throws RemoteException {
		List<JiraIssue> issues = new ArrayList<JiraIssue>();
        for (String id : ids) {
            if(!session.existsIssue(id)) {
                if(debug)
                    logger.println(id+" looked like a JIRA issue but it wasn't");
                continue;   // token looked like a JIRA issue but it's actually not.
            }
            if(!noUpdate) {
                logger.println(Messages.Updater_Updating(id));
                StringBuilder aggregateComment = new StringBuilder();
                for(Entry e :build.getChangeSet()){
                    if(e.getMsg().toUpperCase().contains(id)){
                        aggregateComment.append(e.getMsg()).append("\n");
                        // kutzi: don't know why the issue id was removed in previous versions:
                        //aggregateComment = aggregateComment.replaceAll(id, "");

                    }
                }

                session.addComment(id,
                    createComment(build, useWikiStyleComments,
                    		hudsonRootUrl, aggregateComment.toString()));
            }

            issues.add(new JiraIssue(session.getIssue(id)));
        }
		return issues;
	}

    /**
     * Creates a comment to be used in JIRA for the build.
     */
	private static String createComment(AbstractBuild<?, ?> build,
			boolean wikiStyle, String hudsonRootUrl, String scmComments) {
		return String.format(
		    wikiStyle ?
		    "Integrated in !%1$snocacheImages/16x16/%3$s! [%2$s|%4$s]\n     %5$s":
		    "Integrated in %2$s (See [%4$s])\n    %5$s",
		    hudsonRootUrl,
		    build,
		    build.getResult().color.getImage(),
		    Util.encode(hudsonRootUrl+build.getUrl()),
		    scmComments);
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
        Run<?, ?> prev = build.getPreviousBuild();
        if(prev!=null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if(a!=null)
                ids.addAll(a.getIDs());
        }

        // then issues in this build
        findIssues(build,ids);

        // check for issues fixed in dependencies
        for( DependencyChange depc : build.getDependencyChanges(build.getPreviousBuild()).values())
            for(AbstractBuild<?, ?> b : depc.getBuilds())
                findIssues(b,ids);

        return ids;
    }

    static void findIssues(AbstractBuild<?,?> build, Set<String> ids) {
        for (Entry change : build.getChangeSet()) {
            LOGGER.fine("Looking for JIRA ID in "+change.getMsg());
            Matcher m = ISSUE_PATTERN.matcher(change.getMsg());
            while (m.find())
                ids.add(m.group().toUpperCase());
        }
    }

    /**
     * Regexp pattern that identifies JIRA issue token.
     *
     * <p>
     * First char must be a letter, then at least one letter, digit or underscore.
     * See issue HUDSON-729, HUDSON-4092
     */
    public static final Pattern ISSUE_PATTERN = Pattern.compile("\\b[a-zA-Z]([a-zA-Z0-9_]+)-[1-9][0-9]*\\b");

    private static final Logger LOGGER = Logger.getLogger(Updater.class.getName());

    /**
     * Debug flag.
     */
    public static boolean debug = false;
}
