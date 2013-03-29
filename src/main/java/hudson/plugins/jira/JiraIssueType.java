package hudson.plugins.jira;

import hudson.plugins.jira.soap.RemoteIssueType;

/**
 * One JIRA issue.
 *
 * <p>
 * This class is used to represent Jira Issue Types
 * so that Jenkins can display it without talking to JIRA.
 *
 * @author jdewinne
 */
public final class JiraIssueType implements Comparable<JiraIssueType> {
    /**
     * JIRA Issue type id, like "6 = Incident".
     */
    public final String id;

    /**
     * Title of the issue.
     * For example, in case of MNG-1235, this is "NPE In DiagnosisUtils while using tomcat plugin"
     */
    public final String name;

    public JiraIssueType(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public JiraIssueType(RemoteIssueType issueType) {
        this(issueType.getId(),issueType.getName());
    }

    public int compareTo(JiraIssueType that) {
        return this.name.compareTo(that.name);
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
        JiraIssueType other = (JiraIssueType) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
