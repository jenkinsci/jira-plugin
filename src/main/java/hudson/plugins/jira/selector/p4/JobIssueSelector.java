package hudson.plugins.jira.selector.p4;

import java.util.Set;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.selector.AbstractIssueSelector;

public class JobIssueSelector extends AbstractIssueSelector {

    @Override
    public Set<String> findIssueIds(Run<?, ?> run, JiraSite site, TaskListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

}
