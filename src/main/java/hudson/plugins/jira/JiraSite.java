package hudson.plugins.jira;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.apache.httpcomponents.DefaultHttpClientFactory;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AtlassianHttpClientDecorator;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.plugins.jira.extension.ExtendedAsynchronousJiraRestClient;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import hudson.plugins.jira.extension.ExtendedVersion;
import hudson.plugins.jira.model.JiraIssue;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * <p>
 * Represents an external JIRA installation and configuration
 * needed to access this JIRA.
 * </p>
 * <b>When adding new fields do not miss to look at readResolve method!!</b>
 * @author Kohsuke Kawaguchi
 */
public class JiraSite extends AbstractDescribableImpl<JiraSite> {

    private static final Logger LOGGER = Logger.getLogger(JiraSite.class.getName());

    /**
     * Regexp pattern that identifies JIRA issue token.
     * If this pattern changes help pages (help-issue-pattern_xy.html) must be updated
     * First char must be a letter, then at least one letter, digit or underscore.
     * See issue JENKINS-729, JENKINS-4092
     */
    public static final Pattern DEFAULT_ISSUE_PATTERN = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*)([^.]|\\.[^0-9]|\\.$|$)");
    
    /**
     * Default rest api client calls timeout, in seconds
     * See issue JENKINS-31113 
     */
    public static final int DEFAULT_TIMEOUT = 10;

    public static final int DEFAULT_READ_TIMEOUT = 30;

    public static final int DEFAULT_THREAD_EXECUTOR_NUMBER = 10;
    
    /**
     * URL of JIRA for Jenkins access, like <tt>http://jira.codehaus.org/</tt>.
     * Mandatory. Normalized to end with '/'
     */
    public final URL url;

    /**
     * URL of JIRA for normal access, like <tt>http://jira.codehaus.org/</tt>.
     * Mandatory. Normalized to end with '/'
     */
    public URL alternativeUrl;

    /**
     * JIRA requires HTTP Authentication for login
     */
    public boolean useHTTPAuth;

    /**
     * The id of the credentials to use. Optional.
     */
    public String credentialsId;

    /**
     * Transient stash of the credentials to use, mostly just for providing floating user object.
     */
    public transient UsernamePasswordCredentials credentials;

    /**
     * User name needed to login. Optional.
     * @deprecated use credentialsId
     */
    @Deprecated
    private transient String userName;

    /**
     * Password needed to login. Optional.
     * @deprecated use credentialsId
     */
    @Deprecated
    private transient Secret password;

    /**
     * Group visibility to constrain the visibility of the added comment. Optional.
     */
    public String groupVisibility;

    /**
     * Role visibility to constrain the visibility of the added comment. Optional.
     */
    public String roleVisibility;

    /**
     * True if this JIRA is configured to allow Confluence-style Wiki comment.
     */
    public boolean supportsWikiStyleComment;

    /**
     * to record scm changes in jira issue
     *
     * @since 1.21
     */
    public boolean recordScmChanges;

    /**
     * Disable annotating the changelogs
     *
     * @since todo
     */
    public boolean disableChangelogAnnotations;

    /**
     * user defined pattern
     *
     * @since 1.22
     */
    private String userPattern;

    private transient Pattern userPat;

    /**
     * updated jira issue for all status
     *
     * @since 1.22
     */
    public boolean updateJiraIssueForAllStatus;
    
    /**
     * connection timeout used when calling jira rest api, in seconds
     */
    public int timeout = DEFAULT_TIMEOUT;

    /**
     * response timeout for jira rest call
     * @since 3.0.3
     */
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    /**
     * thread pool number
     * @since 3.0.3
     */
    private int threadExecutorNumber = DEFAULT_THREAD_EXECUTOR_NUMBER;

    /**
     * Configuration  for formatting (date -> text) in jira comments.
     */
    private String dateTimePattern;
    
    /**
     * To add scm entry change date and time in jira comments.
     *
     */
    private boolean appendChangeTimestamp;

    private int ioThreadCount = Integer.getInteger(JiraSite.class.getName() + ".httpclient.options.ioThreadCount", 2);

    /**
     * List of project keys (i.e., "MNG" portion of "MNG-512"),
     * last time we checked. Copy on write semantics.
     */
    // TODO: seems like this is never invalidated (never set to null)
    // should we implement to invalidate this (say every hour)?
    private transient volatile Set<String> projects;

    private transient Cache<String, Optional<Issue>> issueCache = makeIssueCache();

    /**
     * Used to guard the computation of {@link #projects}
     */
    private transient Lock projectUpdateLock = new ReentrantLock();

    private transient JiraSession jiraSession;

    private static ExecutorService executorService;

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(URL url, @CheckForNull URL alternativeUrl, @CheckForNull String credentialsId, boolean supportsWikiStyleComment, boolean recordScmChanges, @CheckForNull String userPattern,
                    boolean updateJiraIssueForAllStatus, @CheckForNull String groupVisibility, @CheckForNull String roleVisibility, boolean useHTTPAuth) {
        this(url, alternativeUrl, CredentialsHelper.lookupSystemCredentials(credentialsId, url), supportsWikiStyleComment, recordScmChanges, userPattern,
                updateJiraIssueForAllStatus, groupVisibility, roleVisibility, useHTTPAuth);
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(URL url, @CheckForNull URL alternativeUrl, String userName, String password, boolean supportsWikiStyleComment, boolean recordScmChanges, @CheckForNull String userPattern,
                    boolean updateJiraIssueForAllStatus, @CheckForNull String groupVisibility, @CheckForNull String roleVisibility, boolean useHTTPAuth) {
        this(url, alternativeUrl, CredentialsHelper.migrateCredentials(userName, password, url), supportsWikiStyleComment, recordScmChanges, userPattern,
                updateJiraIssueForAllStatus, groupVisibility, roleVisibility, useHTTPAuth);
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(URL url, URL alternativeUrl, StandardUsernamePasswordCredentials credentials, boolean supportsWikiStyleComment, boolean recordScmChanges, String userPattern,
                    boolean updateJiraIssueForAllStatus, String groupVisibility, String roleVisibility, boolean useHTTPAuth) {
        this( url, alternativeUrl, credentials, supportsWikiStyleComment, recordScmChanges, userPattern, updateJiraIssueForAllStatus,
              groupVisibility, roleVisibility, useHTTPAuth, DEFAULT_TIMEOUT, DEFAULT_READ_TIMEOUT, DEFAULT_THREAD_EXECUTOR_NUMBER);
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(URL url, URL alternativeUrl, String credentialsId, boolean supportsWikiStyleComment, boolean recordScmChanges, String userPattern,
                    boolean updateJiraIssueForAllStatus, String groupVisibility, String roleVisibility, boolean useHTTPAuth, int timeout, int readTimeout, int threadExecutorNumber){
        this(url, alternativeUrl, CredentialsHelper.lookupSystemCredentials(credentialsId, url), supportsWikiStyleComment, recordScmChanges, userPattern,
             updateJiraIssueForAllStatus, groupVisibility, roleVisibility, useHTTPAuth, timeout, readTimeout, threadExecutorNumber);
    }

    @DataBoundConstructor
    public JiraSite(String url){
        URL mainURL = toURL(url);
        if (mainURL == null) throw new AssertionError("URL cannot be empty");
        this.url = mainURL;
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(URL url, URL alternativeUrl, StandardUsernamePasswordCredentials credentials, boolean supportsWikiStyleComment, boolean recordScmChanges, String userPattern,
                    boolean updateJiraIssueForAllStatus, String groupVisibility, String roleVisibility, boolean useHTTPAuth, int timeout, int readTimeout, int threadExecutorNumber) {
        if (url != null) {
            url = toURL(url.toExternalForm());
        }

        if (alternativeUrl != null) {
            alternativeUrl = toURL(alternativeUrl.toExternalForm());
        }

        this.url = url;
        this.timeout = timeout;
        this.readTimeout = readTimeout;
        this.threadExecutorNumber = threadExecutorNumber;
        this.alternativeUrl = alternativeUrl;
        this.credentials = credentials;
        this.credentialsId = credentials != null ? credentials.getId() : null;
        this.supportsWikiStyleComment = supportsWikiStyleComment;
        this.recordScmChanges = recordScmChanges;
        setUserPattern(userPattern);
        this.updateJiraIssueForAllStatus = updateJiraIssueForAllStatus;
        setGroupVisibility(groupVisibility);
        setRoleVisibility(roleVisibility);
        this.useHTTPAuth = useHTTPAuth;
        this.jiraSession = null;
    }

    static URL toURL(String url) {
        url = Util.fixEmptyAndTrim(url);
        if (url == null) return null;
        if (!url.endsWith("/")) url = url + "/";
        try{
            return new URL(url);
        } catch (MalformedURLException e){
            throw new AssertionError(e);
        }
    }

    @DataBoundSetter
    public void setDisableChangelogAnnotations(boolean disableChangelogAnnotations) {
        this.disableChangelogAnnotations = disableChangelogAnnotations;
    }

    public boolean getDisableChangelogAnnotations() {
        return disableChangelogAnnotations;
    }

    /**
     * Sets connect timeout (in seconds).
     * If not specified, a default timeout will be used.
     * @param timeoutSec Timeout in seconds
     */
    @DataBoundSetter
    public void setTimeout(int timeoutSec) {
		this.timeout = timeoutSec;
	}

    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets read timeout (in seconds).
     * If not specified, a default timeout will be used.
     * @param readTimeout Timeout in seconds
     */
    @DataBoundSetter
    public void setReadTimeout( int readTimeout ) {
        this.readTimeout = readTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
        if (this.credentialsId == null) {
            this.credentials = null;
        } else {
            this.credentials = CredentialsHelper.lookupSystemCredentials(credentialsId, url);
        }
    }

    @DataBoundSetter
    public void setDateTimePattern(String dateTimePattern) {
        this.dateTimePattern = Util.fixEmptyAndTrim(dateTimePattern);
    }

    @DataBoundSetter
    public void setThreadExecutorNumber( int threadExecutorNumber ) {
        this.threadExecutorNumber = threadExecutorNumber;
    }

    public int getThreadExecutorNumber() {
        return threadExecutorNumber;
    }

    @DataBoundSetter
    public void setAppendChangeTimestamp(boolean appendChangeTimestamp) {
        this.appendChangeTimestamp = appendChangeTimestamp;
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }
    
    public boolean isAppendChangeTimestamp() {
        return appendChangeTimestamp;
    }

    public URL getAlternativeUrl() {
        return alternativeUrl;
    }

    public boolean isUseHTTPAuth() {
        return useHTTPAuth;
    }

    public String getGroupVisibility() {
        return groupVisibility;
    }

    public String getRoleVisibility() {
        return roleVisibility;
    }

    public boolean isSupportsWikiStyleComment() {
        return supportsWikiStyleComment;
    }

    public boolean isRecordScmChanges() {
        return recordScmChanges;
    }

    public boolean isUpdateJiraIssueForAllStatus() {
        return updateJiraIssueForAllStatus;
    }

    @DataBoundSetter
    public void setAlternativeUrl(String alternativeUrl) {
        this.alternativeUrl = toURL(alternativeUrl);
    }

    @DataBoundSetter
    public void setUseHTTPAuth(boolean useHTTPAuth) {
        this.useHTTPAuth = useHTTPAuth;
    }

    @DataBoundSetter
    public void setGroupVisibility(String groupVisibility) {
        this.groupVisibility = Util.fixEmptyAndTrim(groupVisibility);
    }

    @DataBoundSetter
    public void setRoleVisibility(String roleVisibility) {
        this.roleVisibility = Util.fixEmptyAndTrim(roleVisibility);
    }

    @DataBoundSetter
    public void setSupportsWikiStyleComment(boolean supportsWikiStyleComment) {
        this.supportsWikiStyleComment = supportsWikiStyleComment;
    }

    @DataBoundSetter
    public void setRecordScmChanges(boolean recordScmChanges) {
        this.recordScmChanges = recordScmChanges;
    }

    @DataBoundSetter
    public void setUserPattern(String userPattern) {
        this.userPattern = Util.fixEmptyAndTrim(userPattern);

        if (this.userPattern == null) {
            this.userPat = null;
        } else {
            this.userPat = Pattern.compile(this.userPattern);
        }
    }

    @DataBoundSetter
    public void setUpdateJiraIssueForAllStatus(boolean updateJiraIssueForAllStatus) {
        this.updateJiraIssueForAllStatus = updateJiraIssueForAllStatus;
    }

    @SuppressWarnings("unused")
    protected Object readResolve() {
        JiraSite jiraSite;
        if (credentialsId == null && userName != null && password != null) { // Migrate credentials
            jiraSite = new JiraSite(url, alternativeUrl, userName, password.getPlainText(), supportsWikiStyleComment, recordScmChanges, userPattern,
                    updateJiraIssueForAllStatus, groupVisibility, roleVisibility, useHTTPAuth);
        } else {
            jiraSite = new JiraSite( url, alternativeUrl, credentialsId, supportsWikiStyleComment, recordScmChanges,
                                 userPattern, updateJiraIssueForAllStatus, groupVisibility, roleVisibility, useHTTPAuth,
                                 timeout, readTimeout, threadExecutorNumber );
        }
        jiraSite.setAppendChangeTimestamp( appendChangeTimestamp );
        jiraSite.setDisableChangelogAnnotations( disableChangelogAnnotations );
        jiraSite.setDateTimePattern( dateTimePattern );
        return jiraSite;
    }

    private static Cache<String, Optional<Issue>> makeIssueCache() {
        return CacheBuilder.newBuilder().concurrencyLevel(2).expireAfterAccess(2, TimeUnit.MINUTES).build();
    }


    public String getName() {
        return url.toExternalForm();
    }

    /**
     * Gets a remote access session to this JIRA site.
     * Creates one if none exists already.
     *
     * @return null if remote access is not supported.
     */
    @Nullable
    public JiraSession getSession() {
        if (jiraSession == null) {
            jiraSession = createSession();
        }
        return jiraSession;
    }

    /**
     * Creates a remote access session to this JIRA.
     *
     * @return null if remote access is not supported.
     */
    private JiraSession createSession() {
        if (credentials == null) {
            return null;    // remote access not supported
        }

        final URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            LOGGER.warning("convert URL to URI error: " + e.getMessage());
            throw new RuntimeException("failed to create JiraSession due to convert URI error");
        }
        LOGGER.fine("creating JIRA Session: " + uri);

        String userName = credentials.getUsername();
        Secret password = credentials.getPassword();

        final ExtendedJiraRestClient jiraRestClient = new ExtendedAsynchronousJiraRestClientFactory()
            .create(uri, new BasicHttpAuthenticationHandler( userName, password.getPlainText()),getHttpClientOptions());
        return new JiraSession(this, new JiraRestService(uri, jiraRestClient, userName, password.getPlainText(), readTimeout));
    }

    private HttpClientOptions getHttpClientOptions() {
        final HttpClientOptions options = new HttpClientOptions();
        options.setRequestTimeout(readTimeout, TimeUnit.SECONDS);
        options.setSocketTimeout(timeout, TimeUnit.SECONDS);
        options.setCallbackExecutor(getExecutorService());
        options.setIoThreadCount(ioThreadCount);
        return options;
    }

    private ExecutorService getExecutorService() {
        if (executorService==null)
        {
            synchronized ( JiraSite.class )
            {
                int nThreads = threadExecutorNumber;
                if ( nThreads < 1 )
                {
                    LOGGER.warning( "nThreads " + nThreads + " cannot be lower than 1 so use default " + DEFAULT_THREAD_EXECUTOR_NUMBER );
                    nThreads = DEFAULT_THREAD_EXECUTOR_NUMBER;
                }
                executorService = Executors.newFixedThreadPool( nThreads, //
                                                                new ThreadFactory() {
                                                                    final AtomicInteger threadNumber = new AtomicInteger( 0 );
                                                                    @Override
                                                                    public Thread newThread( Runnable r )
                                                                    {
                                                                        return new Thread( r,
                                                                                           "jira-plugin-http-request-" + threadNumber.getAndIncrement()
                                                                                               + "-thread" );
                                                                    }
                                                                } );
            }
        }
        return executorService;
    }

    // not really used but let's leave when it will be implemented
    @PreDestroy
    public void destroy() {
        try {
            this.jiraSession = null;
        } catch ( Exception e ) {
            LOGGER.log(Level.WARNING, "skip error destroying JiraSite:" + e.getMessage(), e );
        }
    }

    //-----------------------------------------------------------------------------------
    // internal classes we want to override
    //-----------------------------------------------------------------------------------

    public static class ExtendedAsynchronousJiraRestClientFactory implements JiraRestClientFactory
    {

        public ExtendedJiraRestClient create(final URI serverUri, final AuthenticationHandler authenticationHandler, HttpClientOptions options) {
            final DisposableHttpClient httpClient = createClient(serverUri, authenticationHandler, options);
            return new ExtendedAsynchronousJiraRestClient( serverUri, httpClient);
        }

        @Override
        public ExtendedJiraRestClient create(final URI serverUri, final AuthenticationHandler authenticationHandler) {
            final DisposableHttpClient httpClient = createClient(serverUri, authenticationHandler, new HttpClientOptions());
            return new ExtendedAsynchronousJiraRestClient( serverUri, httpClient);
        }

        @Override
        public ExtendedJiraRestClient createWithBasicHttpAuthentication(final URI serverUri, final String username, final String password) {
            return create(serverUri, new BasicHttpAuthenticationHandler( username, password));
        }

        @Override
        public ExtendedJiraRestClient createWithAuthenticationHandler(final URI serverUri, final AuthenticationHandler authenticationHandler) {
            return create(serverUri, authenticationHandler);
        }

        @Override
        public ExtendedJiraRestClient create(final URI serverUri, final HttpClient httpClient) {
            final DisposableHttpClient disposableHttpClient = createClient(httpClient);
            return new ExtendedAsynchronousJiraRestClient(serverUri, disposableHttpClient);
        }
    }

    private static DisposableHttpClient createClient(final URI serverUri, final AuthenticationHandler authenticationHandler, HttpClientOptions options) {

        final DefaultHttpClientFactory
            defaultHttpClientFactory = new DefaultHttpClientFactory( new NoOpEventPublisher(),
                                                                     new RestClientApplicationProperties( serverUri),
                                                                     new ThreadLocalContextManager() {
                                                                         @Override
                                                                         public Object getThreadLocalContext() {
                                                                             return null;
                                                                         }

                                                                         @Override
                                                                         public void setThreadLocalContext(Object context) {
                                                                         }

                                                                         @Override
                                                                         public void clearThreadLocalContext() {
                                                                         }
                                                                     });

        final HttpClient httpClient = defaultHttpClientFactory.create(options);

        return new AtlassianHttpClientDecorator( httpClient, authenticationHandler) {
            @Override
            public void destroy() throws Exception {
                defaultHttpClientFactory.dispose(httpClient);
            }
        };
    }

    private static DisposableHttpClient createClient(final HttpClient client) {
        return new AtlassianHttpClientDecorator(client, null) {
            @Override
            public void destroy() throws Exception {
                // This should never be implemented. This is simply creation of a wrapper
                // for AtlassianHttpClient which is extended by a destroy method.
                // Destroy method should never be called for AtlassianHttpClient coming from
                // a client! Imagine you create a RestClient, pass your own HttpClient there
                // and it gets destroy.
            }
        };
    }

    private static class NoOpEventPublisher implements EventPublisher
    {
        @Override
        public void publish(Object o) {
        }

        @Override
        public void register(Object o) {
        }

        @Override
        public void unregister(Object o) {
        }

        @Override
        public void unregisterAll() {
        }
    }

    @SuppressWarnings("deprecation")
    private static class RestClientApplicationProperties implements ApplicationProperties
    {

        private final String baseUrl;

        private RestClientApplicationProperties(URI jiraURI) {
            this.baseUrl = jiraURI.getPath();
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * We'll always have an absolute URL as a client.
         */
        @Nonnull
        @Override
        public String getBaseUrl( UrlMode urlMode) {
            return baseUrl;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Atlassian JIRA Rest Java Client";
        }

        @Nonnull
        @Override
        public String getPlatformId() {
            return ApplicationProperties.PLATFORM_JIRA;
        }

        @Nonnull
        @Override
        public String getVersion() {
            return "";
        }

        @Nonnull
        @Override
        public Date getBuildDate() {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public String getBuildNumber() {
            return String.valueOf(0);
        }

        @Override
        public File getHomeDirectory() {
            return new File(".");
        }

        @Override
        public String getPropertyValue(final String s) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    //-----------------------------------------------------------------------------------
    //
    //-----------------------------------------------------------------------------------

    /**
     * @return the server URL
     */
    @Nullable
    public URL getUrl() {
        return Objects.firstNonNull(this.url, this.alternativeUrl);
    }

    /**
     * Computes the URL to the given issue.
     */
    public URL getUrl(JiraIssue issue) throws IOException {
        return getUrl(issue.getKey());
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
            userPat = Pattern.compile(userPattern);
        }
        return userPat;
    }

    public Pattern getIssuePattern() {
        Pattern result = getUserPattern();
        return result == null ? DEFAULT_ISSUE_PATTERN : result;
    }

    /**
     * Gets the list of project IDs in this JIRA.
     * This information could be bit old, or it can be null.
     */
    public Set<String> getProjectKeys() {
        if (projects == null) {
            try {
                if (projectUpdateLock.tryLock(3, TimeUnit.SECONDS)) {
                    try {
                        JiraSession session = getSession();
                        if (session != null) {
                            projects = Collections.unmodifiableSet(session.getProjectKeys());
                        }
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
        if (p == null) {
            return Collections.emptySet();
        }

        return p;
    }

    /**
     * Gets the effective {@link JiraSite} associated with the given project.
     *
     * @return null
     *         if no such was found.
     */
    public static JiraSite get(Job<?, ?> p) {
        JiraProjectProperty jpp = p.getProperty(JiraProjectProperty.class);
        if (jpp != null) {
            // Looks in global configuration for the site configured
            JiraSite site = jpp.getSite();
            if (site != null) {
                return site;
            }
        }

        // Check up the folder chain if a site is defined there
        // This only supports one site per folder
        ItemGroup parent = p.getParent();
        while (parent != null) {
            if (parent instanceof AbstractFolder) {
                AbstractFolder folder = (AbstractFolder) parent;
                JiraFolderProperty jfp = (JiraFolderProperty) folder.getProperties().get(JiraFolderProperty.class);
                if (jfp != null) {
                    JiraSite[] sites = jfp.getSites();
                    if (sites != null && sites.length > 0) {
                        return sites[0];
                    }
                }
            }

            if (parent instanceof Item) {
                parent = ((Item) parent).getParent();
            } else {
                parent = null;
            }
        }

        // none is explicitly configured. try the default ---
        // if only one is configured, that must be it.
        List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
        if (sites.size() == 1) {
            return sites.get(0);
        }

        return null;
    }

    /**
     * Returns the remote issue with the given id or <code>null</code> if it wasn't found.
     */
    @CheckForNull
    public JiraIssue getIssue(final String id) throws IOException {
        try {
            Optional<Issue> issue = issueCache.get(id, () -> {
                JiraSession session = getSession();
                if (session == null) {
                    return null;
                }
                return Optional.fromNullable(session.getIssue(id));
            });

            if (!issue.isPresent()) {
                return null;
            }

            return new JiraIssue(issue.get());
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    @Deprecated
    public boolean existsIssue(String id) {
        try {
            return getIssue(id) != null;
        } catch ( IOException e ) { // restoring backward compat means even avoid exception
            throw new RuntimeException( e.getMessage(),e );
        }
    }

    /**
     * Returns all versions for the given project key.
     *
     * @param projectKey Project Key
     * @return A set of JiraVersions
     */
    public Set<ExtendedVersion> getVersions(String projectKey) {
        JiraSession session = getSession();
        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            return Collections.emptySet();
        }

        return new HashSet<>(session.getVersions(projectKey));
    }

    /**
     * Generates release notes for a given version.
     *
     * @param projectKey the project key
     * @param versionName the version
     * @param filter      Additional JQL Filter. Example: status in (Resolved,Closed)
     * @return release notes
     * @throws TimeoutException if too long
     */
    public String getReleaseNotesForFixVersion(String projectKey, String versionName, String filter) throws TimeoutException {
        JiraSession session = getSession();
        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            return "";
        }

        List<Issue> issues = session.getIssuesWithFixVersion(projectKey, versionName, filter);

        if (issues.isEmpty()) {
            return "";
        }

        Map<String, Set<String>> releaseNotes = new HashMap<>();

        for (Issue issue : issues) {
            String key = issue.getKey();
            String summary = issue.getSummary();
            String status = issue.getStatus().getName();
            String type = issue.getIssueType().getName();

            Set<String> issueSet;
            if (releaseNotes.containsKey(type)) {
                issueSet = releaseNotes.get(type);
            } else {
                issueSet = new HashSet<>();
                releaseNotes.put(type, issueSet);
            }

            issueSet.add(String.format(" - [%s] %s (%s)", key, summary, status));
        }

        StringBuilder sb = new StringBuilder();
        for (String type : releaseNotes.keySet()) {
            sb.append(String.format("# %s\n", type));
            for (String issue : releaseNotes.get(type)) {
                sb.append(issue);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Migrates issues matching the jql query provided to a new fix version.
     *
     * @param projectKey The project key
     * @param toVersion  The new fixVersion
     * @param query      A JQL Query
     * @throws TimeoutException if too long
     */
    public void replaceFixVersion(String projectKey, String fromVersion, String toVersion, String query) throws TimeoutException {
        JiraSession session = getSession();
        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            return;
        }

        session.replaceFixVersion(projectKey, fromVersion, toVersion, query);
    }

    /**
     * Migrates issues matching the jql query provided to a new fix version.
     *
     * @param projectKey  The project key
     * @param versionName The new fixVersion
     * @param query       A JQL Query
     * @throws TimeoutException if too long
     */
    public void migrateIssuesToFixVersion(String projectKey, String versionName, String query) throws TimeoutException {
        JiraSession session = getSession();
        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            return;
        }

        session.migrateIssuesToFixVersion(projectKey, versionName, query);
    }

    /**
     * Adds new fix version to issues matching the jql.
     *
     * @param projectKey the project key
     * @param versionName the version
     * @param query the query
     * @throws TimeoutException if too long
     */
    public void addFixVersionToIssue(String projectKey, String versionName, String query) throws TimeoutException {
        JiraSession session = getSession();
        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            return;
        }

        session.addFixVersion(projectKey, versionName, query);
    }

    /**
     * Progresses all issues matching the JQL search, using the given workflow action. Optionally
     * adds a comment to the issue(s) at the same time.
     *
     * @param jqlSearch the query
     * @param workflowActionName the workflowActionName
     * @param comment the comment
     * @param console the console
     * @throws TimeoutException TimeoutException if too long
     */
    public boolean progressMatchingIssues(String jqlSearch, String workflowActionName, String comment, PrintStream console) throws TimeoutException {
        JiraSession session = getSession();

        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            console.println(Messages.FailedToConnect());
            return false;
        }

        boolean success = true;
        List<Issue> issues = session.getIssuesFromJqlSearch(jqlSearch);

        if (isEmpty(workflowActionName)) {
            console.println("[JIRA] No workflow action was specified, " +
                    "thus no status update will be made for any of the matching issues.");
        }

        for (Issue issue : issues) {
            String issueKey = issue.getKey();

            if (isNotEmpty(comment)) {
                session.addComment(issueKey, comment, null, null);
            }


            if (isEmpty(workflowActionName)) {
                continue;
            }

            Integer actionId = session.getActionIdForIssue(issueKey, workflowActionName);

            if (actionId == null) {
                LOGGER.fine(String.format("Invalid workflow action %s for issue %s; issue status = %s",
                        workflowActionName, issueKey, issue.getStatus()));
                console.println(Messages.JiraIssueUpdateBuilder_UnknownWorkflowAction(issueKey, workflowActionName));
                success = false;
                continue;
            }

            String newStatus = session.progressWorkflowAction(issueKey, actionId);

            console.println(String.format("[JIRA] Issue %s transitioned to \"%s\" due to action \"%s\".",
                    issueKey, newStatus, workflowActionName));
        }

        return success;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<JiraSite> {
        @Override
        public String getDisplayName() {
            return "JIRA Site";
        }


        @SuppressWarnings("unused") // used by stapler
        public FormValidation doCheckUrl(@QueryParameter String value)
            throws IOException, ServletException {
            return checkUrl(value);
        }

        @SuppressWarnings("unused") // used by stapler
        public FormValidation doCheckAlternativeUrl(@QueryParameter String value)
            throws IOException, ServletException {
            return checkUrl(value);
        }

        private FormValidation checkUrl(String url) {
            if (Util.fixEmptyAndTrim(url) == null) {
                return FormValidation.ok();
            }
            try{
                new URL(url);
            } catch (MalformedURLException e){
                return FormValidation.error(String.format("Malformed URL (%s)", url), e );
            }
            return FormValidation.ok();
        }

        /**
         * Checks if the user name and password are valid.
         */
        @RequirePOST
        public FormValidation doValidate(@QueryParameter String url,
                                         @QueryParameter String credentialsId,
                                         @QueryParameter String groupVisibility,
                                         @QueryParameter String roleVisibility,
                                         @QueryParameter boolean useHTTPAuth,
                                         @QueryParameter String alternativeUrl,
                                         @QueryParameter int timeout,
                                         @QueryParameter int readTimeout,
                                         @QueryParameter int threadExecutorNumber,
                                         @AncestorInPath Item item) {

            if (item == null) {
                Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            } else {
                item.checkPermission(Item.CONFIGURE);
            }

            url = Util.fixEmpty(url);
            alternativeUrl = Util.fixEmpty(alternativeUrl);
            URL mainURL, alternativeURL = null;

            try{
                if (url == null) {
                    return FormValidation.error("No URL given");
                }
                mainURL = new URL(url);
            } catch (MalformedURLException e){
                return FormValidation.error(String.format("Malformed URL (%s)", url), e );
            }

            try {
                if (alternativeUrl != null) {
                    alternativeURL = new URL(alternativeUrl);
                }
            }catch (MalformedURLException e){
                return FormValidation.error(String.format("Malformed alternative URL (%s)",alternativeUrl), e );
            }

            credentialsId = Util.fixEmpty(credentialsId);
            JiraSite site = getJiraSiteBuilder()
                    .withMainURL(mainURL)
                    .withAlternativeURL(alternativeURL)
                    .withCredentialsId(credentialsId)
                    .withGroupVisibility(groupVisibility)
                    .withRoleVisibility(roleVisibility)
                    .withUseHTTPAuth(useHTTPAuth)
                    .build();

            if(threadExecutorNumber<1){
                return FormValidation.error( Messages.JiraSite_threadExecutorMinimunSize("1"));
            }
            if(timeout<0){
                return FormValidation.error( Messages.JiraSite_timeoutMinimunValue( "1" ));
            }
            if(readTimeout<0){
                return FormValidation.error( Messages.JiraSite_readTimeoutMinimunValue( "1" ));
            }

            site.setTimeout(timeout);
            site.setReadTimeout(readTimeout);
            site.setThreadExecutorNumber(threadExecutorNumber);
            JiraSession session = null;
            try {
                session = site.getSession();
                session.getMyPermissions();
                return FormValidation.ok("Success");
            } catch (RestClientException e) {
                LOGGER.log(Level.WARNING, "Failed to login to JIRA at " + url, e);
            } finally {
                if(site!=null){
                    site.destroy();
                }
            }

            return FormValidation.error("Failed to login to JIRA");
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String url) {
            AccessControlled _context = (context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance());
            if (_context == null || !_context.hasPermission(Jenkins.ADMINISTER)) {
                return new StandardUsernameListBoxModel();
            }

            return new StandardUsernameListBoxModel()
                    .withEmptySelection()
                    .withAll(
                        CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class,
                            Jenkins.getInstance(),
                            ACL.SYSTEM,
                            URIRequirementBuilder.fromUri(url).build()
                        )
                    );
        }

        JiraSiteBuilder getJiraSiteBuilder() {
            return new JiraSiteBuilder();
        }
    }

    static class JiraSiteBuilder {
        private URL mainURL;
        private URL alternativeURL;
        private String credentialsId;
        private boolean supportsWikiStyleComment;
        private boolean recordScmChanges;
        private String userPattern;
        private boolean updateJiraIssueForAllStatus;
        private String groupVisibility;
        private String roleVisibility;
        private boolean useHTTPAuth;

        public JiraSiteBuilder withMainURL(URL mainURL) {
            this.mainURL = mainURL;
            return this;
        }

        public JiraSiteBuilder withAlternativeURL(URL alternativeURL) {
            this.alternativeURL = alternativeURL;
            return this;
        }

        public JiraSiteBuilder withCredentialsId(String credentialsId) {
            this.credentialsId = credentialsId;
            return this;
        }

        public JiraSiteBuilder withSupportsWikiStyleComment(boolean supportsWikiStyleComment) {
            this.supportsWikiStyleComment = supportsWikiStyleComment;
            return this;
        }

        public JiraSiteBuilder withRecordScmChanges(boolean recordScmChanges) {
            this.recordScmChanges = recordScmChanges;
            return this;
        }

        public JiraSiteBuilder withUserPattern(String userPattern) {
            this.userPattern = userPattern;
            return this;
        }

        public JiraSiteBuilder withUpdateJiraIssueForAllStatus(boolean updateJiraIssueForAllStatus) {
            this.updateJiraIssueForAllStatus = updateJiraIssueForAllStatus;
            return this;
        }

        public JiraSiteBuilder withGroupVisibility(String groupVisibility) {
            this.groupVisibility = groupVisibility;
            return this;
        }

        public JiraSiteBuilder withRoleVisibility(String roleVisibility) {
            this.roleVisibility = roleVisibility;
            return this;
        }

        public JiraSiteBuilder withUseHTTPAuth(boolean useHTTPAuth) {
            this.useHTTPAuth = useHTTPAuth;
            return this;
        }

        public JiraSite build() {
            return new JiraSite(mainURL, alternativeURL, credentialsId, supportsWikiStyleComment,
                    recordScmChanges, userPattern, updateJiraIssueForAllStatus, groupVisibility, roleVisibility, useHTTPAuth);
        }
    }

}
