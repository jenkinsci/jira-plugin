package hudson.plugins.jira;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.google.common.collect.Lists;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

/**
 * Issue custom fields updater
 * @author Dmitry Frolov tekillaz.dev@gmail.com
 * 
 */
public class JiraIssueFieldUpdater extends Builder implements SimpleBuildStep {
		
	private AbstractIssueSelector issueSelector;
	
	public AbstractIssueSelector getIssueSelector() {
		return this.issueSelector;
	}
	
	@DataBoundSetter
	public void setIssueSelector(AbstractIssueSelector issueSelector) {
		this.issueSelector = issueSelector;
	}
	
	
	public String field_id;
	
	public String getFieldId() {
		return this.field_id;
	}
	
	@DataBoundSetter
	public void setFieldId(String field_id) {
		this.field_id = field_id;
	}
	
	
	public String field_value;
	
	public String getFieldValue() {
		return this.field_value;
	}
	
	@DataBoundSetter
	public void setFieldValue(String field_value) {
		this.field_value = field_value;
	}
	
	
	
	
	@DataBoundConstructor
	public JiraIssueFieldUpdater(AbstractIssueSelector issueSelector, String field_id, String field_value ) {
		this.issueSelector = issueSelector;
		this.field_id = field_id;
		this.field_value = field_value;
	}
	
	public String prepare_field_id(String field_id) {
		String prepared = field_id;
		if( !prepared.startsWith("customfield_") )
			prepared = "customfield_" + prepared;
		return prepared;
	}
	
	
	
	
	@Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) 
    		throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		
		AbstractIssueSelector selector = issueSelector;
		if( selector == null ) {
			logger.println("[JIRA][JiraIssueFieldUpdater] No issue selector found!");
			throw new IOException("[JIRA][JiraIssueFieldUpdater] No issue selector found!");
		}
		
		JiraSite site = JiraSite.get(run.getParent());
        if (site == null) {
            logger.println(Messages.Updater_NoJiraSite());
            run.setResult(Result.FAILURE);
            return;
        }
        
        JiraSession session = null;
        try {
            session = site.getSession();
        } catch (IOException e) {
            listener.getLogger().println(Messages.Updater_FailedToConnect());
            e.printStackTrace(listener.getLogger());
        }
        if (session == null) {
            logger.println(Messages.Updater_NoRemoteAccess());
            run.setResult(Result.FAILURE);
            return;
        }
        
        Set<String> issues = selector.findIssueIds(run, site, listener);
        if( issues.isEmpty() ) {
        	logger.println("[JIRA][JiraIssueFieldUpdater] Issue list is empty!");
        	return;
        }
        
        List<JiraIssueField> fields = Lists.newArrayList();
        fields.add(new JiraIssueField(prepare_field_id(field_id), field_value));
        
        for( String issue : issues ) {
        	submitFeilds( session, issue, fields, logger );        
        }   
    }
	
	public void submitFeilds( JiraSession session, String issueId, List<JiraIssueField> fields, PrintStream logger ) {
    	try {
    		session.addFields(issueId, fields);
    	} catch (RestClientException e) {

            if (e.getStatusCode().or(0).equals(404)) {
                logger.println(issueId + " - JIRA issue not found. Dropping comment from update queue.");
            }

            if (e.getStatusCode().or(0).equals(403)) {
                logger.println(issueId + " - Jenkins JIRA user does not have permissions to comment on this issue. Preserving comment for future update.");
            }

            if (e.getStatusCode().or(0).equals(401)) {
                logger.println(issueId + " - Jenkins JIRA authentication problem. Preserving comment for future update.");
            }

            logger.println(Messages.Updater_FailedToCommentOnIssue(issueId));
            logger.println(e.getLocalizedMessage());
        }
	}
	
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}
	
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	
        public FormValidation doCheckField_id(@QueryParameter String value) 
        		throws IOException, ServletException {
            if (Util.fixNull(value).trim().length() == 0)
                return FormValidation.warning(Messages.JiraIssueFieldUpdater_NoIssueFieldID());
        	if(!value.matches("\\d+"))
        		return FormValidation.error(Messages.JiraIssueFieldUpdater_NotAtIssueFieldID());
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraIssueFieldUpdater_DisplayName();
        }
    }

}
