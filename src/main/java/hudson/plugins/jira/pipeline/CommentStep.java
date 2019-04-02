package hudson.plugins.jira.pipeline;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Simple add comment step.
 *
 * @author jan zajic
 */
public class CommentStep extends AbstractStepImpl {

    public final String issueKey;

    public final String body;

    @DataBoundConstructor
    public CommentStep(@Nonnull String issueKey, @Nonnull String body) {
        this.issueKey = issueKey;
        this.body = body;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getBody() {
        return body;
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CommentStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "jiraComment";
        }

        @Override
        public String getDisplayName() {
            return Messages.CommentStep_Descriptor_DisplayName();
        }
    }

    /**
     * @author jan zajic
     */
    public static class CommentStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient CommentStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run run;

        @Override
        protected Void run() {
            JiraSite site = JiraSite.get(run.getParent());
            JiraSession session = site.getSession();
            if (session == null) {
                listener.getLogger().println(Messages.FailedToConnect());
                return null;
            }

            session.addComment(step.issueKey, step.body, site.groupVisibility, site.roleVisibility);
            return null;
        }

    }

}
