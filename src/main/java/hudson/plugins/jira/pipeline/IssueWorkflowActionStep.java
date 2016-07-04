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

/**
 * Created by aleksandr on 01.07.16.
 */
public class IssueWorkflowActionStep  extends AbstractStepImpl {
    private final String issueKey;
    private final String workflowActionName;

    @DataBoundConstructor
    public IssueWorkflowActionStep(@Nonnull String issueKey, @Nonnull String workflowActionName) {
        this.issueKey = Util.fixEmptyAndTrim(issueKey);
        this.workflowActionName = Util.fixEmptyAndTrim(workflowActionName);
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getWorkflowActionName() {
        return workflowActionName;
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(IssueWorkflowActionStep.StepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "jiraIssueWorkflowStep";
        }

        @Override
        public String getDisplayName() {
            return Messages.IssueWorkflowActionStep_Descriptor_DisplayName();
        }
    }

    public static class StepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient IssueWorkflowActionStep step;

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

            listener.getLogger().println("[JIRA] Migrate issue with key: " + step.getIssueKey() +
                    " to step: " + step.getWorkflowActionName()
            );

            try {
                long originalStatusId = site.getSession()
                        .getIssue(step.getIssueKey())
                        .getStatus()
                        .getId();

                Integer actionId = site.getSession().getActionIdForIssue(step.getIssueKey(), step.getWorkflowActionName());
                if (actionId == null) {
                    throw new RuntimeException("Workflow action does not exists");
                }

                site.getSession().progressWorkflowAction(step.getIssueKey(), actionId);

                long currentStatusId = site.getSession()
                        .getIssue(step.getIssueKey())
                        .getStatus()
                        .getId();

                if (originalStatusId == currentStatusId){
                    listener.getLogger().println(Messages.IssueWorkflowActionStep_Failed());
                    run.setResult(Result.FAILURE);
                }
            } catch (Exception e) {
                listener.getLogger().println(Messages.IssueWorkflowActionStep_Failed());
                e.printStackTrace(listener.getLogger());
                run.setResult(Result.FAILURE);
            }

            return null;
        }
    }
}