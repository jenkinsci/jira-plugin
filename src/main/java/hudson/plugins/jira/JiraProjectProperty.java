package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.ListBoxModel;
import java.util.List;
import java.util.stream.Stream;
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

        Stream<JiraSite> streams = sites.stream();
        if (owner != null) {
            Stream<JiraSite> stream2 = JiraFolderProperty.getSitesFromFolders(owner.getParent())
                .stream();
            streams = Stream.concat(streams, stream2).parallel();
        }

        return streams.filter(jiraSite -> jiraSite.getName().equals(siteName))
            .findFirst().orElse(null);
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        @SuppressWarnings("unchecked")
        public boolean isApplicable(Class<? extends Job> jobType) {
            return Job.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraProjectProperty_DisplayName();
        }
    }
}
