package hudson.plugins.jira.pipeline;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import org.apache.commons.lang.StringUtils;
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
public class IssueUpdateStep extends AbstractStepImpl {
    private final String jqlSearch;
    private final String workflowActionName;
    private final String comment;

    @DataBoundConstructor
    public IssueUpdateStep(@Nonnull String jqlSearch, @Nonnull String workflowActionName, String comment) {
        this.jqlSearch = Util.fixEmptyAndTrim(jqlSearch);
        this.workflowActionName = Util.fixEmptyAndTrim(workflowActionName);
        this.comment = Util.fixEmptyAndTrim(comment);
    }

    public String getJqlSearch() {
        return jqlSearch;
    }

    public String getWorkflowActionName() {
        return workflowActionName;
    }

    public String getComment() {
        return comment;
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(IssueUpdateStep.StepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "jiraIssueUpdate";
        }

        @Override
        public String getDisplayName() {
            return Messages.IssueUpdateStep_Descriptor_DisplayName();
        }
    }

    public static class StepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient IssueUpdateStep step;

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

            if (StringUtils.isNotEmpty(step.getWorkflowActionName())) {
                listener.getLogger().println(Messages.JiraIssueUpdateBuilder_UpdatingWithAction(step.getWorkflowActionName()));
            }

            listener.getLogger().println("[JIRA] JQL: " + step.getJqlSearch());

            try {
                if (!site.progressMatchingIssues(step.getJqlSearch(), step.workflowActionName, step.getComment(), listener.getLogger())) {
                    listener.getLogger().println(Messages.JiraIssueUpdateBuilder_SomeIssuesFailed());
                    run.setResult(Result.UNSTABLE);
                }
            } catch (IOException e) {
                listener.getLogger().println(Messages.JiraIssueUpdateBuilder_Failed());
                e.printStackTrace(listener.getLogger());
                run.setResult(Result.FAILURE);
            }

            return null;
        }
    }
}
