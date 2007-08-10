package hudson.plugins.jira;

import hudson.plugins.jira.soap.RemoteIssue;

/**
 * One JIRA issue.
 *
 * <p>
 * This class is used to persist crucial issue information
 * so that Hudson can display it without talking to JIRA.
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
        this(issue.getKey(),issue.getSummary());
    }

    public int compareTo(JiraIssue that) {
        return this.id.compareTo(that.id);
    }
}
