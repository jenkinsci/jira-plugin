package hudson.plugins.jira;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an external JIRA installation and configuration
 * needed to access this JIRA.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraSite {
    /**
     * URL of JIRA, like <tt>http://jira.codehaus.org/</tt>.
     * Mandatory. Normalized to end with '/'
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
     * List of project keys (i.e., "MNG" portion of "MNG-512"),
     * last time we checked. Copy on write semantics.
     */
    private transient volatile Set<String> projects;

    /**
     * @stapler-constructor
     */
    public JiraSite(URL url, String userName, String password, boolean supportsWikiStyleComment) {
        if(!url.toExternalForm().endsWith("/"))
            try {
                url = new URL(url.toExternalForm()+"/");
            } catch (MalformedURLException e) {
                throw new AssertionError(e); // impossible
            }
        this.url = url;
        this.userName = Util.fixEmpty(userName);
        this.password = Util.fixEmpty(password);
        this.supportsWikiStyleComment = supportsWikiStyleComment;
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
        return new JiraSession(this,service,service.login(userName,password));
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
     * Gets the list of project IDs in this JIRA.
     * This information could be bit old, or it can be null.
     */
    public Set<String> getProjectKeys() {
        if(projects==null) {
            synchronized (this) {
                try {
                    if(projects==null) {
                        // this will cause the setProjectKeys invocation.
                        JiraSession session = createSession();
                        if(session!=null)
                            session.getProjectKeys();
                    }
                } catch (IOException e) {
                    // in case of error, set empty set to avoid trying the same thing repeatedly.
                    LOGGER.log(Level.WARNING,"Failed to obtain JIRA project list",e);
                } catch (ServiceException e) {
                    LOGGER.log(Level.WARNING,"Failed to obtain JIRA project list",e);
                }
            }
        }
        // fall back to empty if failed to talk to the server
        if(projects==null)
            setProjectKeys(new HashSet<String>());
        
        return Collections.unmodifiableSet(projects);
    }

    protected void setProjectKeys(Set<String> keys) {
        if(projects!=null && projects.equals(keys))
            return; // no change

        projects = new HashSet<String>(keys);
        JiraProjectProperty.DESCRIPTOR.save();
    }

    /**
     * Gets the effective {@link JiraSite} associated with the given project.
     *
     * @return null
     *      if no such was found.
     */
    public static JiraSite get(AbstractProject<?,?> p) {
        JiraProjectProperty jpp = p.getProperty(JiraProjectProperty.class);
        if(jpp!=null) {
            JiraSite site = jpp.getSite();
            if(site!=null)
                return site;
        }

        // none is explicitly configured. try the default ---
        // if only one is configured, that must be it.
        JiraSite[] sites = JiraProjectProperty.DESCRIPTOR.getSites();
        if(sites.length==1) return sites[0];

        return null;
    }

    /**
     * Checks if the given JIRA id will be likely to exist in this issue tracker.
     *
     * <p>
     * This method checks whether the key portion is a valid key (except that
     * it can potentially use stale data). Number portion is not checked at all.
     *
     * @param id
     *      String like MNG-1234
     */
    public boolean existsIssue(String id) {
        int idx = id.indexOf('-');
        if(idx==-1) return false;

        Set<String> keys = getProjectKeys();
        return keys==null || keys.contains(id.substring(0,idx));
    }

    private static final Logger LOGGER = Logger.getLogger(JiraSite.class.getName());
}
