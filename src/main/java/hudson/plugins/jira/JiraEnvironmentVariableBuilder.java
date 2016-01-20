package hudson.plugins.jira;

import hudson.AbortException;
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
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * Adds JIRA related environment variables to the build
 */
public class JiraEnvironmentVariableBuilder extends Builder  {
    
    private AbstractIssueSelector issueSelector;
    
    @DataBoundConstructor
    public JiraEnvironmentVariableBuilder(AbstractIssueSelector issueSelector) {
        this.issueSelector = issueSelector;
    }
    
    public AbstractIssueSelector getIssueSelector() {
        AbstractIssueSelector uis = this.issueSelector;
        if (uis == null) uis = new DefaultIssueSelector();
        return (this.issueSelector = uis);
    }
    
    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        JiraSite site = getSiteForProject(build.getProject());

        if (site == null) {
            throw new AbortException(Messages.JiraEnvironmentVariableBuilder_NoJiraSite());
        }
        
        Set<String> ids = getIssueSelector().findIssueIds(build, site, listener);

        String idList = StringUtils.join(ids, ",");

        listener.getLogger().println(Messages.JiraEnvironmentVariableBuilder_Updating(JiraEnvironmentContributingAction.ISSUES_VARIABLE_NAME, idList));
 
        build.addAction(new JiraEnvironmentContributingAction(idList, site.getName()));
        
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
    }
}
