package hudson.plugins.jira;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

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
import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.plugins.jira.extension.ExtendedAsynchronousJiraRestClient;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import hudson.plugins.jira.extension.ExtendedVersion;
import hudson.plugins.jira.model.JiraIssue;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jakarta.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import javax.annotation.PreDestroy;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * <b>You must get instance of this only by using the static {@link #get} or {@link #getSitesFromFolders(ItemGroup)} methods</b>
 * <b>The constructors are only used by Jenkins</b>
 * <p>
 * Represents an external Jira installation and configuration
 * needed to access this Jira.
 * </p>
 * <b>When adding new fields do not miss to look at readResolve method!!</b>
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraSite extends AbstractDescribableImpl<JiraSite> {

    private static final Logger LOGGER = Logger.getLogger(JiraSite.class.getName());

    /**
     * Regexp pattern that identifies Jira issue token.
     * If this pattern changes help pages (help-issue-pattern_xy.html) must be updated
     * First char must be a letter, then at least one letter, digit or underscore.
     * See issue JENKINS-729, JENKINS-4092
     */
    public static final Pattern DEFAULT_ISSUE_PATTERN =
            Pattern.compile("([a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*)([^.]|\\.[^0-9]|\\.$|$)");

    /**
     * Default rest api client calls timeout, in seconds
     * See issue JENKINS-31113
     */
    public static final int DEFAULT_TIMEOUT = 10;

    public static final int DEFAULT_READ_TIMEOUT = 30;

    public static final int DEFAULT_THREAD_EXECUTOR_NUMBER = 10;

    public static final Integer MAX_ALLOWED_ISSUES_FROM_JQL = 5000;

    /**
     * URL of Jira for Jenkins access, like {@code http://jira.codehaus.org/}.
     * Mandatory. Normalized to end with '/'
     */
    public final URL url;

    /**
     * URL of Jira for normal access, like {@code http://jira.codehaus.org/}.
     * Mandatory. Normalized to end with '/'
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public URL alternativeUrl;

    /**
     * Jira requires HTTP Authentication for login
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public boolean useHTTPAuth;

    /**
     * The id of the credentials to use. Optional.
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public String credentialsId;

    /**
     * Jira requires Bearer Authentication for login
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public boolean useBearerAuth;

    /**
     * User name needed to login. Optional.
     *
     * @deprecated use credentialsId
     */
    @Deprecated
    private transient String userName;

    /**
     * Password needed to login. Optional.
     *
     * @deprecated use credentialsId
     */
    @Deprecated
    private transient Secret password;

    /**
     * Group visibility to constrain the visibility of the added comment. Optional.
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public String groupVisibility;

    /**
     * Role visibility to constrain the visibility of the added comment. Optional.
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public String roleVisibility;

    /**
     * True if this Jira is configured to allow Confluence-style Wiki comment.
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public boolean supportsWikiStyleComment;

    /**
     * to record scm changes in jira issue
     *
     * @since 1.21
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public boolean recordScmChanges;

    /**
     * Disable annotating the changelogs
     *
     * @since todo
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
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
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public boolean updateJiraIssueForAllStatus;

    /**
     * connection timeout used when calling jira rest api, in seconds
     */
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public int timeout = DEFAULT_TIMEOUT;

    /**
     * response timeout for jira rest call
     *
     * @since 3.0.3
     */
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    /**
     * thread pool number
     *
     * @since 3.0.3
     */
    private int threadExecutorNumber = DEFAULT_THREAD_EXECUTOR_NUMBER;

    /**
     * Configuration  for formatting (date -> text) in jira comments.
     */
    private String dateTimePattern;

    /**
     * To add scm entry change date and time in jira comments.
     */
    private boolean appendChangeTimestamp;

    /**
     * To allow configurable value of max issues from jql search via jira site global configuration.
     */
    private int maxIssuesFromJqlSearch;

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
    public JiraSite(
            URL url,
            @CheckForNull URL alternativeUrl,
            @CheckForNull String credentialsId,
            boolean supportsWikiStyleComment,
            boolean recordScmChanges,
            @CheckForNull String userPattern,
            boolean updateJiraIssueForAllStatus,
            @CheckForNull String groupVisibility,
            @CheckForNull String roleVisibility,
            boolean useHTTPAuth) {
        this(
                url,
                alternativeUrl,
                credentialsId,
                supportsWikiStyleComment,
                recordScmChanges,
                userPattern,
                updateJiraIssueForAllStatus,
                groupVisibility,
                roleVisibility,
                useHTTPAuth,
                DEFAULT_TIMEOUT,
                DEFAULT_READ_TIMEOUT,
                DEFAULT_THREAD_EXECUTOR_NUMBER);
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(
            URL url,
            @CheckForNull URL alternativeUrl,
            String userName,
            String password,
            boolean supportsWikiStyleComment,
            boolean recordScmChanges,
            @CheckForNull String userPattern,
            boolean updateJiraIssueForAllStatus,
            @CheckForNull String groupVisibility,
            @CheckForNull String roleVisibility,
            boolean useHTTPAuth)
            throws FormException {
        this(
                url,
                alternativeUrl,
                CredentialsHelper.migrateCredentials(userName, password, url),
                supportsWikiStyleComment,
                recordScmChanges,
                userPattern,
                updateJiraIssueForAllStatus,
                groupVisibility,
                roleVisibility,
                useHTTPAuth);
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(
            URL url,
            URL alternativeUrl,
            StandardUsernamePasswordCredentials credentials,
            boolean supportsWikiStyleComment,
            boolean recordScmChanges,
            String userPattern,
            boolean updateJiraIssueForAllStatus,
            String groupVisibility,
            String roleVisibility,
            boolean useHTTPAuth)
            throws FormException {
        this(
                url,
                alternativeUrl,
                (String) null,
                supportsWikiStyleComment,
                recordScmChanges,
                userPattern,
                updateJiraIssueForAllStatus,
                groupVisibility,
                roleVisibility,
                useHTTPAuth,
                DEFAULT_TIMEOUT,
                DEFAULT_READ_TIMEOUT,
                DEFAULT_THREAD_EXECUTOR_NUMBER);
        if (credentials != null) {
            // we verify the credential really exists otherwise we migrate it
            StandardUsernamePasswordCredentials standardUsernamePasswordCredentials =
                    CredentialsHelper.lookupSystemCredentials(credentials.getId(), url);
            if (standardUsernamePasswordCredentials == null) {
                credentials = CredentialsHelper.migrateCredentials(
                        credentials.getUsername(), credentials.getPassword().getPlainText(), url);
            }
        }
        setCredentialsId(credentials == null ? null : credentials.getId());
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(
            URL url,
            URL alternativeUrl,
            String credentialsId,
            boolean supportsWikiStyleComment,
            boolean recordScmChanges,
            String userPattern,
            boolean updateJiraIssueForAllStatus,
            String groupVisibility,
            String roleVisibility,
            boolean useHTTPAuth,
            int timeout,
            int readTimeout,
            int threadExecutorNumber) {
        if (url != null) {
            url = toURL(url.toExternalForm());
        }

        if (alternativeUrl != null) {
            alternativeUrl = toURL(alternativeUrl.toExternalForm());
        }

        this.url = url;
        this.credentialsId = credentialsId;
        this.timeout = timeout;
        this.readTimeout = readTimeout;
        this.threadExecutorNumber = threadExecutorNumber;
        this.alternativeUrl = alternativeUrl;
        this.supportsWikiStyleComment = supportsWikiStyleComment;
        this.recordScmChanges = recordScmChanges;
        setUserPattern(userPattern);
        this.updateJiraIssueForAllStatus = updateJiraIssueForAllStatus;
        setGroupVisibility(groupVisibility);
        setRoleVisibility(roleVisibility);
        this.useHTTPAuth = useHTTPAuth;
        this.jiraSession = null;
    }

    @DataBoundConstructor
    public JiraSite(String url) {
        URL mainURL = toURL(url);
        if (mainURL == null) {
            throw new AssertionError("URL cannot be empty");
        }
        this.url = mainURL;
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(
            URL url,
            URL alternativeUrl,
            StandardUsernamePasswordCredentials credentials,
            boolean supportsWikiStyleComment,
            boolean recordScmChanges,
            String userPattern,
            boolean updateJiraIssueForAllStatus,
            String groupVisibility,
            String roleVisibility,
            boolean useHTTPAuth,
            int timeout,
            int readTimeout,
            int threadExecutorNumber) {
        this(
                url,
                alternativeUrl,
                credentials == null ? null : credentials.getId(),
                supportsWikiStyleComment,
                recordScmChanges,
                userPattern,
                updateJiraIssueForAllStatus,
                groupVisibility,
                roleVisibility,
                useHTTPAuth,
                timeout,
                readTimeout,
                threadExecutorNumber);
    }

    // Deprecate the previous constructor but leave it in place for Java-level compatibility.
    @Deprecated
    public JiraSite(
            URL url,
            URL alternativeUrl,
            StandardUsernamePasswordCredentials credentials,
            boolean supportsWikiStyleComment,
            boolean recordScmChanges,
            String userPattern,
            boolean updateJiraIssueForAllStatus,
            String groupVisibility,
            String roleVisibility,
            boolean useHTTPAuth,
            int timeout,
            int readTimeout,
            int threadExecutorNumber,
            boolean useBearerAuth) {
        this(
                url,
                alternativeUrl,
                credentials == null ? null : credentials.getId(),
                supportsWikiStyleComment,
                recordScmChanges,
                userPattern,
                updateJiraIssueForAllStatus,
                groupVisibility,
                roleVisibility,
                useHTTPAuth,
                timeout,
                readTimeout,
                threadExecutorNumber);
        this.useBearerAuth = useBearerAuth;
    }

    static URL toURL(String url) {
        url = Util.fixEmptyAndTrim(url);
        if (url == null) {
            return null;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
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
     *
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
     *
     * @param readTimeout Timeout in seconds
     */
    @DataBoundSetter
    public void setReadTimeout(int readTimeout) {
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
    }

    @DataBoundSetter
    public void setDateTimePattern(String dateTimePattern) {
        this.dateTimePattern = Util.fixEmptyAndTrim(dateTimePattern);
    }

    @DataBoundSetter
    public void setThreadExecutorNumber(int threadExecutorNumber) {
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

    public boolean isUseBearerAuth() {
        return useBearerAuth;
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
    public void setUseBearerAuth(boolean useBearerAuth) {
        this.useBearerAuth = useBearerAuth;
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

    @DataBoundSetter
    public void setMaxIssuesFromJqlSearch(int maxIssuesFromJqlSearch) {
        this.maxIssuesFromJqlSearch = maxIssuesFromJqlSearch > MAX_ALLOWED_ISSUES_FROM_JQL
                ? MAX_ALLOWED_ISSUES_FROM_JQL
                : maxIssuesFromJqlSearch;
    }

    public int getMaxIssuesFromJqlSearch() {
        return maxIssuesFromJqlSearch;
    }

    @SuppressWarnings("unused")
    protected Object readResolve() throws FormException {
        JiraSite jiraSite;

        if (credentialsId == null && userName != null && password != null) { // Migrate credentials
            jiraSite = new JiraSite(
                    url,
                    alternativeUrl,
                    userName,
                    password.getPlainText(),
                    supportsWikiStyleComment,
                    recordScmChanges,
                    userPattern,
                    updateJiraIssueForAllStatus,
                    groupVisibility,
                    roleVisibility,
                    useHTTPAuth);
        } else {
            jiraSite = new JiraSite(
                    url,
                    alternativeUrl,
                    credentialsId,
                    supportsWikiStyleComment,
                    recordScmChanges,
                    userPattern,
                    updateJiraIssueForAllStatus,
                    groupVisibility,
                    roleVisibility,
                    useHTTPAuth,
                    timeout,
                    readTimeout,
                    threadExecutorNumber);
        }
        jiraSite.setAppendChangeTimestamp(appendChangeTimestamp);
        jiraSite.setDisableChangelogAnnotations(disableChangelogAnnotations);
        jiraSite.setDateTimePattern(dateTimePattern);
        jiraSite.setUseBearerAuth(useBearerAuth);
        jiraSite.setMaxIssuesFromJqlSearch(maxIssuesFromJqlSearch);
        return jiraSite;
    }

    protected static Cache<String, Optional<Issue>> makeIssueCache() {
        return Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).build();
    }

    public String getName() {
        return url.toExternalForm();
    }

    /**
     * @deprecated should not be used
     */
    @Deprecated
    public JiraSession getSession() {
        return getSession(null);
    }

    /**
     * Gets a remote access session to this Jira site (job-aware)
     * Creates one if none exists already.
     *
     * @return null if remote access is not supported.
     */
    @Nullable
    public JiraSession getSession(Item item) {
        return getSession(item, false);
    }

    JiraSession getSession(Item item, boolean uiValidation) {
        if (jiraSession == null) {
            jiraSession = createSession(item, uiValidation);
        }
        return jiraSession;
    }

    JiraSession createSession(Item item) {
        return createSession(item, false);
    }

    /**
     * Creates a remote access session to this Jira.
     *
     * @return null if remote access is not supported.
     */
    JiraSession createSession(Item item, boolean uiValidation) {
        ItemGroup itemGroup = map(item);
        item = itemGroup instanceof Folder ? ((Folder) itemGroup) : item;

        StandardUsernamePasswordCredentials credentials = resolveCredentials(item, uiValidation);

        if (credentials == null) {
            LOGGER.fine("no Jira credentials available for " + item);
            return null; // remote access not supported
        }

        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            LOGGER.warning("convert URL to URI error: " + e.getMessage());
            throw new RuntimeException("failed to create JiraSession due to convert URI error");
        }
        LOGGER.fine("creating Jira Session: " + uri);

        return JiraSessionFactory.create(this, uri, credentials);
    }

    Lock getProjectUpdateLock() {
        return projectUpdateLock;
    }

    /**
     * This method only supports credential matching by credentialsId.
     * Older methods are not and will not be supported as the credentials should have been migrated already.
     *
     * @param item         can be <code>null</code> if top level
     * @param uiValidation if <code>true</code> and credentials not found at item level will not go up
     */
    private StandardUsernamePasswordCredentials resolveCredentials(Item item, boolean uiValidation) {
        if (credentialsId == null) {
            LOGGER.fine("credentialsId is null");
            return null; // remote access not supported
        }

        List<DomainRequirement> req = URIRequirementBuilder.fromUri(url != null ? url.toExternalForm() : null)
                .build();

        if (item != null) {
            StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class, item, ACL.SYSTEM, req),
                    CredentialsMatchers.withId(credentialsId));
            if (credentials != null) {
                return credentials;
            }
            // during UI validation of the configuration we definitely don't want to expose
            // global credentials
            if (uiValidation) {
                return null;
            }
        }
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class, Jenkins.get(), ACL.SYSTEM, req),
                CredentialsMatchers.withId(credentialsId));
    }

    protected HttpClientOptions getHttpClientOptions() {
        final HttpClientOptions options = new HttpClientOptions();
        options.setRequestTimeout(readTimeout, TimeUnit.SECONDS);
        options.setSocketTimeout(timeout, TimeUnit.SECONDS);
        options.setCallbackExecutor(getExecutorService());
        options.setIoThreadCount(ioThreadCount);
        return options;
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            synchronized (JiraSite.class) {
                int nThreads = threadExecutorNumber;
                if (nThreads < 1) {
                    LOGGER.warning("nThreads " + nThreads + " cannot be lower than 1 so use default "
                            + DEFAULT_THREAD_EXECUTOR_NUMBER);
                    nThreads = DEFAULT_THREAD_EXECUTOR_NUMBER;
                }
                executorService = Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
                    final AtomicInteger threadNumber = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "jira-plugin-http-request-" + threadNumber.getAndIncrement() + "-thread");
                    }
                });
            }
        }
        return executorService;
    }

    // not really used but let's leave when it will be implemented
    @PreDestroy
    public void destroy() {
        try {
            this.jiraSession = null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "skip error destroying JiraSite:" + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------------------
    // internal classes we want to override
    // -----------------------------------------------------------------------------------

    public static class ExtendedAsynchronousJiraRestClientFactory implements JiraRestClientFactory {

        public ExtendedJiraRestClient create(
                final URI serverUri, final AuthenticationHandler authenticationHandler, HttpClientOptions options) {
            final DisposableHttpClient httpClient = createClient(serverUri, authenticationHandler, options);
            Thread t = Thread.currentThread();
            ClassLoader orig = t.getContextClassLoader();
            t.setContextClassLoader(JiraSite.class.getClassLoader());
            try {
                return new ExtendedAsynchronousJiraRestClient(serverUri, httpClient);
            } finally {
                t.setContextClassLoader(orig);
            }
        }

        @Override
        public ExtendedJiraRestClient create(final URI serverUri, final AuthenticationHandler authenticationHandler) {
            final DisposableHttpClient httpClient =
                    createClient(serverUri, authenticationHandler, new HttpClientOptions());
            return new ExtendedAsynchronousJiraRestClient(serverUri, httpClient);
        }

        @Override
        public ExtendedJiraRestClient createWithBasicHttpAuthentication(
                final URI serverUri, final String username, final String password) {
            return create(serverUri, new BasicHttpAuthenticationHandler(username, password));
        }

        @Override
        public ExtendedJiraRestClient createWithAuthenticationHandler(
                final URI serverUri, final AuthenticationHandler authenticationHandler) {
            return create(serverUri, authenticationHandler);
        }

        @Override
        public ExtendedJiraRestClient create(final URI serverUri, final HttpClient httpClient) {
            final DisposableHttpClient disposableHttpClient = createClient(httpClient);
            return new ExtendedAsynchronousJiraRestClient(serverUri, disposableHttpClient);
        }
    }

    private static DisposableHttpClient createClient(
            final URI serverUri, final AuthenticationHandler authenticationHandler, HttpClientOptions options) {

        final DefaultHttpClientFactory defaultHttpClientFactory = new DefaultHttpClientFactory(
                new NoOpEventPublisher(),
                new RestClientApplicationProperties(serverUri),
                new ThreadLocalContextManager() {
                    @Override
                    public Object getThreadLocalContext() {
                        return null;
                    }

                    @Override
                    public void setThreadLocalContext(Object context) {}

                    @Override
                    public void clearThreadLocalContext() {}
                });

        final HttpClient httpClient = defaultHttpClientFactory.create(options);

        return new AtlassianHttpClientDecorator(httpClient, authenticationHandler) {
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

    private static class NoOpEventPublisher implements EventPublisher {
        @Override
        public void publish(Object o) {}

        @Override
        public void register(Object o) {}

        @Override
        public void unregister(Object o) {}

        @Override
        public void unregisterAll() {}
    }

    @SuppressWarnings("deprecation")
    private static class RestClientApplicationProperties implements ApplicationProperties {

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
        @NonNull
        @Override
        public String getBaseUrl(UrlMode urlMode) {
            return baseUrl;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Atlassian Jira Rest Java Client";
        }

        @NonNull
        @Override
        public String getPlatformId() {
            return ApplicationProperties.PLATFORM_JIRA;
        }

        @NonNull
        @Override
        public String getVersion() {
            return "";
        }

        @NonNull
        @Override
        public Date getBuildDate() {
            throw new UnsupportedOperationException();
        }

        @NonNull
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

        @NonNull
        @Override
        public String getApplicationFileEncoding() {
            return System.getProperty("file.encoding");
        }

        @NonNull
        @Override
        public Optional<Path> getLocalHomeDirectory() {
            return Optional.empty();
        }

        @NonNull
        @Override
        public Optional<Path> getSharedHomeDirectory() {
            return Optional.empty();
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------------------

    /**
     * @return the server URL
     */
    @Nullable
    public URL getUrl() {
        return this.url != null ? this.url : this.alternativeUrl;
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
     * Gets the list of project IDs in this Jira.
     * This information could be bit old, or it can be null.
     */
    public Set<String> getProjectKeys(Item item) {
        // FIXME it means projects list will be never updated until Jenkins is restarted...
        if (projects == null) {
            try {
                if (getProjectUpdateLock().tryLock(3, TimeUnit.SECONDS)) {
                    try {
                        JiraSession session = getSession(item);
                        if (session != null) {
                            projects = Collections.unmodifiableSet(session.getProjectKeys());
                        }
                    } finally {
                        getProjectUpdateLock().unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // process this interruption later
            }
        }
        // fall back to empty if failed to talk to the server
        if (projects == null) {
            return Collections.emptySet();
        }

        return projects;
    }

    /**
     * Returns the remote issue with the given id or <code>null</code> if it wasn't found.
     */
    @CheckForNull
    public JiraIssue getIssue(final String id) throws IOException {
        Optional<Issue> issue = issueCache.get(id, s -> {
            if (this.jiraSession == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(this.jiraSession.getIssue(id));
        });

        if (issue == null || !issue.isPresent()) {
            return null;
        }
        return new JiraIssue(issue.get());
    }

    @Deprecated
    public boolean existsIssue(String id) {
        try {
            return getIssue(id) != null;
        } catch (IOException e) { // restoring backward compat means even avoid exception
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns all versions for the given project key.
     *
     * @param projectKey Project Key
     * @return A set of JiraVersions
     * @deprecated use {@link JiraSession#getVersions(String)}
     */
    @Deprecated
    public Set<ExtendedVersion> getVersions(String projectKey) {
        if (this.jiraSession == null) {
            LOGGER.warning("Jira session could not be established");
            return Collections.emptySet();
        }
        return new HashSet<>(this.jiraSession.getVersions(projectKey));
    }

    /**
     * Generates release notes for a given version.
     *
     * @param projectKey  the project key
     * @param versionName the version
     * @param filter      Additional JQL Filter. Example: status in (Resolved,Closed)
     * @return release notes
     * @throws TimeoutException if too long
     */
    public String getReleaseNotesForFixVersion(String projectKey, String versionName, String filter)
            throws TimeoutException {
        if (this.jiraSession == null) {
            LOGGER.warning("Jira session could not be established");
            return "";
        }

        List<Issue> issues = this.jiraSession.getIssuesWithFixVersion(projectKey, versionName, filter);

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
        for (Map.Entry<String, Set<String>> entry : releaseNotes.entrySet()) {
            sb.append(String.format("# %s%n", entry.getKey()));
            for (String issue : entry.getValue()) {
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
    public void replaceFixVersion(String projectKey, String fromVersion, String toVersion, String query)
            throws TimeoutException {
        if (this.jiraSession == null) {
            LOGGER.warning("Jira session could not be established");
            return;
        }
        this.jiraSession.replaceFixVersion(projectKey, fromVersion, toVersion, query);
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
        if (this.jiraSession == null) {
            LOGGER.warning("Jira session could not be established");
            return;
        }
        this.jiraSession.migrateIssuesToFixVersion(projectKey, versionName, query);
    }

    /**
     * Adds new fix version to issues matching the jql.
     *
     * @param projectKey  the project key
     * @param versionName the version
     * @param query       the query
     * @throws TimeoutException if too long
     */
    public void addFixVersionToIssue(String projectKey, String versionName, String query) throws TimeoutException {
        if (this.jiraSession == null) {
            LOGGER.warning("Jira session could not be established");
            return;
        }
        this.jiraSession.addFixVersion(projectKey, versionName, query);
    }

    /**
     * Progresses all issues matching the JQL search, using the given workflow action. Optionally
     * adds a comment to the issue(s) at the same time.
     *
     * @param jqlSearch          the query
     * @param workflowActionName the workflowActionName
     * @param comment            the comment
     * @param console            the console
     * @throws TimeoutException TimeoutException if too long
     */
    public boolean progressMatchingIssues(
            String jqlSearch, String workflowActionName, String comment, PrintStream console) throws TimeoutException {

        if (this.jiraSession == null) {
            LOGGER.warning("Jira session could not be established");
            console.println(Messages.FailedToConnect());
            return false;
        }

        boolean success = true;
        List<Issue> issues = this.jiraSession.getIssuesFromJqlSearch(jqlSearch);

        if (isEmpty(workflowActionName)) {
            console.println("[Jira] No workflow action was specified, "
                    + "thus no status update will be made for any of the matching issues.");
        }

        for (Issue issue : issues) {
            String issueKey = issue.getKey();

            if (isNotEmpty(comment)) {
                this.jiraSession.addComment(issueKey, comment, null, null);
            }

            if (isEmpty(workflowActionName)) {
                continue;
            }

            Integer actionId = this.jiraSession.getActionIdForIssue(issueKey, workflowActionName);

            if (actionId == null) {
                LOGGER.fine(String.format(
                        "Invalid workflow action %s for issue %s; issue status = %s",
                        workflowActionName, issueKey, issue.getStatus()));
                console.println(Messages.JiraIssueUpdateBuilder_UnknownWorkflowAction(issueKey, workflowActionName));
                success = false;
                continue;
            }

            String newStatus = this.jiraSession.progressWorkflowAction(issueKey, actionId);

            console.println(String.format(
                    "[Jira] Issue %s transitioned to \"%s\" due to action \"%s\".",
                    issueKey, newStatus, workflowActionName));
        }

        return success;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<JiraSite> {
        @Override
        public String getDisplayName() {
            return "Jira Site";
        }

        @SuppressWarnings("unused") // used by stapler
        public FormValidation doCheckUrl(@QueryParameter String value) throws IOException, ServletException {
            return checkUrl(value);
        }

        @SuppressWarnings("unused") // used by stapler
        public FormValidation doCheckAlternativeUrl(@QueryParameter String value) throws IOException, ServletException {
            return checkUrl(value);
        }

        private FormValidation checkUrl(String url) {
            if (Util.fixEmptyAndTrim(url) == null) {
                return FormValidation.ok();
            }
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                return FormValidation.error(String.format("Malformed URL (%s)", url), e);
            }
            return FormValidation.ok();
        }

        /**
         * Checks if the user name and password are valid.
         */
        @RequirePOST
        public FormValidation doValidate(
                @QueryParameter String url,
                @QueryParameter String credentialsId,
                @QueryParameter String groupVisibility,
                @QueryParameter String roleVisibility,
                @QueryParameter boolean useHTTPAuth,
                @QueryParameter String alternativeUrl,
                @QueryParameter int timeout,
                @QueryParameter int readTimeout,
                @QueryParameter int threadExecutorNumber,
                @QueryParameter boolean useBearerAuth,
                @AncestorInPath Item item) {

            if (item == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            } else {
                item.checkPermission(Item.CONFIGURE);
            }

            url = Util.fixEmpty(url);
            alternativeUrl = Util.fixEmpty(alternativeUrl);
            URL mainURL, alternativeURL = null;

            try {
                if (url == null) {
                    return FormValidation.error("No URL given");
                }
                mainURL = new URL(url);
            } catch (MalformedURLException e) {
                return FormValidation.error(String.format("Malformed URL (%s)", url), e);
            }

            try {
                if (alternativeUrl != null) {
                    alternativeURL = new URL(alternativeUrl);
                }
            } catch (MalformedURLException e) {
                return FormValidation.error(String.format("Malformed alternative URL (%s)", alternativeUrl), e);
            }

            credentialsId = Util.fixEmpty(credentialsId);
            JiraSite site = getBuilder()
                    .withMainURL(mainURL)
                    .withAlternativeURL(alternativeURL)
                    .withCredentialsId(credentialsId)
                    .withGroupVisibility(groupVisibility)
                    .withRoleVisibility(roleVisibility)
                    .withUseHTTPAuth(useHTTPAuth)
                    .build();

            if (threadExecutorNumber < 1) {
                return FormValidation.error(Messages.JiraSite_threadExecutorMinimunSize("1"));
            }
            if (timeout < 0) {
                return FormValidation.error(Messages.JiraSite_timeoutMinimunValue("1"));
            }
            if (readTimeout < 0) {
                return FormValidation.error(Messages.JiraSite_readTimeoutMinimunValue("1"));
            }

            site.setTimeout(timeout);
            site.setReadTimeout(readTimeout);
            site.setThreadExecutorNumber(threadExecutorNumber);
            site.setUseBearerAuth(useBearerAuth);
            try {
                JiraSession session = site.getSession(item, true);
                if (session == null) {
                    return FormValidation.error("Cannot validate configuration");
                }
                session.getMyPermissions();
                return FormValidation.ok("Success");
            } catch (RestClientException e) {
                LOGGER.log(Level.WARNING, "Failed to login to Jira at " + url, e);
            } finally {
                if (site != null) {
                    site.destroy();
                }
            }

            return FormValidation.error("Failed to login to Jira");
        }

        @SuppressWarnings("unused") // Used by stapler
        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath final Item item,
                @QueryParameter final String credentialsId,
                @QueryParameter final String url) {
            return CredentialsHelper.doFillCredentialsIdItems(item, credentialsId, url);
        }

        @SuppressWarnings("unused") // Used by stapler
        public FormValidation doCheckCredentialsId(
                @AncestorInPath final Item item, @QueryParameter final String value, @QueryParameter final String url) {
            return CredentialsHelper.doCheckFillCredentialsId(item, value, url);
        }

        Builder getBuilder() {
            return new Builder();
        }
    }

    static class Builder {
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

        public Builder withMainURL(URL mainURL) {
            this.mainURL = mainURL;
            return this;
        }

        public Builder withAlternativeURL(URL alternativeURL) {
            this.alternativeURL = alternativeURL;
            return this;
        }

        public Builder withCredentialsId(String credentialsId) {
            this.credentialsId = credentialsId;
            return this;
        }

        public Builder withSupportsWikiStyleComment(boolean supportsWikiStyleComment) {
            this.supportsWikiStyleComment = supportsWikiStyleComment;
            return this;
        }

        public Builder withRecordScmChanges(boolean recordScmChanges) {
            this.recordScmChanges = recordScmChanges;
            return this;
        }

        public Builder withUserPattern(String userPattern) {
            this.userPattern = userPattern;
            return this;
        }

        public Builder withUpdateJiraIssueForAllStatus(boolean updateJiraIssueForAllStatus) {
            this.updateJiraIssueForAllStatus = updateJiraIssueForAllStatus;
            return this;
        }

        public Builder withGroupVisibility(String groupVisibility) {
            this.groupVisibility = groupVisibility;
            return this;
        }

        public Builder withRoleVisibility(String roleVisibility) {
            this.roleVisibility = roleVisibility;
            return this;
        }

        public Builder withUseHTTPAuth(boolean useHTTPAuth) {
            this.useHTTPAuth = useHTTPAuth;
            return this;
        }

        public JiraSite build() {
            return new JiraSite(
                    mainURL,
                    alternativeURL,
                    credentialsId,
                    supportsWikiStyleComment,
                    recordScmChanges,
                    userPattern,
                    updateJiraIssueForAllStatus,
                    groupVisibility,
                    roleVisibility,
                    useHTTPAuth);
        }
    }

    // helper methods
    // yes this class hierarchy can be a real big mess...

    /**
     * @param item the Jenkins {@link Item} can be a {@link Job} or {@link Folder}
     * @return the parent as {@link ItemGroup} which can be {@link Jenkins} or {@link Folder}
     */
    public static ItemGroup map(Item item) {
        ItemGroup parent = null;
        if (item != null) {
            parent = item instanceof Folder ? (Folder) item : item.getParent();
        }
        return parent;
    }

    /**
     * Creates automatically jiraSession for each jiraSite found
     */
    public static List<JiraSite> getJiraSites(Item item) {
        ItemGroup itemGroup = JiraSite.map(item);
        List<JiraSite> sites = (itemGroup instanceof Folder)
                ? getSitesFromFolders(itemGroup)
                : JiraGlobalConfiguration.get().getSites();
        sites.stream().forEach(jiraSite -> jiraSite.getSession(item));
        return sites;
    }

    /**
     * Creates automatically jiraSession for each jiraSite found
     */
    public static List<JiraSite> getSitesFromFolders(ItemGroup itemGroup) {
        List<JiraSite> result = new ArrayList<>();
        while (itemGroup instanceof AbstractFolder<?>) {
            AbstractFolder<?> folder = (AbstractFolder<?>) itemGroup;
            JiraFolderProperty jiraFolderProperty = folder.getProperties().get(JiraFolderProperty.class);
            if (jiraFolderProperty != null && jiraFolderProperty.getSites().length != 0) {
                List<JiraSite> sites = Arrays.asList(jiraFolderProperty.getSites());
                // setup session for each so it's ready to use
                sites.forEach(jiraSite -> jiraSite.getSession(folder));
                result.addAll(sites);
            }
            itemGroup = folder.getParent();
        }
        return result;
    }

    /**
     * Gets the effective {@link JiraSite} associated with the given project
     * and creates automatically jiraSession for each jiraSite found
     *
     * @return <code>null</code> if no such was found.
     */
    @Nullable
    public static JiraSite get(Job<?, ?> p) {
        JiraSite found = null;
        if (p != null) {
            JiraProjectProperty jpp = p.getProperty(JiraProjectProperty.class);
            if (jpp != null) {
                // Looks in global configuration for the site configured
                JiraSite site = jpp.getSite();
                if (site != null) {
                    found = site;
                }
            }
        }

        if (found == null && p != null) {
            // Check up the folder chain if a site is defined there
            // This only supports one site per folder
            List<JiraSite> sitesFromFolders = getSitesFromFolders(p.getParent());
            if (sitesFromFolders.size() > 0) {
                found = sitesFromFolders.get(0);
            }
        }
        if (found == null) {
            // none is explicitly configured. try the default ---
            // if only one is configured, that must be it.
            List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
            if (sites != null && sites.size() == 1) {
                found = sites.get(0);
            }
        }
        if (found != null) {
            // we create the session here
            found.getSession(p);
        }
        return found;
    }
}
