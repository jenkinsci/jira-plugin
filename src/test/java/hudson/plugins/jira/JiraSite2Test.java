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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JiraSite2Test {

    private static final String ANY_USER = "Kohsuke";
    private static final String ANY_PASSWORD = "Kawaguchi";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private URL validPrimaryUrl;

    private URL exampleOrg;

    @Before
    public void init() throws MalformedURLException {
        validPrimaryUrl = new URL("https://nonexistent.url");
        exampleOrg = new URL("https://example.org/");
    }

    @Test
    public void createSessionWithProvidedCredentials() {
        JiraSite site = new JiraSite(validPrimaryUrl, null,
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
        JiraSite site = new JiraSite(validPrimaryUrl, null,
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
    public void deserializeMigrateCredentials() throws MalformedURLException {
        JiraSiteOld old = new JiraSiteOld(validPrimaryUrl, null,
                ANY_USER, ANY_PASSWORD,
                false, false,
                null, false, null,
                null, true);

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(old);
        // trick to get old version config of JiraSite
        xml = xml.replace(this.getClass().getName() + "_-" + JiraSiteOld.class.getSimpleName(), JiraSite.class.getName());

        assertThat(xml, containsString(validPrimaryUrl.toExternalForm()));
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
    public void deserializeNormal() throws IOException {
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
    public void deserializeWithoutCredentials() {
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
        JiraSite site = new JiraSite(validPrimaryUrl, exampleOrg,
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
        JiraSite jiraSite = new JiraSite(validPrimaryUrl.toExternalForm());
        jiraSite.setAlternativeUrl(exampleOrg.toExternalForm());
        assertTrue(jiraSite.getUrl().toExternalForm().endsWith("/"));
        assertTrue(jiraSite.getAlternativeUrl().toExternalForm().endsWith("/"));
        URL url1 = JiraSite.toURL(validPrimaryUrl.toExternalForm());
        URL url2 = JiraSite.toURL(exampleOrg.toExternalForm());
        assertTrue(url1.toExternalForm().endsWith("/"));
        assertTrue(url2.toExternalForm().endsWith("/"));
    }

    @Test
    @WithoutJenkins
    public void urlNulls() {
        JiraSite jiraSite = new JiraSite(validPrimaryUrl.toExternalForm());
        jiraSite.setAlternativeUrl(" ");
        assertNotNull(jiraSite.getUrl());
        assertNull(jiraSite.getAlternativeUrl());
    }

    @Test
    @WithoutJenkins
    public void toUrlConvertsEmptyStringToNull() {
        URL emptyString = JiraSite.toURL("");
        assertNull(emptyString);
    }

    @Test
    @WithoutJenkins
    public void toUrlConvertsOnlyWhitespaceToNull() {
        URL whitespace = JiraSite.toURL(" ");
        assertNull(whitespace);
    }

    @WithoutJenkins
    @Test(expected = AssertionError.class)
    public void ensureMainUrlIsMandatory() {
        new JiraSite("");
    }

    @Test
    @WithoutJenkins
    public void ensureAlternativeUrlIsNotMandatory() {
        JiraSite jiraSite = new JiraSite(validPrimaryUrl.toExternalForm());
        jiraSite.setAlternativeUrl("");
        assertNull(jiraSite.getAlternativeUrl());
    }

    @WithoutJenkins
    @Test(expected = AssertionError.class)
    public void malformedUrl() {
        new JiraSite("malformed.url");
    }

    @WithoutJenkins
    @Test(expected = AssertionError.class)
    public void malformedAlternativeUrl() {
        JiraSite jiraSite = new JiraSite(validPrimaryUrl.toExternalForm());
        jiraSite.setAlternativeUrl("malformed.url");
    }

    @Test
    @WithoutJenkins
    public void credentialsAreNullByDefault() {
        JiraSite jiraSite = new JiraSite(exampleOrg.toExternalForm());
        jiraSite.setCredentialsId("");
        assertNull(jiraSite.getCredentialsId());
        assertNull(jiraSite.credentials);
    }

    @Test
    public void credentials() throws Exception {
        JiraSite jiraSite = new JiraSite(exampleOrg.toExternalForm());
        String cred = "cred-1";
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
        assertEquals(credentials.getUsername(), jiraSite.credentials.getUsername());
        assertEquals(credentials.getPassword(), jiraSite.credentials.getPassword());
    }
}
