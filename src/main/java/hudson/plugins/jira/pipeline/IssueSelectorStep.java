package hudson.plugins.jira.pipeline;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import hudson.model.Descriptor;
import hudson.model.Result;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.inject.Inject;

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
public class IssueSelectorStep extends AbstractStepImpl {

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

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(IssueSelectorStepExecution.class);
        }

        public Collection<? extends Descriptor<?>> getApplicableDescriptors() {
            return Jenkins.getInstance().getDescriptorList(AbstractIssueSelector.class);
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

    public static class IssueSelectorStepExecution extends AbstractSynchronousNonBlockingStepExecution<Set<String>> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient IssueSelectorStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run run;

        @Override
        protected Set<String> run() {
            return getOptionalJiraSite()
                .map(site -> step.getIssueSelector().findIssueIds(run, site, listener))
                .orElseGet(() -> {
                    listener.getLogger().println(Messages.NoJiraSite());
                    run.setResult(Result.FAILURE);
                    return new HashSet<>();
                });
        }

        Optional<JiraSite> getOptionalJiraSite() {
            return Optional.ofNullable(JiraSite.get(run.getParent()));
        }
    }

}
