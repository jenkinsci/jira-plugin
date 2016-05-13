package hudson.plugins.jira.selector;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.jira.Messages;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;

public class ExplicitIssueSelector extends AbstractIssueSelector {

    @CheckForNull
    private List<String> jiraIssueKeys;
    private String issueKeys;

    @DataBoundConstructor
    public ExplicitIssueSelector(String issueKeys) {
        this.jiraIssueKeys = StringUtils.isNotBlank(issueKeys) ? Lists.newArrayList(issueKeys.split(",")) : Collections.<String>emptyList();
        this.issueKeys = issueKeys;
    }

    public ExplicitIssueSelector(List<String> jiraIssueKeys) {
        this.jiraIssueKeys = jiraIssueKeys;
    }

    public ExplicitIssueSelector(){
        this.jiraIssueKeys = Collections.<String>emptyList();
    }

    public void setIssueKeys(String issueKeys){
        this.issueKeys = issueKeys;
        this.jiraIssueKeys = Lists.newArrayList(issueKeys.split(","));
    }

    public String getIssueKeys(){
        return issueKeys;
    }

    @Override
    public Set<String> findIssueIds(Run<?, ?> run, JiraSite site, TaskListener listener) {
        return Sets.newHashSet(jiraIssueKeys);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {
        @Override
        public String getDisplayName() {
            return Messages.IssueSelector_ExplicitIssueSelector_DisplayName();
        }
    }

}
