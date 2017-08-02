package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.CopyOnWriteList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Provides folder level JIRA configuration.
 */
public class JiraFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {
    /**
     * Hold the JIRA sites configuration.
     */
    private final CopyOnWriteList<JiraSite> sites = new CopyOnWriteList<JiraSite>();

    /**
     * Constructor.
     */
    @DataBoundConstructor
    public JiraFolderProperty() {
    }

    @Override
    public AbstractFolderProperty<?> reconfigure(StaplerRequest req, JSONObject formData)
            throws Descriptor.FormException {
        if (formData == null) {
            return null;
        }
        //Fix^H^H^HDirty hack for empty string to URL conversion error
        //Should check for existing handler etc, but since this is a dirty hack,
        //we won't
        Stapler.CONVERT_UTILS.deregister(java.net.URL.class);
        Stapler.CONVERT_UTILS.register(new EmptyFriendlyURLConverter(), java.net.URL.class);
        //End hack

        sites.replaceBy(req.bindJSONToList(JiraSite.class, formData.get("sites")));
        return this;
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
     * Adds a JIRA site.
     *
     * @param site the JIRA site
     */
    @DataBoundSetter
    public void setSites(JiraSite site) {
        sites.add(site);
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
