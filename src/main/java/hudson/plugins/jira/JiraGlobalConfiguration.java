package hudson.plugins.jira;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.util.PersistedList;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

@Extension
public class JiraGlobalConfiguration extends GlobalConfiguration {

    @NonNull
    public static JiraGlobalConfiguration get() {
        return (JiraGlobalConfiguration) Jenkins.get().getDescriptorOrDie(JiraGlobalConfiguration.class);
    }

    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public List<JiraSite> sites = new PersistedList<>(this);

    public JiraGlobalConfiguration() {
        load();
    }

    public List<JiraSite> getSites() {
        return sites;
    }

    @DataBoundSetter
    public void setSites(List<JiraSite> sites) {
        this.sites = Util.fixNull(sites);
        save();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject formData) throws FormException {
        // list must be bind additionally because when the list is cleared
        // then setSites() method is not invoked and previous values persists incorrectly
        sites = req.bindJSONToList(JiraSite.class, formData.get("sites"));
        req.bindJSON(this, formData);
        save();

        return super.configure(req, formData);
    }
}
