package hudson.plugins.jira;

import hudson.plugins.jira.soap.RemotePriority;

/**
 * One JIRA issue.
 *
 * <p>
 * This class is used to represent Jira Prioirities
 * so that Jenkins can display it without talking to JIRA.
 *
 * @author jdewinne
 */
public final class JiraPriority implements Comparable<JiraPriority> {
    /**
     * JIRA Issue type id, like "2 = High".
     */
    public final String id;

    /**
     * Name of the priority.
     * For example, "High"
     */
    public final String name;

    public JiraPriority(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public JiraPriority(RemotePriority priority) {
        this(priority.getId(),priority.getName());
    }

    public int compareTo(JiraPriority that) {
        return this.id.compareTo(that.id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JiraPriority other = (JiraPriority) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
