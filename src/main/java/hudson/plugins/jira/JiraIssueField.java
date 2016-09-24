package hudson.plugins.jira;

import java.lang.String;

public class JiraIssueField implements Comparable<JiraIssueField> {

    private final String field_id;
    private final Object field_value;

    public JiraIssueField(String fieldId, Object fieldValue) {
        this.field_id = fieldId;
        this.field_value = fieldValue;
    }

    public int compareTo(JiraIssueField that) {
        return this.compareTo(that);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field_id == null) ? 0 : field_id.hashCode());
        result = prime * result + ((field_value == null) ? 0 : field_value.hashCode());
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

        JiraIssueField other = (JiraIssueField) obj;
        if (field_id == null) {
            if (other.field_id != null) {
                return false;
            }
        } else if (!field_id.equals(other.field_id)) {
            return false;
        }

        if (field_value == null) {
            if (other.field_value != null) {
                return false;
            }
        } else if (!field_value.equals(other.field_value)) {
            return false;
        }

        return true;
    }

    public String getId() {
        return field_id;
    }

    public Object getValue() {
        return field_value;
    }

}
