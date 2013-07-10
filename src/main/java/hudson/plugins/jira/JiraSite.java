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
import hudson.plugins.jira.soap.RemoteVersion;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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
     * URL of JIRA for Jenkins access, like <tt>http://jira.codehaus.org/</tt>.
     * Mandatory. Normalized to end with '/'
     */
    public final URL url;

    /**
     * URL of JIRA for normal access, like <tt>http://jira.codehaus.org/</tt>.
     * Mandatory. Normalized to end with '/'
     */
    public final URL alternativeUrl;

    /**
     * Jira requires HTTP Authentication for login
     */
    public final boolean useHTTPAuth;

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
    
    private transient Cache<String,RemoteIssue> issueCache = makeIssueCache();

    /**
     * Used to guard the computation of {@link #projects}
     */
    private transient Lock projectUpdateLock = new ReentrantLock();
    
    private transient ThreadLocal<WeakReference<JiraSession>> jiraSession = new ThreadLocal<WeakReference<JiraSession>>();

    @DataBoundConstructor
    public JiraSite(URL url, URL alternativeUrl, String userName, String password, boolean supportsWikiStyleComment, boolean recordScmChanges, String userPattern,
                    boolean updateJiraIssueForAllStatus, String groupVisibility, String roleVisibility, boolean useHTTPAuth) {
        if(!url.toExternalForm().endsWith("/"))
            try {
                url = new URL(url.toExternalForm()+"/");
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }

        if(alternativeUrl!=null && !alternativeUrl.toExternalForm().endsWith("/"))
            try {
                alternativeUrl = new URL(alternativeUrl.toExternalForm()+"/");
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }

        this.url = url;
        this.alternativeUrl = alternativeUrl;
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
        this.useHTTPAuth = useHTTPAuth;
        this.jiraSession.set(new WeakReference<JiraSession>(null));
    }

    protected Object readResolve() {
        projectUpdateLock = new ReentrantLock();
        issueCache = makeIssueCache();
        jiraSession = new ThreadLocal<WeakReference<JiraSession>>();
        jiraSession.set(new WeakReference<JiraSession>(null));
        return this;
    }

    private static Cache<String, RemoteIssue> makeIssueCache() {
        return CacheBuilder.newBuilder().concurrencyLevel(2).expireAfterAccess(2, TimeUnit.MINUTES).build();
    }


    public String getName() {
        return url.toExternalForm();
    }

    /**
     * Gets a remote access session to this JIRA site.
     * Creates one if none exists already.
     *
     * @return
     *      null if remote access is not supported.
     */
    @Nullable
    public JiraSession getSession() throws IOException, ServiceException {
    	JiraSession session = null;
    	
        WeakReference<JiraSession> weakReference = jiraSession.get();
        if (weakReference != null) {
        	session = weakReference.get();
        }

        if (session == null) {
            // TODO: we should check for session timeout, too (but there's no method for that on JiraSoapService)
            // Currently no real problem, as we're using a weak reference for the session, so it will be GC'ed very quickly
            session = createSession();
            jiraSession.set(new WeakReference<JiraSession>(session));
        }
        return session;
    }
    
    /**
     * Creates a remote access session to this JIRA.
     *
     * @return
     *      null if remote access is not supported.
     * @deprecated please use {@link #getSession()} unless you really want a NEW session
     */
    @Deprecated
    public JiraSession createSession() throws IOException, ServiceException {
        if(userName==null || password==null)
            return null;    // remote access not supported
        JiraSoapServiceService jiraSoapServiceGetter = new JiraSoapServiceServiceLocator();

        if(useHTTPAuth) {
            String httpAuthUrl = url.toExternalForm().replace(
                    url.getHost(),
                    userName+":"+password+"@"+url.getHost())+"rpc/soap/jirasoapservice-v2";
            JiraSoapService service = jiraSoapServiceGetter.getJirasoapserviceV2(
                    new URL(httpAuthUrl));
            return new JiraSession(this,service,null); //no need to login
        }

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
     * Computes the alternative link URL to the given issue.
     */
    public URL getAlternativeUrl(String id) throws MalformedURLException {
        return alternativeUrl == null ? null : new URL(alternativeUrl, "browse/" + id.toUpperCase());
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
                            JiraSession session = getSession();
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
    
    private static final RemoteIssue NULL = new RemoteIssue();
    
    /**
     * Returns the remote issue with the given id or <code>null</code> if it wasn't found.
     */
    @CheckForNull
    public JiraIssue getIssue(final String id) throws IOException, ServiceException {
        
        try {
            RemoteIssue remoteIssue = issueCache.get(id, new Callable<RemoteIssue>() {
                public RemoteIssue call() throws Exception {
                    JiraSession session = getSession();
                    RemoteIssue issue = null;
                    if (session != null) {
                        issue = session.getIssue(id);
                    }
                    
                    return issue != null ? issue : NULL;
                }
            });
            
            if (remoteIssue == NULL) {
                return null;
            }
            
            return new JiraIssue(remoteIssue);
        } catch (ExecutionException e) {
            throw new ServiceException(e);
        }
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
        JiraSession session = getSession();
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
    	JiraSession session = getSession();
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
    	JiraSession session = getSession();
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
    	JiraSession session = getSession();
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
    	JiraSession session = getSession();
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
    	JiraSession session = getSession();
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
        JiraSession session = getSession();

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
                                          @QueryParameter String roleVisibility,
                                          @QueryParameter boolean useHTTPAuth,
                                          @QueryParameter String alternativeUrl)
                throws IOException {
            url = Util.fixEmpty(url);
            alternativeUrl = Util.fixEmpty(alternativeUrl);
            if (url == null) {// URL not entered yet
                return FormValidation.error("No URL given");
            }

            URL altUrl = null;

            if ( StringUtils.isNotEmpty( alternativeUrl )) {
                altUrl = new URL(alternativeUrl);
            }


            JiraSite site = new JiraSite(new URL(url), altUrl, userName, password, false,
                    false, null, false, groupVisibility, roleVisibility, useHTTPAuth);
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
