package hudson.plugins.jira.pipeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.inject.Inject;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.plugins.jira.Messages;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

/**
 * Find all issues referenced by the commit logs of current build
 *
 * @author emilio escobar
 */
public class FindIssuesAtCommitLogsStep extends AbstractStepImpl {

    public static final String DEFAULT_ISSUE_PATTERN = "((?<!([A-Z]{1,10})-?)[A-Z]+-\\d+)";
    protected String issuePattern = DEFAULT_ISSUE_PATTERN;
    
    @DataBoundConstructor
    public FindIssuesAtCommitLogsStep() {
    }
    
    @DataBoundSetter
    public void setIssuePattern(@Nonnull String issuePattern) {
        this.issuePattern = StringUtils.isBlank(issuePattern) ? DEFAULT_ISSUE_PATTERN : issuePattern;
    }
    
    public String getIssuePattern() {
        return issuePattern;
    }
    
    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        
        public DescriptorImpl() {
            super(FindIssuesAtCommitLogsStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "jiraFindIssuesAtCommitLogs";
        }

        @Override
        public String getDisplayName() {
            return Messages.FindIssuesAtCommitLogsStep_Descriptor_DisplayName();
        }
    }
    
    public static class FindIssuesAtCommitLogsStepExecution extends AbstractSynchronousNonBlockingStepExecution<List<String>> {

        private static final long serialVersionUID = -6080152002390517795L;

        @Inject
        private transient FindIssuesAtCommitLogsStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient WorkflowRun run;

        @Override
        protected List<String> run() throws Exception {
            
            List<ChangeLogSet<? extends Entry>> changelogsets = run.getChangeSets();
            
            Set<String> issues = new HashSet<String>();
            Pattern pattern = Pattern.compile(step.getIssuePattern());
            
            for (ChangeLogSet<? extends Entry> changelogset : changelogsets) {
                Object[] entries = changelogset.getItems();
                for (int i = 0; i < entries.length; i++) {
                    Entry entry = (Entry)entries[i];
                    String msg = entry.getMsg();
                    Matcher matcher = pattern.matcher(msg);
                    while (matcher.find()) {
                        String issueKey = msg.substring(matcher.start(), matcher.end());
                        issues.add(issueKey);
                    }
                }
            }
           
            List<String> resultList = new ArrayList<String>(issues);
            return resultList;
        }

    }
}
