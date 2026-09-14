package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ItemGroup;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Provides folder level Jira configuration.
 */
public class JiraFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {
    /**
     * Hold the Jira sites configuration.
     *
     * <p>Never the immutable {@code Collections.emptyList()} it used to default to: {@link
     * #setSites(JiraSite)} adds to this list in place, so a freshly constructed property threw
     * {@link UnsupportedOperationException} on every call.
     */
    private List<JiraSite> sites = new ArrayList<>();

    /**
     * Constructor.
     */
    @DataBoundConstructor
    public JiraFolderProperty() {}

    /**
     * Return the Jira sites.
     *
     * @return the Jira sites
     */
    public JiraSite[] getSites() {
        return sites.toArray(new JiraSite[0]);
    }

    /**
     * @param site the Jira site
     * @deprecated use {@link #setSites(List)} instead
     */
    @Deprecated
    public void setSites(JiraSite site) {
        List<JiraSite> updated = new ArrayList<>(this.sites);
        updated.add(site);
        this.sites = updated;
    }

    @DataBoundSetter
    public void setSites(List<JiraSite> sites) {
        // Copy rather than alias: callers pass Arrays.asList(...) and other fixed-size lists, and used
        // to find their own list mutated by the single-site setter above.
        this.sites = sites == null ? new ArrayList<>() : new ArrayList<>(sites);
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
