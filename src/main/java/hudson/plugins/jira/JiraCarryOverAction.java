package hudson.plugins.jira;

import hudson.Util;
import hudson.model.InvisibleAction;

import java.util.Collection;
import java.util.Arrays;

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

    public JiraCarryOverAction(Collection<String> ids) {
        this.ids = Util.join(ids,",");
    }

    public Collection<String> getIDs() {
        return Arrays.asList(Util.tokenize(ids,","));
    }
}
