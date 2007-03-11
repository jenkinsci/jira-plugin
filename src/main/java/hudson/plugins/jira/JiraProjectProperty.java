package hudson.plugins.jira;

import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;
import hudson.util.CopyOnWriteList;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Associates {@link AbstractProject} with {@link JiraSite}.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraProjectProperty extends JobProperty<AbstractProject<?,?>> {

    /**
     * Used to find {@link JiraSite}. Matches {@link JiraSite#getName()}.
     * Always non-null (but beware that this value might become stale
     * if the system config is changed.)
     */
    public final String siteName;

    /**
     * @stapler-constructor
     */
    public JiraProjectProperty(String siteName) {
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

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends JobPropertyDescriptor {
        private final CopyOnWriteList<JiraSite> sites = new CopyOnWriteList<JiraSite>();

        public DescriptorImpl() {
            super(JiraProjectProperty.class);
            load();
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public String getDisplayName() {
            return "Associated JIRA";
        }

        public JiraSite[] getSites() {
            return sites.toArray(new JiraSite[0]);
        }
        
        public JobProperty<?> newInstance(StaplerRequest req) throws FormException {
            JiraProjectProperty jpp = req.bindParameters(JiraProjectProperty.class, "jira.");
            if(jpp.siteName==null)
                jpp = null; // not configured
            return jpp;
        }

        public boolean configure(StaplerRequest req) {
            sites.replaceBy(req.bindParametersToList(JiraSite.class,"jira."));
            save();
            return true;
        }
    }
}
