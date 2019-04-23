package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import java.net.URL;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.ListBoxModel;
import hudson.security.ACL;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;

/**
 * Associates {@link Job} with {@link JiraSite}.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraProjectProperty extends JobProperty<Job<?, ?>> {

    /**
     * Used to find {@link JiraSite}. Matches {@link JiraSite#getName()}. Always
     * non-null (but beware that this value might become stale if the system
     * config is changed.)
     */
    public final String siteName;
    private String jobCredentialId;
    private boolean useAlternativeCredential = false;
    private transient JiraSession jiraSession;

    @DataBoundConstructor
    public JiraProjectProperty(String siteName, boolean useAlternativeCredential, String jobCredentialId) {
        siteName = Util.fixEmptyAndTrim(siteName);
        if (siteName == null) {
            // defaults to the first one
            List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
            if (!sites.isEmpty()) {
                siteName = sites.get(0).getName();
            }
        }
        this.siteName = siteName;
        this.useAlternativeCredential = useAlternativeCredential;
        this.jobCredentialId = jobCredentialId;
    }

    /**
     * Get the jobCredentialId to display it in configuration screen
     * 
     * @return jobCredentialId
     */
    public String getJobCredentialId() {
        return jobCredentialId;
    }

    /**
     * Get useAlternativeCredential to display it in configuration screen
     * 
     * @return useAlternativeCredential
     */
	public boolean isUseAlternativeCredential() {
        return useAlternativeCredential;
    }

    /**
     * Gets the {@link JiraSite} that this project belongs to.
     *
     * @return null if the configuration becomes out of sync.
     */
    @Nullable
    public JiraSite getSite() {
        List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();

        if (siteName == null && sites.size() > 0) {
            // default
            return sites.get(0);
        }

        Stream<JiraSite> streams = sites.stream();
        if (owner != null) {
            Stream<JiraSite> stream2 = JiraFolderProperty.getSitesFromFolders(owner.getParent())
                .stream();
            streams = Stream.concat(streams, stream2).parallel();
        }
    
        return streams.filter(jiraSite -> jiraSite.getName().equals(siteName))
            .findFirst().orElse(null);
    }

    /**
     * Gets a remote access session to JIRA for this Item.
     * 
     * @param item
     * @return
     */
    public JiraSession getJiraProjectSession(Item item) {
        JiraSite jiraSite = getSite();
        if (jiraSite == null) {
        	throw new IllegalStateException("JIRA site needs to be configured in the project " + item.getFullDisplayName());
        }

        if (!this.useAlternativeCredential || null == this.jobCredentialId) {
            return jiraSite.getSession();
        }
        if (jiraSession == null) {
            jiraSession = jiraSite.getSession(this.jobCredentialId, item);
        }
    	return jiraSession;
    }

    /**
     * Gets a remote access session to JIRA.
     * Because it is a static method, it can be call by a job wich want to get a connection to Jira
     * 
     * @param job
     * @return
     */
    public static JiraSession getJiraProjectSession(Job<?, ?> job) {
        final JiraProjectProperty jiraProjectProperty = job.getProperty(JiraProjectProperty.class);
        if (jiraProjectProperty != null) {
            return jiraProjectProperty.getJiraProjectSession((Item) job);
        }
        return null;
    }

    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {
        @Deprecated
        protected transient List<JiraSite> sites;

        @Override
        @SuppressWarnings("unchecked")
        public boolean isApplicable(Class<? extends Job> jobType) {
            return Job.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraProjectProperty_DisplayName();
        }

        /**
         * @deprecated use {@link JiraGlobalConfiguration#setSites(List)} instead
         *
         * @param site the JIRA site
         */
        @Deprecated
        public void setSites(JiraSite site) {
            JiraGlobalConfiguration.get().getSites().add(site);
        }

        /**
         * @deprecated use {@link JiraGlobalConfiguration#getSites()} instead
         *
         * @return array of sites
         */
        @Deprecated
        public JiraSite[] getSites() {
            return JiraGlobalConfiguration.get().getSites().toArray(new JiraSite[0]);
        }

        @SuppressWarnings("unused") // Used by stapler
        public ListBoxModel doFillSiteNameItems(@AncestorInPath AbstractFolder<?> folder) {
            ListBoxModel items = new ListBoxModel();
            for (JiraSite site : JiraGlobalConfiguration.get().getSites()) {
                items.add(site.getName());
            }
            if (folder != null) {
                List<JiraSite> sitesFromFolder = JiraFolderProperty.getSitesFromFolders(folder);
                sitesFromFolder.stream().map(JiraSite::getName).forEach(items::add);
            }
            return items;
        }

        @SuppressWarnings("unused") // Used to start migration after all extensions are loaded
        @Initializer(after=InitMilestone.EXTENSIONS_AUGMENTED)
        public void migrate() {
            DescriptorImpl descriptor = (DescriptorImpl) Jenkins.getInstance()
                .getDescriptor(JiraProjectProperty.class);
            if (descriptor != null) {
                descriptor.load(); // force readResolve without registering descriptor as configurable
            }
        }

        @SuppressWarnings("deprecation") // Migrate configuration
        protected Object readResolve() {
            if (sites != null) {
                JiraGlobalConfiguration jiraGlobalConfiguration = (JiraGlobalConfiguration) Jenkins.getInstance()
                    .getDescriptorOrDie(JiraGlobalConfiguration.class);
                jiraGlobalConfiguration.load();
                jiraGlobalConfiguration.getSites().addAll(sites);
                jiraGlobalConfiguration.save();
                sites = null;
                DescriptorImpl oldDescriptor = (DescriptorImpl) Jenkins.getInstance()
                    .getDescriptor(JiraProjectProperty.class);
                if (oldDescriptor != null) {
                    oldDescriptor.save();
                }
            }
            return this;
        }

        /**
         * Fill credentials availables for the dropbox on the configuration screen
         * 
         * @param item
         * @param url
         * @param jobCredentialId
         * @return
         */
        public ListBoxModel doFillJobCredentialIdItems(@AncestorInPath Item item, @QueryParameter String url,
                @QueryParameter String jobCredentialId) {
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(ACL.SYSTEM, item, StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.fromUri(url).build(), CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class))
                    .includeCurrentValue(jobCredentialId);
        }

        /**
         * Call by the button test connection. Test the connection to Jira with the credential setted.
         * 
         * @param jobCredentialId
         * @param siteName
         * @param item
         * @return
         */
        public FormValidation doTestConnection(@QueryParameter String jobCredentialId,
                @QueryParameter String siteName, @AncestorInPath Item item) {
            JiraSite currentSite = null;
            for (JiraSite site : sites) {
                if (site.getName().equals(siteName)) {
                    currentSite = site;
                    break;
                }
            }
            if(null != currentSite) {
                JiraSession session = currentSite.getSession(jobCredentialId, item);
                if(session != null) {
                    return FormValidation.ok("Success");
                }
            }
            return FormValidation.error("Failed to login to JIRA");
        }
    }
}
