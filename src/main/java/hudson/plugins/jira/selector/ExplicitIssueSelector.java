package hudson.plugins.jira.selector;

import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;

public class ExplicitIssueSelector extends AbstractIssueSelector {

    @CheckForNull
    private List<String> jiraIssueKeys;

    @DataBoundConstructor
    public ExplicitIssueSelector(String jiraIssueKeys) {
        this(Lists.newArrayList(jiraIssueKeys.split(",")));
    }

    public ExplicitIssueSelector(List<String> jiraIssueKeys) {
        this.jiraIssueKeys = jiraIssueKeys;
    }

    @Override
    public Set<String> findIssueIds(Run<?, ?> run, JiraSite site, TaskListener listener) {
        return Sets.newHashSet(jiraIssueKeys);
    }

}
