package hudson.plugins.jira;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Item;
import jenkins.model.Jenkins;

/**
 * Permission gate shared by the Stapler web methods ({@code doCheck*} / {@code doFill*}) of this
 * plugin's descriptors.
 *
 * <p>Stapler routes these methods independently of the configuration form their field appears on,
 * so each one decides for itself whether the caller is entitled to the answer rather than assuming
 * it was reached from that form.
 */
final class DescriptorPermissions {

    private DescriptorPermissions() {}

    /**
     * Whether the caller may see configuration-scoped data.
     *
     * @param item the item the web method was invoked against, or {@code null} when it was invoked
     *     from global configuration
     * @return {@code true} if the caller can configure {@code item}, or administer Jenkins when
     *     there is no item in the request path
     */
    static boolean canSeeConfiguration(@CheckForNull Item item) {
        if (item == null) {
            return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
        }
        return item.hasPermission(Item.CONFIGURE);
    }
}
