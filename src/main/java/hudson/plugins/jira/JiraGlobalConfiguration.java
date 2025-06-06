package hudson.plugins.jira;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.util.PersistedList;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;

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
        this.sites = sites;
        save();
    }
}
