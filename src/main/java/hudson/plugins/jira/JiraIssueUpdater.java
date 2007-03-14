package hudson.plugins.jira;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses build changelog for JIRA issue IDs and then
 * updates JIRA issues accordingly.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraIssueUpdater extends Publisher {
    public JiraIssueUpdater() {
    }

    /**
     * Regexp pattern that identifies JIRA issue token.
     *
     * <p>
     * At least two upper alphabetic (no numbers allowed.)
     */
    public static final Pattern ISSUE_PATTERN = Pattern.compile("\\b[A-Z]([A-Z]+)-[1-9][0-9]*\\b");

    public boolean perform(Build build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
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
                logger.println("Updating "+id);
                session.addComment(id,
                    MessageFormat.format(
                        site.supportsWikiStyleComment?
                        "Integrated in !{0}nocacheImages/16x16/{3}.gif! [{2}|{0}{1}]":
                        "Integrated in {2} (See {0}{1})",
                        rootUrl, build.getUrl(), build, build.getResult().color));

                issues.add(new JiraIssue(session.getIssue(id)));

            }
        } catch (ServiceException e) {
            e.printStackTrace(listener.error("Failed to connect to JIRA"));
        }
        build.getActions().add(new JiraBuildAction(build,issues));

        return true;
    }

    private Set<String> findIssueIdsRecursive(Build build) {
        Set<String> ids = new HashSet<String>();

        findIssues(build,ids);

        // check for issues fixed in dependencies
        for( DependencyChange depc : build.getDependencyChanges(build.getPreviousBuild()).values())
            for(AbstractBuild b : depc.getBuilds())
                findIssues(b,ids);

        return ids;
    }

    private void findIssues(AbstractBuild<?,?> build, Set<String> ids) {
        for (Iterator<? extends Entry> itr = build.getChangeSet().iterator(); itr.hasNext();) {
            Entry change =  itr.next();
            Matcher m = ISSUE_PATTERN.matcher(change.getMsg());
            while(m.find())
                ids.add(m.group());
        }
    }

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<Publisher> {
        private DescriptorImpl() {
            super(JiraIssueUpdater.class);
        }

        public String getDisplayName() {
            return "Updated relevant JIRA issues";
        }

        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        public Publisher newInstance(StaplerRequest req) {
            return new JiraIssueUpdater();
        }
    }
}
