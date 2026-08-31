package hudson.plugins.jira.selector;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.EnvironmentExpander;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class ExplicitIssueSelector extends AbstractIssueSelector {

    /**
     * The comma-separated issue keys, exactly as typed in the configuration form.
     *
     * <p>This is the only persisted representation. The split list used to be stored alongside it and
     * was the one {@link #findIssueIds} actually read, while the form bound to this string - so a
     * selector built through either of the other two constructors rendered an empty text box, and the
     * next save silently wiped its keys.
     */
    private String issueKeys;

    /**
     * Landing zone for configurations written before the split list stopped being persisted.
     * {@link #readResolve()} folds it into {@link #issueKeys} and clears it, so it is never written
     * again. Deliberately not {@code transient}: XStream would then skip it on read as well as on
     * write, and those old configurations would lose their keys.
     *
     * @deprecated superseded by {@link #issueKeys}
     */
    @Deprecated
    private List<String> jiraIssueKeys;

    @DataBoundConstructor
    public ExplicitIssueSelector(String issueKeys) {
        this.issueKeys = issueKeys;
    }

    public ExplicitIssueSelector(List<String> jiraIssueKeys) {
        this(jiraIssueKeys == null ? "" : String.join(",", jiraIssueKeys));
    }

    public ExplicitIssueSelector() {
        this("");
    }

    public void setIssueKeys(String issueKeys) {
        this.issueKeys = issueKeys;
    }

    public String getIssueKeys() {
        return issueKeys;
    }

    /**
     * The configured keys, split and trimmed. Never null, never contains blanks.
     */
    @NonNull
    public List<String> getJiraIssueKeys() {
        if (StringUtils.isBlank(issueKeys)) {
            return Collections.emptyList();
        }
        return Arrays.stream(issueKeys.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    // TODO(4.0): remove along with jiraIssueKeys once this migration is no longer needed - see #1214
    @SuppressWarnings("deprecation")
    protected Object readResolve() {
        if (issueKeys == null && jiraIssueKeys != null) {
            issueKeys = String.join(",", jiraIssueKeys);
        }
        jiraIssueKeys = null;
        return this;
    }

    @Override
    public Set<String> findIssueIds(Run<?, ?> run, JiraSite site, TaskListener listener) {
        EnvVars envVars = EnvironmentExpander.getEnvVars(run, listener);

        Set<String> expanded = new LinkedHashSet<>();
        for (String issue : getJiraIssueKeys()) {
            expanded.add(EnvironmentExpander.expandVariable(issue, envVars));
        }

        return expanded;
    }

    @Extension
    @Symbol("ExplicitSelector")
    public static final class DescriptorImpl extends Descriptor<AbstractIssueSelector> {
        @Override
        public String getDisplayName() {
            return Messages.IssueSelector_ExplicitIssueSelector_DisplayName();
        }
    }
}
