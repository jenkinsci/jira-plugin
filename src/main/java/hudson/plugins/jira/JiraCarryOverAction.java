package hudson.plugins.jira;

import hudson.Util;
import hudson.model.InvisibleAction;
import hudson.plugins.jira.model.JiraIssue;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Remembers JIRA IDs that need to be updated later,
 * when we get a successful build.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class JiraCarryOverAction extends InvisibleAction {
    /**
     * ','-separate IDs, for compact persistence.
     */
    private final String ids;

    public JiraCarryOverAction(Set<JiraIssue> issues) {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (JiraIssue issue : issues) {
            if (first) {
                first = false;
            } else {
                buf.append(",");
            }
            buf.append(issue.getKey());
        }
        this.ids = buf.toString();
    }

    @Exported
    public Collection<String> getIDs() {
        return Arrays.asList(Util.tokenize(ids, ","));
    }
}
