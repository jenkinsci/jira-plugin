package hudson.plugins.jira.model;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.plugins.jira.JiraSite;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;

/**
 * One JIRA issue.
 * This class is used to persist crucial issue information
 * so that Jenkins can display it without talking to JIRA.
 *
 * @author Kohsuke Kawaguchi
 * @see JiraSite#getUrl(JiraIssue)
 */
@ExportedBean
public final class JiraIssue implements Comparable<JiraIssue> {

    private final String id;
    private final String title;

    public JiraIssue(String id, String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * @return JIRA ID, like "MNG-1235".
     */
    @Exported
    public String getId() {
        return id;
    }

    /**
     * @return Title of the issue. For example, in case of MNG-1235, this is "NPE In DiagnosisUtils while using tomcat plugin"
     */
    @Exported
    public String getTitle() {
        return title;
    }

    public JiraIssue(Issue issue) {
        this(issue.getKey(), issue.getSummary());
    }

    public int compareTo(@Nonnull JiraIssue that) {
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
