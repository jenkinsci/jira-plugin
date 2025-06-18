package hudson.plugins.jira.pipeline;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Simple search issues step
 *
 * @author jan zajic
 */
public class SearchIssuesStep extends Step {

    public final String jql;

    @DataBoundConstructor
    public SearchIssuesStep(@NonNull String jql) {
        this.jql = jql;
    }

    public String getJql() {
        return jql;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SearchStepExecution(this, context);
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
    public static class SearchStepExecution extends SynchronousNonBlockingStepExecution<List<String>> {

        private static final long serialVersionUID = 1L;

        private final transient SearchIssuesStep step;

        protected SearchStepExecution(SearchIssuesStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<String> run() throws Exception {
            JiraSite site = JiraSite.get(getContext().get(Run.class).getParent());
            JiraSession session = site.getSession(getContext().get(Run.class).getParent());
            if (session == null) {
                getContext().get(TaskListener.class).getLogger().println(Messages.FailedToConnect());
                throw new AbortException("Cannot open Jira session - error occurred");
            }

            List<String> resultList = new ArrayList<>();
            try {
                List<Issue> issuesFromJqlSearch = session.getIssuesFromJqlSearch(step.jql);
                for (Issue issue : issuesFromJqlSearch) {
                    resultList.add(issue.getKey());
                }
            } catch (RestClientException e) {
                getContext().get(TaskListener.class).getLogger().println(e.getMessage());
            }
            return resultList;
        }
    }
}
