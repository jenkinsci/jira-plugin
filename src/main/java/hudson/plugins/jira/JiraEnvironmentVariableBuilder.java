package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.Set;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Adds Jira related environment variables to the build
 */
public class JiraEnvironmentVariableBuilder extends Builder {

    private AbstractIssueSelector issueSelector;

    @DataBoundConstructor
    public JiraEnvironmentVariableBuilder(AbstractIssueSelector issueSelector) {
        this.issueSelector = issueSelector;
    }

    public AbstractIssueSelector getIssueSelector() {
        AbstractIssueSelector uis = this.issueSelector;
        if (uis == null) {
            uis = new DefaultIssueSelector();
        }
        return (this.issueSelector = uis);
    }

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        JiraSite site = getSiteForProject(build.getProject());

        if (site == null) {
            listener.getLogger().println(Messages.JiraEnvironmentVariableBuilder_NoJiraSite());
            return false;
        }

        Set<String> ids;
        try {
            ids = getIssueSelector().findIssueIds(build, site, listener);
        } catch (RestClientException e) {
            listener.getLogger().println(e.getMessage());
            return false;
        }

        String idList = StringUtils.join(ids, ",");
        Integer idListSize = ids.size();

        listener.getLogger()
                .println(Messages.JiraEnvironmentVariableBuilder_Updating(
                        JiraEnvironmentContributingAction.ISSUES_VARIABLE_NAME, idList));
        listener.getLogger()
                .println(Messages.JiraEnvironmentVariableBuilder_Updating(
                        JiraEnvironmentContributingAction.ISSUES_SIZE_VARIABLE_NAME, idListSize));

        build.addAction(new JiraEnvironmentContributingAction(idList, idListSize, site.getName()));

        return true;
    }

    /**
     * Descriptor for {@link JiraEnvironmentVariableBuilder}.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> klass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraEnvironmentVariableBuilder_DisplayName();
        }

        public boolean hasIssueSelectors() {
            return Jenkins.get().getDescriptorList(AbstractIssueSelector.class).size() > 0;
        }
    }
}
