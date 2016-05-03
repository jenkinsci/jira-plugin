package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.*;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Connection to JIRA.
 * JIRA has a built-in timeout for a session, so after some inactive period the
 * session will become invalid. The caller must make sure that this doesn't
 * happen.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraSession {
    private static final Logger LOGGER = Logger.getLogger(JiraSession.class.getName());

    public final JiraRestService service;

    /**
     * Lazily computed list of project keys.
     */
    private Set<String> projectKeys;

    /**
     * This session is created for this site.
     */
    private final JiraSite site;

    /* package */JiraSession(JiraSite site, JiraRestService jiraRestService) {
        this.site = site;
        this.service = jiraRestService;
    }

    /**
     * Returns the set of project keys (like MNG, JENKINS, etc) that are
     * available in this JIRA.
     * Guarantees to return all project keys in upper case.
     */
    public Set<String> getProjectKeys() {
        if (projectKeys == null) {
            LOGGER.fine("Fetching remote project key list from " + site.getName());
            final List<String> keys = service.getProjectsKeys();
            projectKeys = new HashSet<String>(keys.size());
            projectKeys.addAll(keys);
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
                           String groupVisibility, String roleVisibility) {
        service.addComment(issueId, comment, groupVisibility, roleVisibility);
    }

    /**
     * Adds new labels to the existing issue.
     * Old labels remains untouched.
     *
     * @param issueId Jira issue ID like "MNG-1235".
     * @param labels New labels to add.
     */
    public void addLabels(String issueId, List<String> labels) {
        List<String> newLabels = Lists.newArrayList();
        Issue existingIssue = service.getIssue(issueId);
        if(existingIssue.getLabels() != null) {
            newLabels.addAll(existingIssue.getLabels());
        }
        boolean changed = false;
        for(String label : labels) {
            if(!newLabels.contains(label)) {
                newLabels.add(label);
                changed = true;
            }
        }
        if(changed) {
            service.setIssueLabels(issueId, newLabels);
        }
    }

    /**
     * Gets the details of one issue.
     *
     * @param id Issue ID like "MNG-1235".
     * @return null if no such issue exists.
     */
    public Issue getIssue(String id) {
        if (existsIssue(id)) {
            return service.getIssue(id);
        } else {
            return null;
        }
    }

    /**
     * Gets all issues that match the given JQL filter
     *
     * @param jqlSearch JQL query string to execute
     * @return issues matching the JQL query
     */
    public List<Issue> getIssuesFromJqlSearch(final String jqlSearch) {
        return service.getIssuesFromJqlSearch(jqlSearch, 50);
    }

    /**
     * Get all versions from the given project
     *
     * @param projectKey The key for the project
     * @return An array of versions
     */
    public List<Version> getVersions(String projectKey) {
        LOGGER.fine("Fetching versions from project: " + projectKey);

        return service.getVersions(projectKey);
    }

    /**
     * Get a version by its name
     *
     * @param projectKey The key for the project
     * @param name       The version name
     * @return A RemoteVersion, or null if not found
     */
    public Version getVersionByName(String projectKey, String name) {
        LOGGER.fine("Fetching versions from project: " + projectKey);
        List<Version> versions = getVersions(projectKey);
        if (versions == null) {
            return null;
        }
        for (Version version : versions) {
            if (version.getName().equals(name)) {
                return version;
            }
        }
        return null;
    }

    public List<Issue> getIssuesWithFixVersion(String projectKey, String version) {
        return getIssuesWithFixVersion(projectKey, version, "");
    }

    public List<Issue> getIssuesWithFixVersion(String projectKey, String version, String filter) {
        LOGGER.fine("Fetching versions from project: " + projectKey + " with fixVersion:" + version);
        if (isNotEmpty(filter)) {
            return service.getIssuesFromJqlSearch(String.format("project = \"%s\" and fixVersion = \"%s\" and " + filter, projectKey, version), Integer.MAX_VALUE);
        }
        return service.getIssuesFromJqlSearch(String.format("project = \"%s\" and fixVersion = \"%s\"", projectKey, version), Integer.MAX_VALUE);
    }

    /**
     * Get all issue types
     *
     * @return An array of issue types
     */
    public List<IssueType> getIssueTypes() {
        LOGGER.fine("Fetching issue types");
        return service.getIssueTypes();
    }

    @Deprecated
    public boolean existsIssue(String id) {
        return site.existsIssue(id);
    }


    public void releaseVersion(String projectKey, Version version) {
        LOGGER.fine("Releasing version: " + version.getName());
        service.releaseVersion(projectKey, version);
    }

    /**
     * Replaces the fix version list of all issues matching the JQL Query with the version specified.
     *
     * @param projectKey The JIRA Project key
     * @param version    The replacement version
     * @param query      The JQL Query
     */
    public void migrateIssuesToFixVersion(String projectKey, String version, String query) {

        Version newVersion = getVersionByName(projectKey, version);
        if (newVersion == null) {
            LOGGER.warning("Version " + version + " was not found");
            return;
        }

        LOGGER.fine("Fetching versions with JQL:" + query);
        List<Issue> issues = service.getIssuesFromJqlSearch(query, Integer.MAX_VALUE);
        if (issues == null || issues.isEmpty()) {
            return;
        }
        LOGGER.fine("Found issues: " + issues.size());

        for (Issue issue : issues) {
            LOGGER.fine("Migrating issue: " + issue.getKey());
            service.updateIssue(issue.getKey(), Lists.newArrayList(newVersion));
        }
    }

    /**
     * Replaces the given fromVersion with toVersion in all issues matching the JQL query.
     *
     * @param projectKey  The JIRA Project
     * @param fromVersion The name of the version to replace
     * @param toVersion   The name of the replacement version
     * @param query       The JQL Query
     */
    public void replaceFixVersion(String projectKey, String fromVersion, String toVersion, String query) {

        Version newVersion = getVersionByName(projectKey, toVersion);
        if (newVersion == null) {
            LOGGER.warning("Version " + toVersion + " was not found");
            return;
        }

        LOGGER.fine("Fetching versions with JQL:" + query);
        List<Issue> issues = service.getIssuesFromJqlSearch(query, Integer.MAX_VALUE);
        if (issues == null) {
            return;
        }
        LOGGER.fine("Found issues: " + issues.size());

        for (Issue issue : issues) {
            Set<Version> newVersions = new HashSet<Version>();
            newVersions.add(newVersion);
            for (Version currentVersion : issue.getFixVersions()) {
                if (!currentVersion.getName().equals(fromVersion)) {
                    newVersions.add(currentVersion);
                }
            }

            LOGGER.fine("Replaceing version in issue: " + issue.getKey());
            service.updateIssue(issue.getKey(), Lists.newArrayList(newVersions));
        }
    }

    /**
     * Adds the specified version to the fix version list of all issues matching the JQL.
     *
     * @param projectKey The JIRA Project
     * @param version    The version to add
     * @param query      The JQL Query
     */
    public void addFixVersion(String projectKey, String version, String query) {

        Version newVersion = getVersionByName(projectKey, version);
        if (newVersion == null) {
            LOGGER.warning("Version " + version + " was not found");
            return;
        }

        LOGGER.fine("Fetching issues with JQL:" + query);
        List<Issue> issues = service.getIssuesFromJqlSearch(query, Integer.MAX_VALUE);
        if (issues == null || issues.isEmpty()) {
            return;
        }
        LOGGER.fine("Found issues: " + issues.size());

        for (Issue issue : issues) {
            LOGGER.fine("Adding version: " + newVersion.getName() + " to issue: " + issue.getKey());
            List<Version> fixVersions = Lists.newArrayList(issue.getFixVersions());
            fixVersions.add(newVersion);
            service.updateIssue(issue.getKey(), fixVersions);
        }
    }

    /**
     * Progresses the issue's workflow by performing the specified action. The issue's new status is returned.
     *
     * @param issueKey
     * @param actionId
     * @return The new status
     */
    public String progressWorkflowAction(String issueKey, Integer actionId) {
        LOGGER.fine("Progressing issue " + issueKey + " with workflow action: " + actionId);
        final Issue issue = service.progressWorkflowAction(issueKey, actionId);
        getStatusById(issue.getStatus().getId());
        return getStatusById(issue.getStatus().getId());
    }

    /**
     * Returns the matching action id for a given action name.
     *
     * @param issueKey
     * @param workflowAction
     * @return The action id, or null if the action cannot be found.
     */
    public Integer getActionIdForIssue(String issueKey, String workflowAction) {
        List<Transition> actions = service.getAvailableActions(issueKey);

        if (actions != null) {
            for (Transition action : actions) {
                if (action.getName() != null && action.getName().equalsIgnoreCase(workflowAction)) {
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
     */
    public String getStatusById(Long statusId) {
        String status = getKnownStatuses().get(statusId);

        if (status == null) {
            LOGGER.warning("JIRA status could not be found: " + statusId + ". Checking JIRA for new status types.");
            knownStatuses = null;
            // Try again, just in case the admin has recently added a new status. This should be a rare condition.
            status = getKnownStatuses().get(statusId);
        }

        return status;
    }

    private HashMap<Long, String> knownStatuses = null;

    /**
     * Returns all known statuses.
     *
     * @return
     */
    private HashMap<Long, String> getKnownStatuses() {
        if (knownStatuses == null) {
            List<Status> statuses = service.getStatuses();
            knownStatuses = new HashMap<Long, String>(statuses.size());
            for (Status status : statuses) {
                knownStatuses.put(status.getId(), status.getName());
            }
        }
        return knownStatuses;
    }

    /**
     * Returns issue-id of the created issue
     *
     * @param projectKey
     * @param description
     * @param assignee
     * @param components
     * @param summary
     * @return The issue id
     */
    public Issue createIssue(String projectKey, String description, String assignee, Iterable<String> components, String summary) {
        final BasicIssue basicIssue = service.createIssue(projectKey, description, assignee, components, summary);
        return service.getIssue(basicIssue.getKey());
    }

    /**
     * Adds a comment to the existing issue.There is no constrains to the visibility of the comment.
     *
     * @param issueId
     * @param comment
     */
    public void addCommentWithoutConstrains(String issueId, String comment) {
        service.addComment(issueId, comment, null, null);
    }

    /**
     * Returns information about the specific issue as identified by the issue id
     *
     * @param issueId
     * @return issue object
     */
    public Issue getIssueByKey(String issueId) {
        return service.getIssue(issueId);
    }

    /**
     * Returns all the components for the particular project
     *
     * @param projectKey
     * @return An array of componets
     */
    public List<Component> getComponents(String projectKey) {
        return service.getComponents(projectKey);
    }

    /**
     * Creates a new version and returns it
     *
     * @param version    version id to create
     * @param projectKey
     * @return
     *
     */
    public Version addVersion(String version, String projectKey) {
        return service.addVersion(projectKey, version);
    }

    /**
     * Get User's permissions
     */
    public Permissions getMyPermissions(){ return service.getMyPermissions(); }

}