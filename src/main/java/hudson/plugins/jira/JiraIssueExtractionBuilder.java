package hudson.plugins.jira;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * Extracts a list of relevant (i.e. referenced in commit messages in the build changeset) issues
 * to a properties file so it can be used in later steps
 * 
 * @author sfhardma
 * 
 */
public class JiraIssueExtractionBuilder extends Builder  {
    
    private UpdaterIssueSelector issueSelector;
    
    private final String issuesPropertyName;
    
    private final String propertiesFilePath;
    
    @DataBoundConstructor
    public JiraIssueExtractionBuilder(UpdaterIssueSelector issueSelector, String issuesPropertyName,
            String propertiesFilePath) {
        this.issuesPropertyName = Util.fixEmptyAndTrim(issuesPropertyName);
        this.propertiesFilePath = Util.fixEmptyAndTrim(propertiesFilePath);
        this.issueSelector = issueSelector;
    }
    
    public String getIssuesPropertyName() {
        return issuesPropertyName;
    }
    
    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }
    
    public UpdaterIssueSelector getIssueSelector() {
        UpdaterIssueSelector uis = this.issueSelector;
        if (uis == null) uis = new DefaultUpdaterIssueSelector();
        return (this.issueSelector = uis);
    }
    
    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }
    
    FilePath getRemoteFilePath(VirtualChannel channel, String filePath) {
        return new FilePath(channel, filePath);
    }
    
    VirtualChannel getChannel(AbstractBuild build)
    {
        return build.getBuiltOn().getChannel();
    }
     /**
     * Performs the actual update based on job configuration.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String realIssuesPropertyName = Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(issuesPropertyName));
        String realPropertiesFilePath = Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(propertiesFilePath));
        
        JiraSite site = getSiteForProject(build.getProject());

        if (site == null) {
            throw new AbortException("JIRA site not configured");
        }
        
        Set<String> ids = getIssueSelector().findIssueIds(build, site, listener);

        String idList = StringUtils.join(ids, ",");

        listener.getLogger().println(Messages.JiraIssueExtractionBuilder_Updating(realIssuesPropertyName, idList, realPropertiesFilePath));
        
        FilePath remoteFilePath = getRemoteFilePath(getChannel(build), realPropertiesFilePath);
        
        remoteFilePath.write(realIssuesPropertyName+"="+idList, null);
        
        return true;
    }

    /**
     * Descriptor for {@link JiraIssueExtractionBuilder}.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckIssuesPropertyName(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error(Messages.JiraIssueExtractionBuilder_NoIssuesPropertyName());
            }

            return FormValidation.ok();
        }
        
        public FormValidation doCheckPropertiesFilePath(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error(Messages.JiraIssueExtractionBuilder_NoPropertiesFilePath());
            }

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> klass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraIssueExtractionBuilder_DisplayName();
        }
    }
}
