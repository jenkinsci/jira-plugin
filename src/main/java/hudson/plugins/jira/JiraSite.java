package hudson.plugins.jira;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.plugins.jira.soap.RemoteIssueType;
import hudson.plugins.jira.soap.RemoteComponent;
import hudson.plugins.jira.soap.RemoteVersion;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Represents an external JIRA installation and configuration
 * needed to access this JIRA.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraSite extends AbstractDescribableImpl<JiraSite> {
	
    /**
     * Regexp pattern that identifies JIRA issue token.
     * If this pattern changes help pages (help-issue-pattern_xy.html) must be updated 
     * <p>
     * First char must be a letter, then at least one letter, digit or underscore.
     * See issue JENKINS-729, JENKINS-4092
     */
    protected static final Pattern DEFAULT_ISSUE_PATTERN = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*)([^.]|\\.[^0-9]|\\.$|$)");

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
     * Group visibility to constrain the visibility of the added comment. Optional.
     */
    public final String groupVisibility;
    
    /**
     * Role visibility to constrain the visibility of the added comment. Optional.
     */
    public final String roleVisibility;
    
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
     * user defined pattern
     * @since 1.22
     */    
    private final String userPattern;
    
    private transient Pattern userPat;
    
    /**
     * updated jira issue for all status
     * @since 1.22
     */
    public final boolean updateJiraIssueForAllStatus;
    

    /**
     * List of project keys (i.e., "MNG" portion of "MNG-512"),
     * last time we checked. Copy on write semantics.
     */
    // TODO: seems like this is never invalidated (never set to null)
    // should we implement to invalidate this (say every hour)?
    private transient volatile Set<String> projects;

    /**
     * Used to guard the computation of {@link #projects}
     */
    private transient Lock projectUpdateLock = new ReentrantLock();

    @DataBoundConstructor
    public JiraSite(URL url, String userName, String password, boolean supportsWikiStyleComment, boolean recordScmChanges, String userPattern, 
                    boolean updateJiraIssueForAllStatus, String groupVisibility, String roleVisibility) {
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
        this.userPattern = Util.fixEmpty(userPattern);
        if (this.userPattern != null) {
        	this.userPat = Pattern.compile(this.userPattern);
        } else {
        	this.userPat = null;
        }
         
        this.updateJiraIssueForAllStatus = updateJiraIssueForAllStatus;
        this.groupVisibility = Util.fixEmpty(groupVisibility);
        this.roleVisibility = Util.fixEmpty(roleVisibility);
    }

    protected Object readResolve() {
        projectUpdateLock = new ReentrantLock();
        return this;
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
     * Gets the user-defined issue pattern if any.
     * 
     * @return the pattern or null
     */
    public Pattern getUserPattern() {
    	if (userPattern == null) {
    		return null;
    	}
    	
    	if (userPat == null) {
    		// We don't care about any thread race- or visibility issues here.
    		// The worst thing which could happen, is that the pattern
    		// is compiled multiple times.
    		Pattern p = Pattern.compile(userPattern);
    		userPat = p;
    	}
    	return userPat;
    }
    
    public Pattern getIssuePattern() {
    	if (getUserPattern() != null) {
    		return getUserPattern();
    	}
    	
    	return DEFAULT_ISSUE_PATTERN;
    }

    /**
     * Gets the list of project IDs in this JIRA.
     * This information could be bit old, or it can be null.
     */
    public Set<String> getProjectKeys() {
        if(projects==null) {
            try {
                if (projectUpdateLock.tryLock(3, TimeUnit.SECONDS)) {
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
                    } finally {
                        projectUpdateLock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // process this interruption later
            }
        }
        // fall back to empty if failed to talk to the server
        Set<String> p = projects;
        if(p ==null) {
            return Collections.emptySet();
        }
        
        return p;
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

    public void createIssue(String realSummary, String realProject, String realIssueType, String realComponent, String realPriority, String realDescription) throws IOException, ServiceException {
        RemoteIssue remoteIssueToBeCreated = new RemoteIssue();
        String[] componentNames = realComponent.split(",");
        RemoteComponent [] components = new RemoteComponent [componentNames.length];
        for (int i = 0; i < componentNames.length; i++) {
            components[i] = new RemoteComponent();
            components[i].setId(componentNames[i]);

        }
        remoteIssueToBeCreated.setSummary(realSummary);
        remoteIssueToBeCreated.setProject(realProject);
        remoteIssueToBeCreated.setType(realIssueType);
        remoteIssueToBeCreated.setComponents(components);
        remoteIssueToBeCreated.setPriority(realPriority);
        remoteIssueToBeCreated.setDescription(realDescription);

        JiraSession session = createSession();
        session.createIssue(remoteIssueToBeCreated);

    }
    
    /**
     * Release a given version.
     * 
     * @param projectKey The Project Key
     * @param versionName The name of the version
     * @throws IOException
     * @throws ServiceException
     */
    public void releaseVersion(String projectKey, String versionName) throws IOException, ServiceException {
        JiraSession session = createSession();
        if (session != null) {
            RemoteVersion[] versions = session.getVersions(projectKey);
            if(versions == null ) return;
            for( RemoteVersion version : versions ) {
            	if(version.getName().equals(versionName)) {
            		version.setReleased(true);
            		version.setReleaseDate(Calendar.getInstance());
            		session.releaseVersion(projectKey,version);
            		return;
            	}
            }
        }
    }
    
    /**
     * Returns all versions for the given project key.
     * 
     * @param projectKey Project Key
     * @return A set of JiraVersions
     * @throws IOException
     * @throws ServiceException
     */
    public Set<JiraVersion> getVersions(String projectKey) throws IOException, ServiceException {
    	JiraSession session = createSession();
    	if(session == null) return Collections.emptySet();
    	
    	RemoteVersion[] versions = session.getVersions(projectKey);
    	
    	if(versions == null ) return Collections.emptySet();
    	
    	Set<JiraVersion> versionsSet = new HashSet<JiraVersion>(versions.length);
    			
    	for( int i = 0; i < versions.length; ++i) {
    		RemoteVersion version = versions[i];
    		versionsSet.add(new JiraVersion(version));
    	}
    	
    	return versionsSet;
    }
    
    /**
     * Generates release notes for a given version.
     * 
     * @param projectKey
     * @param versionName
     * @return release notes
     * @throws IOException
     * @throws ServiceException
     */
    public String getReleaseNotesForFixVersion(String projectKey, String versionName) throws IOException, ServiceException {
    	return getReleaseNotesForFixVersion(projectKey, versionName, "");
    }
    
    /**
     * Generates release notes for a given version.
     * 
     * @param projectKey
     * @param versionName
     * @param filter Additional JQL Filter. Example: status in (Resolved,Closed)
     * @return release notes
     * @throws IOException
     * @throws ServiceException
     */
    public String getReleaseNotesForFixVersion(String projectKey, String versionName, String filter) throws IOException, ServiceException {
    	JiraSession session = createSession();
    	if(session == null) return "";
    	
    	RemoteIssue[] issues = session.getIssuesWithFixVersion(projectKey, versionName, filter);
    	RemoteIssueType[] types = session.getIssueTypes();
    	
    	HashMap<String,String> typeNameMap = new HashMap<String,String>();
    	
    	for( RemoteIssueType type : types ) {
    		typeNameMap.put(type.getId(), type.getName());
    	}
    	    	
    	if(issues == null ) return "";

    	Map<String, Set<String>> releaseNotes = new HashMap<String,Set<String>>();
    	
    	for( int i = 0; i < issues.length; ++i ) {
    		RemoteIssue issue = issues[i];
    		String key = issue.getKey();
    		String summary =  issue.getSummary();
    		String type = "UNKNOWN";
    		
    		if( typeNameMap.containsKey(issue.getType())) {
    			type = typeNameMap.get(issue.getType());
    		}
    		
    		Set<String> issueSet;
    		if( !releaseNotes.containsKey(type)) {
    			issueSet = new HashSet<String>();
    			releaseNotes.put(type, issueSet);
    		} else {
    			issueSet = releaseNotes.get(type);
    		}
    		
    		issueSet.add(String.format(" - [%s] %s",key,summary));
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	for( String type : releaseNotes.keySet() ) {
    		sb.append(String.format("# %s\n",type));
    		for(String issue : releaseNotes.get(type)) {
    			sb.append(issue);
    			sb.append("\n");
    		}
    	}
    	
    	return sb.toString();
    }
    
    /**
     * Gets a set of issues that have the given fixVersion associated with them.
     * 
     * @param projectKey The project key
     * @param versionName The fixVersion
     * @return A set of JiraIssues
     * @throws IOException
     * @throws ServiceException
     */
    public Set<JiraIssue> getIssueWithFixVersion(String projectKey, String versionName) throws IOException, ServiceException {
    	JiraSession session = createSession();
    	if(session == null) return Collections.emptySet();
    	
    	RemoteIssue[] issues = session.getIssuesWithFixVersion(projectKey, versionName);
    	    	
    	if(issues == null ) return Collections.emptySet();
    	
    	Set<JiraIssue> issueSet = new HashSet<JiraIssue>(issues.length);
    			
    	for( int i = 0; i < issues.length; ++i) {
    		RemoteIssue issue = issues[i];
    		issueSet.add(new JiraIssue(issue));
    	}
    	
    	return issueSet;
    }
    
    /**
     * Migrates issues matching the jql query provided to a new fix version.
     * 
     * @param projectKey The project key
     * @param versionName The new fixVersion
     * @param query A JQL Query
     * @throws IOException
     * @throws ServiceException
     */
    public void replaceFixVersion(String projectKey, String fromVersion, String toVersion, String query) throws IOException, ServiceException {
    	JiraSession session = createSession();
    	if(session == null) return;
    	
    	session.replaceFixVersion(projectKey, fromVersion, toVersion, query);
    }
    
    /**
     * Migrates issues matching the jql query provided to a new fix version.
     * 
     * @param projectKey The project key
     * @param versionName The new fixVersion
     * @param query A JQL Query
     * @throws IOException
     * @throws ServiceException
     */
    public void migrateIssuesToFixVersion(String projectKey, String versionName, String query) throws IOException, ServiceException {
    	JiraSession session = createSession();
    	if(session == null) return;
    	
    	session.migrateIssuesToFixVersion(projectKey, versionName, query);
    }

    /**
     * Progresses all issues matching the JQL search, using the given workflow action. Optionally
     * adds a comment to the issue(s) at the same time.
     *
     * @param jqlSearch
     * @param workflowActionName
     * @param comment
     * @param console
     * @throws IOException
     * @throws ServiceException
     */
    public boolean progressMatchingIssues(String jqlSearch, String workflowActionName, String comment, PrintStream console) throws IOException,
            ServiceException {
        JiraSession session = createSession();

        if (session == null) {
            console.println(Messages.Updater_FailedToConnect());
            return false;
        }

        boolean success = true;
        RemoteIssue[] issues = session.getIssuesFromJqlSearch(jqlSearch);

        for (int i = 0; i < issues.length; i++) {
            String issueKey = issues[i].getKey();

            String actionId = session.getActionIdForIssue(issueKey, workflowActionName);

            if (actionId == null) {
                LOGGER.fine("Invalid workflow action " + workflowActionName + " for issue " + issueKey + "; issue status = " + issues[i].getStatus());
                console.println(Messages.JiraIssueUpdateBuilder_UnknownWorkflowAction(issueKey, workflowActionName));
                success = false;
                continue;
            }

            String newStatus = session.progressWorkflowAction(issueKey, actionId, null);

            console.println("[JIRA] Issue " + issueKey + " transitioned to \"" + newStatus
                    + "\" due to action \"" + workflowActionName + "\".");

            if (comment != null && !comment.isEmpty()) {
                session.addComment(issueKey, comment, null, null);
            }
        }

        return success;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<JiraSite> {
        @Override
        public String getDisplayName() {
            return "JIRA Site";
        }

        /**
         * Checks if the JIRA URL is accessible and exists.
         */
        public FormValidation doUrlCheck(@QueryParameter final String value)
                throws IOException, ServletException {
            // this can be used to check existence of any file in any URL, so
            // admin only
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER))
                return FormValidation.ok();

            return new FormValidation.URLCheck() {
                @Override
                protected FormValidation check() throws IOException,
                        ServletException {
                    String url = Util.fixEmpty(value);
                    if (url == null) {
                        return FormValidation.error(Messages
                                .JiraProjectProperty_JiraUrlMandatory());
                    }

                    // call the wsdl uri to check if the jira soap service can be reached
                    try {
                          if (!findText(open(new URL(url)), "Atlassian JIRA"))
                              return FormValidation.error(Messages
                                      .JiraProjectProperty_NotAJiraUrl());

                        URL soapUrl = new URL(new URL(url), "rpc/soap/jirasoapservice-v2?wsdl");
                        if (!findText(open(soapUrl), "wsdl:definitions"))
                              return FormValidation.error(Messages
                                      .JiraProjectProperty_NoWsdlAvailable());

                          return FormValidation.ok();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING,
                                "Unable to connect to " + url, e);
                        return handleIOException(url, e);
                    }
                }
            }.check();
        }

        public FormValidation doCheckUserPattern(@QueryParameter String value) throws IOException {
            String userPattern = Util.fixEmpty(value);
            if (userPattern == null) {// userPattern not entered yet
                return FormValidation.ok();
            }
            try {
                Pattern.compile(userPattern);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.error(e.getMessage());
            }
        }

        /**
         * Checks if the user name and password are valid.
         */
        public FormValidation doValidate(@QueryParameter String userName,
                                          @QueryParameter String url,
                                          @QueryParameter String password,
                                          @QueryParameter String groupVisibility,
                                          @QueryParameter String roleVisibility)
                throws IOException {
            url = Util.fixEmpty(url);
            if (url == null) {// URL not entered yet
                return FormValidation.error("No URL given");
            }
            JiraSite site = new JiraSite(new URL(url), userName, password, false,
                    false, null, false, groupVisibility, roleVisibility);
            try {
                site.createSession();
                return FormValidation.ok("Success");
            } catch (AxisFault e) {
                LOGGER.log(Level.WARNING, "Failed to login to JIRA at " + url,
                        e);
                return FormValidation.error(e.getFaultString());
            } catch (ServiceException e) {
                LOGGER.log(Level.WARNING, "Failed to login to JIRA at " + url,
                        e);
                return FormValidation.error(e.getMessage());
            }
        }
    }
    
    private static final Logger LOGGER = Logger.getLogger(JiraSite.class.getName());
}
