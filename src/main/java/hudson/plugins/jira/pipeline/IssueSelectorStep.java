package hudson.plugins.jira.pipeline;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.model.Descriptor;
import hudson.model.Result;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import hudson.plugins.jira.selector.AbstractIssueSelector;

/**
 * Step that run selected issue selector.
 *
 * @see hudson.plugins.jira.selector.AbstractIssueSelector
 */
public class IssueSelectorStep extends Step {

    private AbstractIssueSelector issueSelector;

    @DataBoundConstructor
    public IssueSelectorStep() {
    }

    @DataBoundSetter
    public void setIssueSelector(AbstractIssueSelector issueSelector) {
        this.issueSelector = issueSelector;
    }

    public AbstractIssueSelector getIssueSelector() {
        return issueSelector;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new IssueSelectorStepExecution(this, context);
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends StepDescriptor {

        public Collection<? extends Descriptor<?>> getApplicableDescriptors() {
            return Jenkins.get().getDescriptorList(AbstractIssueSelector.class);
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, Run.class, TaskListener.class);
            return Collections.unmodifiableSet(context);
        }

        @Override
        public String getFunctionName() {
            return "jiraIssueSelector";
        }

        @Override
        public String getDisplayName() {
            return Messages.IssueSelectorStep_Descriptor_DisplayName();
        }
    }

    public static class IssueSelectorStepExecution extends SynchronousNonBlockingStepExecution<Set<String>> {

        private static final long serialVersionUID = 1L;

        private final transient IssueSelectorStep step;

        protected IssueSelectorStepExecution(IssueSelectorStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Set<String> run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            Run run = getContext().get(Run.class);
            return Optional.ofNullable(JiraSite.get(run.getParent()))
                .map(site -> step.getIssueSelector().findIssueIds(run, site, listener))
                .orElseGet(() -> {
                    listener.getLogger().println(Messages.NoJiraSite());
                    run.setResult(Result.FAILURE);
                    return new HashSet<>();
                });
        }
    }

}
