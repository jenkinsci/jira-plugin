package hudson.plugins.jira.pipeline;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import hudson.plugins.jira.model.JiraIssueField;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Issue custom fields updater
 * 
 * @author Dmitry Frolov tekillaz.dev@gmail.com
 * 
 */
public class IssueFieldUpdateStep extends Builder implements SimpleBuildStep {

    private AbstractIssueSelector issueSelector;

    public AbstractIssueSelector getIssueSelector() {
        return this.issueSelector;
    }

    @DataBoundSetter
    public void setIssueSelector(AbstractIssueSelector issueSelector) {
        this.issueSelector = issueSelector;
    }

    public String fieldId;

    public String getFieldId() {
        return this.fieldId;
    }

    @DataBoundSetter
    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String fieldValue;

    public String getFieldValue() {
        return this.fieldValue;
    }

    @DataBoundSetter
    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    @DataBoundConstructor
    public IssueFieldUpdateStep(AbstractIssueSelector issueSelector, String fieldId, String fieldValue) {
        this.issueSelector = issueSelector;
        this.fieldId = fieldId;
        this.fieldValue = fieldValue;
    }

    public String prepareFieldId(String fieldId) {
        String prepared = fieldId;
        if (!prepared.startsWith("customfield_"))
            prepared = "customfield_" + prepared;
        return prepared;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException {
        PrintStream logger = listener.getLogger();

        AbstractIssueSelector selector = issueSelector;
        if (selector == null) {
            logger.println("[JIRA][IssueFieldUpdateStep] No issue selector found!");
            throw new IOException("[JIRA][IssueFieldUpdateStep] No issue selector found!");
        }

        JiraSite site = JiraSite.get(run.getParent());
        if (site == null) {
            logger.println(Messages.NoJiraSite());
            run.setResult(Result.FAILURE);
            return;
        }

        JiraSession session = site.getSession();
        if (session == null) {
            logger.println(Messages.NoRemoteAccess());
            run.setResult(Result.FAILURE);
            return;
        }

        Set<String> issues = selector.findIssueIds(run, site, listener);
        if (issues.isEmpty()) {
            logger.println("[JIRA][IssueFieldUpdateStep] Issue list is empty!");
            return;
        }

        List<JiraIssueField> fields = new ArrayList();
        fields.add(new JiraIssueField(prepareFieldId(fieldId), fieldValue));

        for (String issue : issues) {
            submitFields(session, issue, fields, logger);
        }
    }

    public void submitFields(JiraSession session, String issueId, List<JiraIssueField> fields, PrintStream logger) {
        try {
            session.addFields(issueId, fields);
        } catch (RestClientException e) {

            if (e.getStatusCode().or(0).equals(404)) {
                logger.println(issueId + " - JIRA issue not found");
            }

            if (e.getStatusCode().or(0).equals(403)) {
                logger.println(issueId
                        + " - Jenkins JIRA user does not have permissions to comment on this issue");
            }

            if (e.getStatusCode().or(0).equals(401)) {
                logger.println(
                        issueId + " - Jenkins JIRA authentication problem");
            }

            logger.println(Messages.FailedToUpdateIssue(issueId));
            logger.println(e.getLocalizedMessage());
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckField_id(@QueryParameter String value) throws IOException, ServletException {
            if (Util.fixNull(value).trim().length() == 0)
                return FormValidation.warning(Messages.JiraIssueFieldUpdater_NoIssueFieldID());
            if (!value.matches("\\d+"))
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
