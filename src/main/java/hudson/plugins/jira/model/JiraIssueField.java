package hudson.plugins.jira.model;

import java.lang.String;

public class JiraIssueField implements Comparable<JiraIssueField> {

    private final String fieldId;
    private final Object fieldValue;

    public JiraIssueField(String fieldId, Object fieldValue) {
        this.fieldId = fieldId;
        this.fieldValue = fieldValue;
    }

    public int compareTo(JiraIssueField that) {
        return this.compareTo(that);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldId == null) ? 0 : fieldId.hashCode());
        result = prime * result + ((fieldValue == null) ? 0 : fieldValue.hashCode());
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
        if (fieldId == null) {
            if (other.fieldId != null) {
                return false;
            }
        } else if (!fieldId.equals(other.fieldId)) {
            return false;
        }

        if (fieldValue == null) {
            if (other.fieldValue != null) {
                return false;
            }
        } else if (!fieldValue.equals(other.fieldValue)) {
            return false;
        }

        return true;
    }

    public String getId() {
        return fieldId;
    }

    public Object getValue() {
        return fieldValue;
    }

}
