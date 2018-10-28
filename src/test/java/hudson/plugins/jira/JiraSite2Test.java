package hudson.plugins.jira;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.util.Secret;
import hudson.util.XStream2;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JiraSite2Test {

    private static final String ANY_USER = "Kohsuke";
    private static final String ANY_PASSWORD = "Kawaguchi";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private URL nonExistentUrl;

    private URL exampleOrg;

    @Before
    public void init() throws MalformedURLException {
        nonExistentUrl = new URL("https://nonexistent.url");
        exampleOrg = new URL("https://example.org/");
    }

    @Test
    public void createSessionWithProvidedCredentials() {
        JiraSite site = new JiraSite(nonExistentUrl, null,
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, ANY_USER, ANY_PASSWORD),
                false, false,
                null, false, null,
                null, true);
        site.setTimeout(1);
        JiraSession session = site.getSession();
        assertNotNull(session);
        assertEquals(session, site.getSession());
    }

    @Test
    public void createSessionReturnsNullIfCredentialsIsNull() {
        JiraSite site = new JiraSite(nonExistentUrl, null,
                (StandardUsernamePasswordCredentials)null,
                false, false,
                null, false, null,
                null, true);
        site.setTimeout(1);
        JiraSession session = site.getSession();
        assertEquals(session, site.getSession());
        assertNull(session);
    }

    @Test
    public void testDeserializeMigrateCredentials() throws MalformedURLException {
        JiraSiteOld old = new JiraSiteOld(nonExistentUrl, null,
                ANY_USER, ANY_PASSWORD,
                false, false,
                null, false, null,
                null, true);

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(old);
        // trick to get old version config of JiraSite
        xml = xml.replace(this.getClass().getName() + "_-" + JiraSiteOld.class.getSimpleName(), JiraSite.class.getName());

        assertThat(xml, containsString(nonExistentUrl.toExternalForm()));
        assertThat(xml, containsString("userName"));
        assertThat(xml, containsString("password"));
        assertThat(xml, not(containsString("credentialsId")));
        assertThat(CredentialsProvider.lookupStores(j.jenkins).iterator().next().getCredentials(Domain.global()), empty());

        JiraSite site = (JiraSite)xStream2.fromXML(xml);

        assertNotNull(site);
        assertNotNull(site.credentials);
        assertNotNull(site.credentialsId);
        assertEquals(CredentialsHelper.lookupSystemCredentials(site.credentialsId, null), site.credentials);
        assertEquals(ANY_USER, site.credentials.getUsername());
        assertEquals(ANY_PASSWORD, site.credentials.getPassword().getPlainText());
        assertThat(CredentialsProvider.lookupStores(j.jenkins).iterator().next().getCredentials(Domain.global()), hasItem(site.credentials));
    }

    @Test
    public void testDeserializeNormal() throws IOException {
        Domain domain = new Domain("example", "test domain", Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                null,
                null,
                ANY_USER,
                ANY_PASSWORD
        );
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addDomain(domain, c);

        JiraSite site = new JiraSite(exampleOrg, null,
                c.getId(),
                false, false,
                null, false, null,
                null, true);

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(site);

        assertThat(xml, not(containsString("userName")));
        assertThat(xml, not(containsString("password")));
        assertThat(xml, containsString("credentialsId"));

        JiraSite site1 = (JiraSite)xStream2.fromXML(xml);

        assertNotNull(site1.credentials);
        assertNotNull(site1.credentialsId);
        assertEquals(c, site1.credentials);
    }

    @WithoutJenkins
    @Test
    public void testDeserializeWithoutCredentials() {
        JiraSite site = new JiraSite(exampleOrg, null,
                (String)null,
                false, false,
                null, false, null,
                null, true);

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(site);

        assertThat(xml, not(containsString("credentialsId")));

        JiraSite site1 = (JiraSite)xStream2.fromXML(xml);

        assertNotNull(site1.url);
        assertEquals(exampleOrg, site1.url);
        assertNull(site1.credentials);
        assertNull(site1.credentialsId);
    }

    private static class JiraSiteOld extends JiraSite {
        public String userName;
        public Secret password;

        JiraSiteOld(URL url, URL alternativeUrl, String userName, String password, boolean supportsWikiStyleComment, boolean recordScmChanges, String userPattern,
                    boolean updateJiraIssueForAllStatus, String groupVisibility, String roleVisibility, boolean useHTTPAuth) {
            super(url, alternativeUrl, (StandardUsernamePasswordCredentials)null, supportsWikiStyleComment, recordScmChanges, userPattern,
                    updateJiraIssueForAllStatus, groupVisibility, roleVisibility, useHTTPAuth);
            this.userName = userName;
            this.password = Secret.fromString(password);
        }
    }

    @Test
    @WithoutJenkins
    public void alternativeURLNotNull() {
        JiraSite site = new JiraSite(nonExistentUrl, exampleOrg,
            (StandardUsernamePasswordCredentials) null,
            false, false,
            null, false, null,
            null, true);
        assertNotNull(site.getAlternativeUrl());
        assertEquals(exampleOrg, site.getAlternativeUrl());
    }

    @Test
    @WithoutJenkins
    public void ensureUrlEndsWithSlash() {
        JiraSite jiraSite = new JiraSite(nonExistentUrl.toExternalForm());
        jiraSite.setAlternativeUrl(exampleOrg.toExternalForm());
        assertTrue(jiraSite.getUrl().toExternalForm().endsWith("/"));
        assertTrue(jiraSite.getAlternativeUrl().toExternalForm().endsWith("/"));
        URL url1 = JiraSite.toURL(nonExistentUrl.toExternalForm());
        URL url2 = JiraSite.toURL(exampleOrg.toExternalForm());
        assertTrue(url1.toExternalForm().endsWith("/"));
        assertTrue(url2.toExternalForm().endsWith("/"));
    }

    @Test
    @WithoutJenkins
    public void urlNulls() {
        JiraSite jiraSite = new JiraSite(nonExistentUrl.toExternalForm());
        jiraSite.setAlternativeUrl(" ");
        URL url1 = JiraSite.toURL("");
        URL url2 = JiraSite.toURL(" ");
        assertNotNull(jiraSite.getUrl());
        assertNull(jiraSite.getAlternativeUrl());
        assertNull(url1);
        assertNull(url2);
    }

    @Test
    @WithoutJenkins
    public void ensureMainUrlIsMandatory() {
        thrown.expect(AssertionError.class);
        thrown.expectMessage("URL cannot be null");
        new JiraSite("");
    }

    @Test
    @WithoutJenkins
    public void ensureAlternativeUrlIsNotMandatory() {
        JiraSite jiraSite = new JiraSite(nonExistentUrl.toExternalForm());
        jiraSite.setAlternativeUrl("");
        assertNull(jiraSite.getAlternativeUrl());
    }

    @Test
    @WithoutJenkins
    public void malformedUrl() {
        thrown.expect(AssertionError.class);
        thrown.expectMessage("java.net.MalformedURLException: no protocol");
        new JiraSite("malformed.url");
    }

    @Test
    @WithoutJenkins
    public void malformedAlternativeUrl() {
        JiraSite jiraSite = new JiraSite(nonExistentUrl.toExternalForm());
        thrown.expect(AssertionError.class);
        thrown.expectMessage("java.net.MalformedURLException: no protocol");
        jiraSite.setAlternativeUrl("malformed.url");
    }

    @Test
    public void credentials() throws Exception {
        JiraSite jiraSite = new JiraSite(exampleOrg.toExternalForm());
        jiraSite.setCredentialsId("");
        assertNull(jiraSite.getCredentialsId());
        assertNull(jiraSite.credentials);
        String cred = "cred-1-id";
        String user = "user1";
        String pwd = "pwd1";

        UsernamePasswordCredentialsImpl credentials = new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL, cred, null, user, pwd);

        SystemCredentialsProvider systemProvider = SystemCredentialsProvider.getInstance();
        systemProvider.getCredentials().add(credentials);
        systemProvider.save();
        jiraSite.setCredentialsId(cred);
        assertNotNull(jiraSite.getCredentialsId());
        assertNotNull(jiraSite.credentials);
        assertEquals(cred, jiraSite.getCredentialsId());
        assertEquals(credentials.getUsername(), jiraSite.credentials.getUsername());
        assertEquals(credentials.getPassword(), jiraSite.credentials.getPassword());
    }

    @Test
    @WithoutJenkins
    public void gettersAndSetters() throws Exception {
        JiraSite jiraSite = new JiraSite(nonExistentUrl.toExternalForm());
        jiraSite.setAlternativeUrl(exampleOrg.toExternalForm());
        assertFalse(jiraSite.getDisableChangelogAnnotations());
        assertEquals(JiraSite.DEFAULT_TIMEOUT, jiraSite.getTimeout());
        assertEquals(JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER, jiraSite.getThreadExecutorNumber());
        assertEquals(JiraSite.DEFAULT_READ_TIMEOUT, jiraSite.getReadTimeout());
        assertFalse(jiraSite.isAppendChangeTimestamp());
        assertNull(jiraSite.getDateTimePattern());
        assertNull(jiraSite.getGroupVisibility());
        assertNull(jiraSite.getRoleVisibility());
        assertFalse(jiraSite.isUseHTTPAuth());
        assertFalse(jiraSite.isSupportsWikiStyleComment());
        assertFalse(jiraSite.isUpdateJiraIssueForAllStatus());
        assertFalse(jiraSite.isRecordScmChanges());
        assertFalse(jiraSite.isUpdateJiraIssueForAllStatus());
        jiraSite.setUseHTTPAuth(true);
        assertTrue(jiraSite.isUseHTTPAuth());
        jiraSite.setSupportsWikiStyleComment(true);
        assertTrue(jiraSite.isSupportsWikiStyleComment());
        jiraSite.setRecordScmChanges(true);
        assertTrue(jiraSite.isRecordScmChanges());
        jiraSite.setUpdateJiraIssueForAllStatus(true);
        assertTrue(jiraSite.isUpdateJiraIssueForAllStatus());
        assertNull(jiraSite.getUserPattern());
        jiraSite.setUserPattern("");
        assertNull(jiraSite.getUserPattern());
        jiraSite.setUserPattern("[a-zA-Z0-9]");
        assertNotNull(jiraSite.getUserPattern());
        String id = "JIRA-1";
        URL url = jiraSite.getUrl(id);
        assertEquals(new URL(nonExistentUrl, "browse/" + id.toUpperCase()), url);
        URL alternativeUrl = jiraSite.getAlternativeUrl(id);
        assertEquals(new URL(exampleOrg, "browse/" + id.toUpperCase()), alternativeUrl);
    }
}
