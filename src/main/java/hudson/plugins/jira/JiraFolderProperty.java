package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static List<JiraSite> getSitesFromFolders(ItemGroup itemGroup) {
        List<JiraSite> result = new ArrayList<>();
        while (itemGroup != null) {
            if (itemGroup instanceof AbstractFolder<?>) {
                AbstractFolder<?> folder = (AbstractFolder<?>) itemGroup;
                JiraFolderProperty jiraFolderProperty = folder.getProperties()
                    .get(JiraFolderProperty.class);
                if (jiraFolderProperty != null && jiraFolderProperty.getSites().length != 0) {
                    result.addAll(Arrays.asList(jiraFolderProperty.getSites()));
                }
                itemGroup = folder.getParent();
            }

            if (!(itemGroup instanceof AbstractFolder<?>)) {
                itemGroup = null;
            }
        }
        return result;
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
