package hudson.plugins.jira.selector;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.Sets;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static hudson.Util.fixNull;

public class JqlIssueSelector extends AbstractIssueSelector {

    private String jql;

    @DataBoundConstructor
    public JqlIssueSelector(String jql) {
        super();
        this.jql = jql;
    }

    public void setJql(String jql){
        this.jql = jql;
    }

    public String getJql() {
        return jql;
    }

    @Override
    public Set<String> findIssueIds(Run<?, ?> run, JiraSite site, TaskListener listener) {
        try {
            JiraSession session = site.getSession();
            if (session == null)
                throw new IllegalStateException("Remote access for JIRA isn't configured in Jenkins");

            List<Issue> issues = session.getIssuesFromJqlSearch(jql);

            List<String> issueKeys = new ArrayList<>();

            for (Issue issue : fixNull(issues)) {
                issueKeys.add(issue.getKey());
            }

            // deduplication
            return Sets.newHashSet(issueKeys);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Can't open rest session to JIRA site " + site, e);
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {
        @Override
        public String getDisplayName() {
            return Messages.IssueSelector_JqlIssueSelector_DisplayName();
        }
    }
}
