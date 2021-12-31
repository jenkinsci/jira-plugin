package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.ItemGroup;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides folder level Jira configuration.
 */
public class JiraFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {
    /**
     * Hold the Jira sites configuration.
     */
    private List<JiraSite> sites = Collections.emptyList();

    /**
     * Constructor.
     */
    @DataBoundConstructor
    public JiraFolderProperty() {
    }

    /**
     * Return the Jira sites.
     *
     * @return the Jira sites
     */
    public JiraSite[] getSites() {
        return sites.toArray(new JiraSite[0]);
    }

    /**
     * @deprecated use {@link #setSites(List)} instead
     *
     * @param site the Jira site
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
     * @deprecated use {@link JiraSite#getSitesFromFolders(ItemGroup)}
     */
    @Deprecated
    public static List<JiraSite> getSitesFromFolders(ItemGroup itemGroup) {
        return JiraSite.getSitesFromFolders(itemGroup);
    }

    /**
     * Descriptor class.
     */
    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.JiraFolderProperty_DisplayName();
        }
    }
}
