package hudson.plugins.jira;

import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.URL;

/**
 * Represents an external JIRA installation and configuration
 * needed to access this JIRA.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraSite {
    /**
     * URL of JIRA, like <tt>http://jira.codehaus.org/</tt>.
     * Mandatory.
     */
    public URL url;

    /**
     * User name needed to login. Optional.
     */
    public String userName;

    /**
     * Password needed to login. Optional.
     */
    public String password;

    /**
     * True if this JIRA is configured to allow Confluence-style Wiki comment.
     */
    public boolean supportsWikiStyleComment;

    /**
     * @stapler-constructor
     */
    public JiraSite(URL url, String userName, String password) {
        this.url = url;
        this.userName = userName;
        this.password = password;
    }

    public JiraSite() {}
    
    public String getName() {
        return url.toExternalForm();
    }

    public JiraSession createSession() throws IOException, ServiceException {
        JiraSoapServiceService jiraSoapServiceGetter = new JiraSoapServiceServiceLocator();

        JiraSoapService service = jiraSoapServiceGetter.getJirasoapserviceV2(
            new URL(url, "rpc/soap/jirasoapservice-v2"));
        return new JiraSession(service,service.login(userName,password));
    }
}
