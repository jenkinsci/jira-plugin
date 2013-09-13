package hudson.plugins.jira;

import hudson.plugins.jira.soap.RemoteIssue;

/**
 * One JIRA issue.
 * This class is used to persist crucial issue information
 * so that Jenkins can display it without talking to JIRA.
 *
 * @author Kohsuke Kawaguchi
 * @see JiraSite#getUrl(JiraIssue)
 */
public final class JiraIssue implements Comparable<JiraIssue> {
    /**
     * JIRA ID, like "MNG-1235".
     */
    public final String id;

    /**
     * Title of the issue.
     * For example, in case of MNG-1235, this is "NPE In DiagnosisUtils while using tomcat plugin"
     */
    public final String title;

    public JiraIssue(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public JiraIssue(RemoteIssue issue) {
        this(issue.getKey(), issue.getSummary());
    }

    public int compareTo(JiraIssue that) {
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JiraIssue other = (JiraIssue) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
