package hudson.plugins.jira;

import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.RemoteComment;

import java.rmi.RemoteException;

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

    /*package*/ JiraSession(JiraSoapService service, String token) {
        this.service = service;
        this.token = token;
    }

    /**
     * Adds a comment to the existing issue.
     */
    public void addComment(String issueId, String comment) throws RemoteException {
        RemoteComment rc = new RemoteComment();
        rc.setBody(comment);
        service.addComment(token, issueId, rc);
    }
}
