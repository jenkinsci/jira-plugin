package hudson.plugins.jira.selector;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class PatternIssueSelector extends DefaultIssueSelector {

    private String issuePattern;

    @DataBoundConstructor
    public PatternIssueSelector(){
    }

    @DataBoundSetter
    public void setIssuePattern(String issuePattern) {
        this.issuePattern = issuePattern;
    }

    public String getIssuePattern() {
        return StringUtils.isBlank(issuePattern) ? JiraSite.DEFAULT_ISSUE_PATTERN.toString() : issuePattern;
    }

    private Pattern getPattern(final JiraSite site) {
        return StringUtils.isBlank(issuePattern) ? site.getIssuePattern() : Pattern.compile(issuePattern);
    }


    @Override
    public Set<String> findIssueIds(@Nonnull final Run<?, ?> run, @Nonnull final JiraSite site,
                                    @Nonnull final TaskListener listener) {
        HashSet<String> issuesIds = new HashSet<String>();
        addIssuesRecursive(run, getPattern(site), listener, issuesIds);
        return issuesIds;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {

        @Override
        public String getDisplayName() {
            return Messages.IssueSelector_PatternIssueSelector_DisplayName();
        }
    }
}
