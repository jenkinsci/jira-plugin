package hudson.plugins.jira;

import hudson.Util;
import hudson.model.InvisibleAction;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;

/**
 * Remembers JIRA IDs that need to be updated later,
 * when we get a successful build.
 * 
 * @author Kohsuke Kawaguchi
 */
public class JiraCarryOverAction extends InvisibleAction {
    /**
     * ','-separate IDs, for compact persistence.
     */
    private final String ids;

    public JiraCarryOverAction(List<JiraIssue> issues) {
        StringBuilder buf = new StringBuilder();
        boolean first=true;
        for (JiraIssue issue : issues) {
            if(first)   first=false;
            else        buf.append(",");
            buf.append(issue.id);
        }
        this.ids = buf.toString();
    }

    public Collection<String> getIDs() {
        return Arrays.asList(Util.tokenize(ids,","));
    }
}
