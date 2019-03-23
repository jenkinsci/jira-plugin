package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import java.util.Collections;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Provides folder level JIRA configuration.
 */
public class JiraFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {
    /**
     * Hold the JIRA sites configuration.
     */
    private List<JiraSite> sites = Collections.emptyList();

    /**
     * Constructor.
     */
    @DataBoundConstructor
    public JiraFolderProperty() {
    }

    /**
     * Return the JIRA sites.
     *
     * @return the JIRA sites
     */
    public JiraSite[] getSites() {
        return sites.toArray(new JiraSite[0]);
    }

    /**
     * @deprecated use {@link #setSites(List)} instead
     *
     * @param site the JIRA site
     */
    @Deprecated
    public void setSites(JiraSite site) {
        sites.add(site);
    }

    @DataBoundSetter
    public void setSites(List<JiraSite> sites) {
        this.sites = sites;
    }

    /**
     * Descriptor class.
     */
    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.JiraFolderProperty_DisplayName();
        }
    }
}
