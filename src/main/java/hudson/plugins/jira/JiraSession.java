package hudson.plugins.jira;

import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.RemoteComment;
import hudson.plugins.jira.soap.RemoteGroup;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.plugins.jira.soap.RemoteProject;
import hudson.plugins.jira.soap.RemoteValidationException;

import java.rmi.RemoteException;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Connection to JIRA.
 * 
 * <p>
 * JIRA has a built-in timeout for a session, so after some inactive period the
 * session will become invalid. The caller must make sure that this doesn't
 * happen.
 * 
 * @author Kohsuke Kawaguchi
 */
public class JiraSession {
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

	/* package */JiraSession(JiraSite site, JiraSoapService service,
			String token) {
		this.service = service;
		this.token = token;
		this.site = site;
	}

	/**
	 * Returns the set of project keys (like MNG, HUDSON, etc) that are
	 * available in this JIRA.
	 * 
	 * Guarantees to return all project keys in upper case.
	 */
	public Set<String> getProjectKeys() throws RemoteException {
		if (projectKeys == null) {
			LOGGER.fine("Fetching remote project key list from "
					+ site.getName());
			RemoteProject[] remoteProjects = service
					.getProjectsNoSchemes(token);
			projectKeys = new HashSet<String>(remoteProjects.length);
			for (RemoteProject p : remoteProjects) {
				projectKeys.add(p.getKey().toUpperCase());
			}
			LOGGER.fine("Project list=" + projectKeys);
		}
		return projectKeys;
	}

	/**
	 * Adds a comment to the existing issue. Constrains the visibility of the
	 * comment the the supplied groupVisibility.
	 * 
	 * @param groupVisibility
	 */
	public void addComment(String issueId, String comment,
			String groupVisibility) throws RemoteException {
		RemoteComment rc = new RemoteComment();
		rc.setBody(comment);

		try {
			if (groupVisibility != null && groupVisibility != ""
					&& getGroup(groupVisibility) != null) {
				rc.setGroupLevel(groupVisibility);
			}
		} catch (RemoteValidationException rve) {
			LOGGER.throwing(this.getClass().toString(), "addComment", rve);
		}
		service.addComment(token, issueId, rc);
	}

	/**
	 * Gets the details of one issue.
	 * 
	 * @param id
	 *            Issue ID like "MNG-1235".
	 * @return null if no such issue exists.
	 */
	public RemoteIssue getIssue(String id) throws RemoteException {
		if (existsIssue(id))
			return service.getIssue(token, id);
		else
			return null;
	}

	/**
	 * Gets the details of a group, given a groupId. Used for validating group
	 * visibility.
	 * 
	 * @param Group
	 *            ID like "Software Development"
	 * @return null if no such group exists
	 */
	public RemoteGroup getGroup(String groupId) throws RemoteException {
		LOGGER.fine("Fetching groupInfo from " + groupId);
		return service.getGroup(token, groupId);
	}

	public boolean existsIssue(String id) throws RemoteException {
		return site.existsIssue(id);
	}

	private static final Logger LOGGER = Logger.getLogger(JiraSession.class
			.getName());
}
