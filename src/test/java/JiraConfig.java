import java.util.ResourceBundle;

public final class JiraConfig {

    private static final ResourceBundle CONFIG = ResourceBundle
            .getBundle("jira");

    public static String getUrl() {
        return CONFIG.getString("url");
    }

    public static String getUsername() {
        return CONFIG.getString("username");
    }

    public static String getPassword() {
        return CONFIG.getString("password");
    }
}
