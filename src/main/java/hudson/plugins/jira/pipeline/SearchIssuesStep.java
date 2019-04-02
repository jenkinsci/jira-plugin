package hudson.plugins.jira.pipeline;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.inject.Inject;
import hudson.AbortException;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Simple search issues step
 *
 * @author jan zajic
 */
public class SearchIssuesStep extends AbstractStepImpl {

    public final String jql;

    @DataBoundConstructor
    public SearchIssuesStep(@Nonnull String jql) {
        this.jql = jql;
    }

    public String getJql() {
        return jql;
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(SearchStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "jiraSearch";
        }

        @Override
        public String getDisplayName() {
            return Messages.SearchIssuesStep_Descriptor_DisplayName();
        }
    }

    /**
     * @author jan zajic
     */
    public static class SearchStepExecution extends AbstractSynchronousNonBlockingStepExecution<List<String>> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient SearchIssuesStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run run;

        @Override
        protected List<String> run() throws Exception {
            JiraSite site = JiraSite.get(run.getParent());
            JiraSession session = site.getSession();
            if (session == null) {
                listener.getLogger().println(Messages.FailedToConnect());
                throw new AbortException("Cannot open jira session - error occurred");
            }

            List<String> resultList = new ArrayList<>();
            List<Issue> issuesFromJqlSearch = session.getIssuesFromJqlSearch(step.jql);
            for (Issue issue : issuesFromJqlSearch)
                resultList.add(issue.getKey());
            return resultList;
        }

    }

}
