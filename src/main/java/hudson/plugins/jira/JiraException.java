package hudson.plugins.jira;

public class JiraException extends Exception {
    public JiraException(String message, Throwable cause) {
        super(message, cause);
    }

    public JiraException(String message) {
        super(message);
    }
}
