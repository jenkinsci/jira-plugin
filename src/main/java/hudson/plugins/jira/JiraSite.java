package hudson.plugins.jira;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;
import hudson.plugins.jira.soap.RemoteIssue;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
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
    public final URL url;

    /**
     * User name needed to login. Optional.
     */
    public final String userName;

    /**
     * Password needed to login. Optional.
     */
    public final String password;

    /**
     * True if this JIRA is configured to allow Confluence-style Wiki comment.
     */
    public final boolean supportsWikiStyleComment;
    
    /**
     * to record scm changes in jira issue
     * @since 1.21
     */
    public final boolean recordScmChanges;    

    /**
     * List of project keys (i.e., "MNG" portion of "MNG-512"),
     * last time we checked. Copy on write semantics.
     */
    // TODO: seems like this is never invalidated (never set to null)
    // should we implement to invalidate this (say every hour)?
    private transient volatile Set<String> projects;

    /**
     * @stapler-constructor
     */
    public JiraSite(URL url, String userName, String password, boolean supportsWikiStyleComment, boolean recordScmChanges) {
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
        this.recordScmChanges = recordScmChanges;
    }

    public String getName() {
        return url.toExternalForm();
    }

    /**
     * Creates a remote access session to this JIRA.
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
        return new URL(url, "browse/" + id.toUpperCase());
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
                        JiraSession session = createSession();
                        if(session!=null)
                            projects = Collections.unmodifiableSet(session.getProjectKeys());
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
        if(projects==null) {
            return Collections.emptySet();
        }
        
        return projects;
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
        return keys.contains(id.substring(0,idx).toUpperCase());
    }
    
    /**
     * Returns the remote issue with the given id or <code>null</code> if it wasn't found.
     */
    public JiraIssue getIssue(String id) throws IOException, ServiceException {
        JiraSession session = createSession();
        if (session != null) {
            RemoteIssue remoteIssue = session.getIssue(id);
            if (remoteIssue != null) {
                return new JiraIssue(remoteIssue);
            }
        }
        return null;
    }

    private static final Logger LOGGER = Logger.getLogger(JiraSite.class.getName());
}
