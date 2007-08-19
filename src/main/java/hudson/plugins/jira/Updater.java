package hudson.plugins.jira;

import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.scm.ChangeLogSet.Entry;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
            logger.println("No jira site is configured for this project. This must be a project configuration error");
            build.setResult(Result.FAILURE);
            return true;
        }

        String rootUrl = Hudson.getInstance().getRootUrl();
        if(rootUrl==null) {
            logger.println("Hudson URL is not configured yet. Go to system configuration to set this value");
            build.setResult(Result.FAILURE);
            return true;
        }

        Set<String> ids = findIssueIdsRecursive(build);

        if(ids.isEmpty())
            return true;    // nothing found here.

        List<JiraIssue> issues = new ArrayList<JiraIssue>();
        try {
            JiraSession session = site.createSession();
            if(session==null) {
                logger.println("The system configuration does not allow remote JIRA access");
                build.setResult(Result.FAILURE);
                return true;
            }
            for (String id : ids) {
                if(!session.existsIssue(id))
                    continue;   // token looked like a JIRA issue but it's actually not.
                logger.println("Updating "+id);
                session.addComment(id,
                    String.format(
                        site.supportsWikiStyleComment?
                        "Integrated in !%1$snocacheImages/16x16/%4$s.gif! [%3$s|%1$s%2$s]":
                        "Integrated in %3$s (See [%1$s%2$s])",
                        rootUrl, build.getUrl(), build, build.getResult().color));

                issues.add(new JiraIssue(session.getIssue(id)));

            }
        } catch (ServiceException e) {
            e.printStackTrace(listener.error("Failed to connect to JIRA"));
        }
        build.getActions().add(new JiraBuildAction(build,issues));

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

        findIssues(build,ids);

        // check for issues fixed in dependencies
        for( DependencyChange depc : build.getDependencyChanges(build.getPreviousBuild()).values())
            for(AbstractBuild b : depc.getBuilds())
                findIssues(b,ids);

        return ids;
    }

    private static void findIssues(AbstractBuild<?,?> build, Set<String> ids) {
        for (Iterator<? extends Entry> itr = build.getChangeSet().iterator(); itr.hasNext();) {
            Entry change =  itr.next();
            Matcher m = ISSUE_PATTERN.matcher(change.getMsg());
            while(m.find())
                ids.add(m.group());
        }
    }

    /**
     * Regexp pattern that identifies JIRA issue token.
     *
     * <p>
     * At least two upper alphabetic (no numbers allowed.)
     */
    public static final Pattern ISSUE_PATTERN = Pattern.compile("\\b[A-Z]([A-Z]+)-[1-9][0-9]*\\b");
}
