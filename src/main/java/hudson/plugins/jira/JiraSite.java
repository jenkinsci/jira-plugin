package hudson.plugins.jira;

import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.Util;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

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
        this.userName = Util.fixEmpty(userName);
        this.password = Util.fixEmpty(password);
    }

    public JiraSite() {}
    
    public String getName() {
        return url.toExternalForm();
    }

    /**
     * Creates a remote access session tos his JIRA.
     *
     * @return
     *      null if remote access is not supported.
     */
    public JiraSession createSession() throws IOException, ServiceException {
        if(userName==null || password==null)
            return null;    // remote access not supported
        JiraSoapServiceService jiraSoapServiceGetter = new JiraSoapServiceServiceLocator();

        JiraSoapService service = jiraSoapServiceGetter.getJirasoapserviceV2(
            new URL(url, "rpc/soap/jirasoapservice-v2"));
        return new JiraSession(service,service.login(userName,password));
    }

    /**
     * Computes the URL to the given issue.
     */
    public URL getUrl(JiraIssue issue) throws IOException {
        return getUrl(issue.id);
    }

    /**
     * Computes the URL to the given issue.
     */
    public URL getUrl(String id) throws MalformedURLException {
        return new URL(url,"browse/"+ id);
    }


    /**
     * Gets the effective {@link JiraSite} associated with the given project.
     *
     * @return null
     *      if no such was found.
     */
    public static JiraSite get(AbstractProject<?,?> p) {
        JiraProjectProperty jpp = p.getProperty(JiraProjectProperty.class);
        if(jpp!=null)
            return jpp.getSite();

        // none is explicitly configured. try the default ---
        // if only one is configured, that must be it.
        JiraSite[] sites = JiraProjectProperty.DESCRIPTOR.getSites();
        if(sites.length==1) return sites[0];

        return null;

    }
}
