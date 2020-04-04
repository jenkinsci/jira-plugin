package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.ItemGroup;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
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

    @DataBoundSetter
    public void setSites(List<JiraSite> sites) {
        this.sites = sites;
    }

    public static List<JiraSite> getSitesFromFolders(ItemGroup itemGroup) {
        List<JiraSite> result = new ArrayList<>();
        while (itemGroup instanceof AbstractFolder<?>) {
            AbstractFolder<?> folder = (AbstractFolder<?>) itemGroup;
            JiraFolderProperty jiraFolderProperty = folder.getProperties()
                .get(JiraFolderProperty.class);
            if (jiraFolderProperty != null && jiraFolderProperty.getSites().length != 0) {
                result.addAll(Arrays.asList(jiraFolderProperty.getSites()));
            }
            itemGroup = folder.getParent();
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
