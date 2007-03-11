package hudson.plugins.jira;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Hudson;
import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import org.kohsuke.stapler.StaplerRequest;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.MessageFormat;

/**
 * Parses build changelog for JIRA issue IDs and then
 * updates JIRA issues accordingly.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraIssueUpdater extends Publisher {

    /**
     * Used to find {@link JiraSite}. Matches {@link JiraSite#getName()}.
     */
    public final String siteName;

    /**
     * @stapler-constructor
     */
    public JiraIssueUpdater(String siteName) {
        if(siteName==null) {
            // defaults to the first one
            JiraSite[] sites = DESCRIPTOR.getSites();
            if(sites.length>0)
                siteName = sites[0].getName();
        }
        this.siteName = siteName;
    }

    /**
     * Gets the {@link JiraSite} that this project belongs to.
     *
     * @return
     *      null if the configuration becomes out of sync.
     */
    public JiraSite getSite() {
        JiraSite[] sites = DESCRIPTOR.getSites();
        if(siteName==null && sites.length>0)
            // default
            return sites[0];

        for( JiraSite site : sites ) {
            if(site.getName().equals(siteName))
                return site;
        }
        return null;
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

        JiraSite site = getSite();
        if(site==null) {
            logger.println("No such jira site '"+this.siteName+"'. This must be a project configuration error");
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
            for (String id : ids) {
                logger.println("Updating "+id);
                session.addComment(id,
                    MessageFormat.format(
                        site.supportsWikiStyleComment?
                        "Integrated in !{0}nocacheImages/16x16/{3}.gif! [{2}|{0}{1}]":
                        "Integrated in {2} (See {0}{1})",
                        rootUrl, build.getUrl(), build, build.getIconColor().noAnime()));

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

        private final CopyOnWriteList<JiraSite> sites = new CopyOnWriteList<JiraSite>();

        private DescriptorImpl() {
            super(JiraIssueUpdater.class);
            load();
        }

        public String getDisplayName() {
            return "Updated relevant JIRA issues";
        }

        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        public JiraSite[] getSites() {
            return sites.toArray(new JiraSite[0]);
        }

        public Publisher newInstance(StaplerRequest req) {
            return req.bindParameters(JiraIssueUpdater.class,"jira.");
        }

        public boolean configure(StaplerRequest req) {
            sites.replaceBy(req.bindParametersToList(JiraSite.class,"jira."));
            save();
            return true;
        }
    }
}
