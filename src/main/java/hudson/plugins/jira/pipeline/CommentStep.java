package hudson.plugins.jira.pipeline;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Simple add comment step.
 *
 * @author jan zajic
 */
public class CommentStep extends Step {

    public final String issueKey;

    public final String body;

    @DataBoundConstructor
    public CommentStep(@NonNull String issueKey, @NonNull String body) {
        this.issueKey = issueKey;
        this.body = body;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getBody() {
        return body;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new CommentStepExecution(this, context);
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, Run.class, TaskListener.class);
            return Collections.unmodifiableSet(context);
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
    public static class CommentStepExecution extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        private final transient CommentStep step;

        protected CommentStepExecution(CommentStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            JiraSite site = JiraSite.get(getContext().get(Run.class).getParent());
            if (site == null) {
                return null;
            }
            JiraSession session = site.getSession(getContext().get(Run.class).getParent());
            if (session == null) {
                getContext().get(TaskListener.class).getLogger().println(Messages.FailedToConnect());
                return null;
            }

            session.addComment(step.issueKey, step.body, site.groupVisibility, site.roleVisibility);
            return null;
        }
    }
}
