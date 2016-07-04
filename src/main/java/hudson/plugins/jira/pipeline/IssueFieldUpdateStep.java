package hudson.plugins.jira.pipeline;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Step for updating jira issue with workflow migration
 *
 * @author aatarasoff
 */
public class IssueFieldUpdateStep extends AbstractStepImpl {
    private final String issueKey;
    private final String fieldName;
    private final String fieldValue;

    @DataBoundConstructor
    public IssueFieldUpdateStep(@Nonnull String issueKey, @Nonnull String fieldName, @Nonnull String fieldValue) {
        this.issueKey = Util.fixEmptyAndTrim(issueKey);
        this.fieldName = Util.fixEmptyAndTrim(fieldName);
        this.fieldValue = Util.fixEmptyAndTrim(fieldValue);
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(IssueFieldUpdateStep.StepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "jiraIssueFieldUpdate";
        }

        @Override
        public String getDisplayName() {
            return Messages.IssueFieldUpdateStep_Descriptor_DisplayName();
        }
    }

    public static class StepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient IssueFieldUpdateStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run run;

        @Override
        protected Void run() throws Exception {
            JiraSite site = JiraSite.get(run.getParent());

            if (site == null) {
                listener.getLogger().println(Messages.Updater_NoJiraSite());
                run.setResult(Result.FAILURE);
            }

            listener.getLogger().println("[JIRA] Update issue with key: " + step.getIssueKey());

            try {
                if (!site.getSession().updateIssueFieldValue(step.getIssueKey(), step.getFieldName(), step.getFieldValue())) {
                    listener.getLogger().println(Messages.IssueFieldUpdateStep_Failed());
                    run.setResult(Result.UNSTABLE);
                }
            } catch (IOException e) {
                listener.getLogger().println(Messages.IssueFieldUpdateStep_Failed());
                e.printStackTrace(listener.getLogger());
                run.setResult(Result.FAILURE);
            }

            return null;
        }
    }
}
