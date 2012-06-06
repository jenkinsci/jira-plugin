package hudson.plugins.jira;

import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.RemoteComment;
import hudson.plugins.jira.soap.RemoteFieldValue;
import hudson.plugins.jira.soap.RemoteGroup;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.plugins.jira.soap.RemoteIssueType;
import hudson.plugins.jira.soap.RemoteNamedObject;
import hudson.plugins.jira.soap.RemoteProject;
import hudson.plugins.jira.soap.RemoteProjectRole;
import hudson.plugins.jira.soap.RemoteStatus;
import hudson.plugins.jira.soap.RemoteValidationException;
import hudson.plugins.jira.soap.RemoteVersion;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
	 * Returns the set of project keys (like MNG, JENKINS, etc) that are
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
			String groupVisibility, String roleVisibility) throws RemoteException {
		RemoteComment rc = new RemoteComment();
		rc.setBody(comment);

		try {
			if (roleVisibility != null && roleVisibility.equals("") == false
					&& getRole(roleVisibility) != null) {
				rc.setRoleLevel(roleVisibility);
			}
		} catch (RemoteValidationException rve) {
			LOGGER.throwing(this.getClass().toString(), "setRoleLevel", rve);
		}
		
		try {
			if (groupVisibility != null && groupVisibility.equals("") == false
					&& getGroup(groupVisibility) != null) {
				rc.setGroupLevel(groupVisibility);
			}
		} catch (RemoteValidationException rve) {
			LOGGER.throwing(this.getClass().toString(), "setGroupLevel", rve);
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
	 * Gets all issues that match the given JQL filter
	 * 
	 * @param jqlSearch
	 *            JQL query string to execute
	 * @return issues matching the JQL query
	 * @throws RemoteException
	 */
	public RemoteIssue[] getIssuesFromJqlSearch(final String jqlSearch)
			throws RemoteException {
		return service.getIssuesFromJqlSearch(token, jqlSearch, 50);
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
	
	/**
	 * Gets the details of a role, given a roleId. Used for validating role
	 * visibility.
	 * 
	 * TODO: Cannot validate against the real project role the user have in the project, 
	 * jira soap api has no such function!
	 * 
	 * @param Role
	 *            ID like "Software Development"
	 * @return null if no such role exists
	 */
	public RemoteProjectRole getRole(String roleId) throws RemoteException {
		LOGGER.fine("Fetching roleInfo from " + roleId);
		
		RemoteProjectRole[] roles= service.getProjectRoles(token);
		
		if(roles != null && roles.length > 0) {
			for(RemoteProjectRole role : roles) {
				if(role != null && role.getName() != null && role.getName().equals(roleId)) {
					return role;
				}
			}
		}
		
		LOGGER.info("Did not find role named " + roleId + ".");

		return null;
	}
	
	/**
	 * Get all versions from the given project
	 * 
	 * @param projectKey The key for the project
	 * @return An array of versions
	 * @throws RemoteException
	 */
	public RemoteVersion[] getVersions(String projectKey) throws RemoteException {
		LOGGER.fine("Fetching versions from project: " + projectKey);
		
		return service.getVersions(token, projectKey);
	}
	
	/**
	 * Get a version by its name
	 * 
	 * @param projectKey The key for the project
	 * @param name The version name
	 * @return A RemoteVersion, or null if not found
	 * @throws RemoteException
	 */
	public RemoteVersion getVersionByName(String projectKey, String name) throws RemoteException {
		LOGGER.fine("Fetching versions from project: " + projectKey);
		RemoteVersion[] versions = getVersions(projectKey);
		if(versions == null) return null;
		for( RemoteVersion version : versions ) {
			if( version.getName().equals(name) ) {
				return version;
			}
		}
		return null;
	}
	
	public RemoteIssue[] getIssuesWithFixVersion(String projectKey, String version) throws RemoteException {
		return getIssuesWithFixVersion(projectKey, version, "");
	}
	
	public RemoteIssue[] getIssuesWithFixVersion(String projectKey, String version, String filter) throws RemoteException {
		LOGGER.fine("Fetching versions from project: " + projectKey + " with fixVersion:" + version);
		if( filter != null && !filter.isEmpty())
			return service.getIssuesFromJqlSearch(token, String.format("project = \"%s\" and fixVersion = \"%s\" and " + filter,projectKey,version) , Integer.MAX_VALUE);
		return service.getIssuesFromJqlSearch(token, String.format("project = \"%s\" and fixVersion = \"%s\"",projectKey,version) , Integer.MAX_VALUE);
	}
	
	/**
	 * Get all issue types
	 * 
	 * @return An array of issue types
	 * @throws RemoteException
	 */
	public RemoteIssueType[] getIssueTypes() throws RemoteException {
		LOGGER.fine("Fetching issue types");
		
		return service.getIssueTypes(token);
	}

	public boolean existsIssue(String id) throws RemoteException {
		return site.existsIssue(id);
	}

	private static final Logger LOGGER = Logger.getLogger(JiraSession.class
			.getName());

	public void releaseVersion(String projectKey, RemoteVersion version) throws RemoteException  {
		LOGGER.fine("Releaseing version: " + version.getName());
		
		service.releaseVersion(token, projectKey, version);
	}
	
	/**
	 * Replaces the fix version list of all issues matching the JQL Query with the version specified.
	 * 
	 * @param projectKey The JIRA Project key
	 * @param version The replacement version
	 * @param query The JQL Query
	 * @throws RemoteException
	 */
	public void migrateIssuesToFixVersion(String projectKey, String version, String query) throws RemoteException {
		
		RemoteVersion newVersion = getVersionByName(projectKey,version);
		if(newVersion == null ) return;
		
		LOGGER.fine("Fetching versions with JQL:" + query);
		RemoteIssue[] issues = service.getIssuesFromJqlSearch(token,query,Integer.MAX_VALUE);
		if( issues == null ) return;
		LOGGER.fine("Found issues: " + issues.length);
		
		RemoteFieldValue value = new RemoteFieldValue("fixVersions", new String[] { newVersion.getId() } );
		for( RemoteIssue issue : issues ) {
			LOGGER.fine("Migrating issue: " + issue.getKey());
			service.updateIssue(token, issue.getKey(), new RemoteFieldValue[] { value });
		}
	}
	
	/**
	 * Replaces the given fromVersion with toVersion in all issues matching the JQL query.
	 * 
	 * @param projectKey The JIRA Project
	 * @param fromVersion The name of the version to replace
	 * @param toVersion The name of the replacement version
	 * @param query The JQL Query
	 * @throws RemoteException
	 */
	public void replaceFixVersion(String projectKey, String fromVersion, String toVersion, String query) throws RemoteException {
		
		RemoteVersion newVersion = getVersionByName(projectKey,toVersion);
		if(newVersion == null ) return;
		
		LOGGER.fine("Fetching versions with JQL:" + query);
		RemoteIssue[] issues = service.getIssuesFromJqlSearch(token,query,Integer.MAX_VALUE);
		if( issues == null ) return;
		LOGGER.fine("Found issues: " + issues.length);
		
		for( RemoteIssue issue : issues ) {
			Set<String> newVersions = new HashSet<String>();
			newVersions.add(newVersion.getId());
			for( RemoteVersion currentVersion : issue.getFixVersions() ) {
				if( !currentVersion.getName().equals(fromVersion) ) {
					newVersions.add(currentVersion.getId());
				}
			}
			
			RemoteFieldValue value = new RemoteFieldValue("fixVersions", newVersions.toArray(new String[0]) );
			
			LOGGER.fine("Replaceing version in issue: " + issue.getKey());
			service.updateIssue(token, issue.getKey(), new RemoteFieldValue[] { value });
		}
	}

    /**
     * Progresses the issue's workflow by performing the specified action. The issue's new status is returned.
     *
     * @param issueKey
     * @param workflowActionName
     * @param fields
     * @return The new status
     * @throws RemoteException
     */
    public String progressWorkflowAction(String issueKey, String workflowActionName, RemoteFieldValue[] fields)
            throws RemoteException {
        LOGGER.fine("Progressing issue " + issueKey + " with workflow action: " + workflowActionName);
        RemoteIssue issue = service.progressWorkflowAction(token, issueKey, workflowActionName, fields);
        return getStatusById(issue.getStatus());
    }

    /**
     * Returns the matching action id for a given action name.
     *
     * @param issueKey
     * @param workflowAction
     * @return The action id, or null if the action cannot be found.
     * @throws RemoteException
     */
    public String getActionIdForIssue(String issueKey, String workflowAction) throws RemoteException {
        RemoteNamedObject[] actions = service.getAvailableActions(token, issueKey);

        if (actions != null) {
	        for (RemoteNamedObject action : actions) {
	            if (workflowAction.equalsIgnoreCase(action.getName())) {
	                return action.getId();
	            }
	        }
        }

        return null;
    }

    /**
     * Returns the status name by status id.
     *
     * @param statusId
     * @return
     * @throws RemoteException
     */
    public String getStatusById(String statusId) throws RemoteException {
        String status = getKnownStatuses().get(statusId);

        if (status == null) {
            LOGGER.warning("JIRA status could not be found: " + statusId + ". Checking JIRA for new status types.");
            knownStatuses = null;
            // Try again, just in case the admin has recently added a new status. This should be a rare condition.
            status = getKnownStatuses().get(statusId);
        }

        return status;
    }

    private HashMap<String, String> knownStatuses = null;

    /**
     * Returns all known statuses.
     *
     * @return
     * @throws RemoteException
     */
    private HashMap<String,String> getKnownStatuses() throws RemoteException {
        if (knownStatuses == null) {
            RemoteStatus[] statuses = service.getStatuses(token);
            knownStatuses = new HashMap<String, String>(statuses.length);

            for (RemoteStatus status : statuses) {
                knownStatuses.put(status.getId(), status.getName());
            }
        }

        return knownStatuses;
    }
}
