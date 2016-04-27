package hudson.plugins.jira.selector.jobs;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.RunScmChangeExtractor;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;

public abstract class JobIssueSelector extends AbstractIssueSelector {

    @Override
    public Set<String> findIssueIds(Run<?, ?> build, JiraSite site, TaskListener listener) {
        Set<String> ids = new HashSet<String>();
        addIssuesRecursive(build, ids, listener);
        printInfoAboutFoundIssues(ids, listener);
        return ids;
    }

    private void printInfoAboutFoundIssues(Set<String> ids, TaskListener listener) {
        if (ids.isEmpty()) {
            listener.getLogger().println("JIRA: Found no JIRA issues.");
        } else {
            String issues = StringUtils.join(ids, ",");
            listener.getLogger().println("JIRA: The following JIRA issues will be updated: " + issues);
        }
    }

    protected void addIssuesFromCurrentBuild(Run<?, ?> build, Set<String> issues, TaskListener listener) {
        addJobIdsFromBuild(build, issues, listener);
        DefaultIssueSelector.addIssuesFromParameters(build, issues);
    }

    protected abstract Logger getLogger();

    protected abstract void addJobIdsFromBuild(Run<?, ?> build, Set<String> issues, TaskListener listener);

    private void addIssuesRecursive(Run<?, ?> build, Set<String> ids, TaskListener listener) {
        DefaultIssueSelector.addCarriedOverIssues(build, ids);
        addIssuesFromCurrentBuild(build, ids, listener);
        addIssuesFromDependentBuilds(build, ids, listener);
    }

    protected void addIssuesFromDependentBuilds(Run<?, ?> build, Set<String> ids, TaskListener listener) {

        for (DependencyChange depc : RunScmChangeExtractor.getDependencyChanges(build.getPreviousBuild()).values()) {
            for (AbstractBuild<?, ?> b : depc.getBuilds()) {
                getLogger().finer("Searching for JIRA issues in dependency " + b + " of " + build);
                addIssuesRecursive(b, ids, listener);
            }
        }
    }

}
