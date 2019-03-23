package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.ListBoxModel;
import java.util.List;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

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

    @DataBoundConstructor
    public JiraProjectProperty(String siteName) {
        siteName = Util.fixEmptyAndTrim(siteName);
        if (siteName == null) {
            // defaults to the first one
            List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
            if (!sites.isEmpty()) {
                siteName = sites.get(0).getName();
            }
        }
        this.siteName = siteName;
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

        for (JiraSite site : sites) {
            if (site.getName().equals(siteName)) {
                return site;
            }
        }

        return null;
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
        public ListBoxModel doFillSiteNameItems() {
            ListBoxModel items = new ListBoxModel();
            for (JiraSite site : JiraGlobalConfiguration.get().getSites()) {
                items.add(site.getName());
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
    }
}
