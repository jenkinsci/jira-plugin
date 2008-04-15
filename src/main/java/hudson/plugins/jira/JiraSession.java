package hudson.plugins.jira;

import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.RemoteComment;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.plugins.jira.soap.RemoteProject;

import java.rmi.RemoteException;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Connection to JIRA.
 *
 * <p>
 * JIRA has a built-in timeout for a session, so after some inactive period
 * the session will become invalid. The caller must make sure that this
 * doesn't happen.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JiraSession {
    public final JiraSoapService service;

    /**
     * This security token is used by the server to associate SOAP invocations
     * with a specific user.
     */
    public final String token;

    /**
     * Lazily computed list of project keys.
     */
    private Set<String> projectKeys;

    /**
     * This session is created for this site.
     */
    private final JiraSite site;

    /*package*/ JiraSession(JiraSite site, JiraSoapService service, String token) {
        this.service = service;
        this.token = token;
        this.site = site;
    }

    /**
     * Returns the set of project keys (like MNG, HUDSON, etc) that are available
     * in this JIRA.
     */
    public Set<String> getProjectKeys() throws RemoteException {
        if(projectKeys==null) {
            LOGGER.fine("Fetching remote project key list from "+site.getName());
            projectKeys = new HashSet<String>();
            for(RemoteProject p : service.getProjects(token))
                projectKeys.add(p.getKey());
            site.setProjectKeys(projectKeys);
            LOGGER.fine("Project list="+projectKeys);
        }
        return projectKeys;
    }

    /**
     * Adds a comment to the existing issue.
     */
    public void addComment(String issueId, String comment) throws RemoteException {
        RemoteComment rc = new RemoteComment();
        rc.setBody(comment);
        service.addComment(token, issueId, rc);
    }

    /**
     * Gets the details of one issue.
     *
     * @param id
     *      Issue ID like "MNG-1235".
     * @return
     *      null if no such issue exists.
     */
    public RemoteIssue getIssue(String id) throws RemoteException {
        if(existsIssue(id))
            return service.getIssue(token,id);
        else
            return null;
    }

    public boolean existsIssue(String id) throws RemoteException {
        int idx = id.indexOf('-');
        return idx >= 0 && getProjectKeys().contains(id.substring(0, idx));
    }

    private static final Logger LOGGER = Logger.getLogger(JiraSession.class.getName());
}
