package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Descriptor.FormException;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.plugins.jira.model.JiraIssue;
import hudson.util.DescribableList;
import hudson.util.Secret;
import hudson.util.XStream2;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JiraSiteTest {

    private static final String ANY_USER = "Kohsuke";
    private static final String ANY_PASSWORD = "Kawaguchi";

    private URL validPrimaryUrl;

    private URL exampleOrg;

    @BeforeEach
    void init() throws MalformedURLException {
        validPrimaryUrl = new URL("https://nonexistent.url");
        exampleOrg = new URL("https://example.org/");
    }

    @Test
    void createSessionWithProvidedCredentials(JenkinsRule r) throws FormException {
        JiraSite site = new JiraSite(
                validPrimaryUrl,
                null,
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, ANY_USER, ANY_PASSWORD),
                false,
                false,
                null,
                false,
                null,
                null,
                true);
        site.setTimeout(1);
        JiraSession session = site.getSession(null);
        assertNotNull(session);
        assertEquals(session, site.getSession(null));
    }

    @Test
    @Issue("JENKINS-64083")
    void createSessionWithGlobalCredentials(JenkinsRule r) throws FormException {
        JiraSite site = new JiraSite(
                validPrimaryUrl,
                null,
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, ANY_USER, ANY_PASSWORD),
                false,
                false,
                null,
                false,
                null,
                null,
                true);
        site.setTimeout(1);
        JiraSession session = site.getSession(mock(Job.class));
        assertNotNull(session);
        assertEquals(session, site.getSession(null));
    }

    @Test
    void createSessionReturnsNullIfCredentialsIsNull(JenkinsRule r) throws FormException {
        JiraSite site = new JiraSite(
                validPrimaryUrl,
                null,
                (StandardUsernamePasswordCredentials) null,
                false,
                false,
                null,
                false,
                null,
                null,
                true);
        site.setTimeout(1);
        JiraSession session = site.getSession(null);
        assertEquals(session, site.getSession(null));
        assertNull(session);
    }

    @Test
    void deserializeMigrateCredentials(JenkinsRule j) throws MalformedURLException, FormException {
        JiraSiteOld old = new JiraSiteOld(
                validPrimaryUrl, null, ANY_USER, ANY_PASSWORD, false, false, null, false, null, null, true);

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(old);
        // trick to get old version config of JiraSite
        xml = xml.replace(
                this.getClass().getName() + "_-" + JiraSiteOld.class.getSimpleName(), JiraSite.class.getName());

        assertThat(xml, containsString(validPrimaryUrl.toExternalForm()));
        assertThat(xml, containsString("userName"));
        assertThat(xml, containsString("password"));
        assertThat(xml, not(containsString("credentialsId")));
        assertThat(
                CredentialsProvider.lookupStores(j.jenkins).iterator().next().getCredentials(Domain.global()), empty());

        JiraSite site = (JiraSite) xStream2.fromXML(xml);

        assertNotNull(site);
        assertNotNull(site.credentialsId);
        assertEquals(
                ANY_USER,
                CredentialsHelper.lookupSystemCredentials(site.credentialsId, null)
                        .getUsername());
        assertEquals(
                ANY_PASSWORD,
                CredentialsHelper.lookupSystemCredentials(site.credentialsId, null)
                        .getPassword()
                        .getPlainText());
    }

    @Test
    void deserializeNormal(JenkinsRule j) throws IOException, FormException {
        Domain domain = new Domain(
                "example",
                "test domain",
                Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c =
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, ANY_USER, ANY_PASSWORD);
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addDomain(domain, c);

        JiraSite site = new JiraSite(exampleOrg, null, c.getId(), false, false, null, false, null, null, true);
        site.setUseBearerAuth(true);

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(site);

        assertThat(xml, not(containsString("userName")));
        assertThat(xml, not(containsString("password")));
        assertThat(xml, containsString("credentialsId"));

        JiraSite site1 = (JiraSite) xStream2.fromXML(xml);
        assertNotNull(site1.credentialsId);
        assertTrue(site1.useBearerAuth);
    }

    @WithoutJenkins
    @Test
    void deserializeWithoutCredentials() {
        JiraSite site = new JiraSite(exampleOrg, null, (String) null, false, false, null, false, null, null, true);

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(site);

        assertThat(xml, not(containsString("credentialsId")));

        JiraSite site1 = (JiraSite) xStream2.fromXML(xml);

        assertNotNull(site1.url);
        assertEquals(exampleOrg, site1.url);
        assertNull(site1.credentialsId);
    }

    private static class JiraSiteOld extends JiraSite {
        public String userName;
        public Secret password;

        JiraSiteOld(
                URL url,
                URL alternativeUrl,
                String userName,
                String password,
                boolean supportsWikiStyleComment,
                boolean recordScmChanges,
                String userPattern,
                boolean updateJiraIssueForAllStatus,
                String groupVisibility,
                String roleVisibility,
                boolean useHTTPAuth)
                throws FormException {
            super(
                    url,
                    alternativeUrl,
                    (StandardUsernamePasswordCredentials) null,
                    supportsWikiStyleComment,
                    recordScmChanges,
                    userPattern,
                    updateJiraIssueForAllStatus,
                    groupVisibility,
                    roleVisibility,
                    useHTTPAuth);
            this.userName = userName;
            this.password = Secret.fromString(password);
        }
    }

    @Test
    @WithoutJenkins
    void alternativeURLNotNull() throws FormException {
        JiraSite site = new JiraSite(
                validPrimaryUrl,
                exampleOrg,
                (StandardUsernamePasswordCredentials) null,
                false,
                false,
                null,
                false,
                null,
                null,
                true);
        assertNotNull(site.getAlternativeUrl());
        assertEquals(exampleOrg, site.getAlternativeUrl());
    }

    @Test
    @WithoutJenkins
    void ensureUrlEndsWithSlash() {
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
    void urlNulls() {
        JiraSite jiraSite = new JiraSite(validPrimaryUrl.toExternalForm());
        jiraSite.setAlternativeUrl(" ");
        assertNotNull(jiraSite.getUrl());
        assertNull(jiraSite.getAlternativeUrl());
    }

    @Test
    @WithoutJenkins
    void toUrlConvertsEmptyStringToNull() {
        URL emptyString = JiraSite.toURL("");
        assertNull(emptyString);
    }

    @Test
    @WithoutJenkins
    void toUrlConvertsOnlyWhitespaceToNull() {
        URL whitespace = JiraSite.toURL(" ");
        assertNull(whitespace);
    }

    @WithoutJenkins
    @Test
    void ensureMainUrlIsMandatory() {
        assertThrows(AssertionError.class, () -> {
            new JiraSite("");
        });
    }

    @Test
    @WithoutJenkins
    void ensureAlternativeUrlIsNotMandatory() {
        JiraSite jiraSite = new JiraSite(validPrimaryUrl.toExternalForm());
        jiraSite.setAlternativeUrl("");
        assertNull(jiraSite.getAlternativeUrl());
    }

    @WithoutJenkins
    @Test
    void malformedUrl() {
        assertThrows(AssertionError.class, () -> {
            new JiraSite("malformed.url");
        });
    }

    @WithoutJenkins
    @Test
    void malformedAlternativeUrl() {
        JiraSite jiraSite = new JiraSite(validPrimaryUrl.toExternalForm());
        assertThrows(AssertionError.class, () -> jiraSite.setAlternativeUrl("malformed.url"));
    }

    @Test
    @WithoutJenkins
    void credentialsAreNullByDefault() {
        JiraSite jiraSite = new JiraSite(exampleOrg.toExternalForm());
        jiraSite.setCredentialsId("");
        assertNull(jiraSite.getCredentialsId());
    }

    @Test
    void credentials(JenkinsRule r) throws Exception {
        JiraSite jiraSite = new JiraSite(exampleOrg.toExternalForm());
        String cred = "cred-1";
        String user = "user1";
        String pwd = "pwd1";

        UsernamePasswordCredentialsImpl credentials =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, cred, null, user, pwd);

        SystemCredentialsProvider systemProvider = SystemCredentialsProvider.getInstance();
        systemProvider.getCredentials().add(credentials);
        systemProvider.save();
        jiraSite.setCredentialsId(cred);
        assertNotNull(jiraSite.getCredentialsId());
        assertEquals(
                credentials.getUsername(),
                CredentialsHelper.lookupSystemCredentials(cred, null).getUsername());
        assertEquals(
                credentials.getPassword(),
                CredentialsHelper.lookupSystemCredentials(cred, null).getPassword());
    }

    @Test
    @WithoutJenkins
    void siteAsProjectProperty() throws Exception {
        JiraSite jiraSite = new JiraSite(new URL("https://foo.org/").toExternalForm());
        Job<?, ?> job = mock(Job.class);
        JiraProjectProperty jpp = mock(JiraProjectProperty.class);
        when(job.getProperty(JiraProjectProperty.class)).thenReturn(jpp);
        when(jpp.getSite()).thenReturn(jiraSite);

        assertEquals(jiraSite.getUrl(), JiraSite.get(job).getUrl());
    }

    @Test
    void projectPropertySiteAndParentBothNull(JenkinsRule j) {
        JiraGlobalConfiguration jiraGlobalConfiguration = mock(JiraGlobalConfiguration.class);
        Job<?, ?> job = mock(Job.class);
        JiraProjectProperty jpp = mock(JiraProjectProperty.class);

        when(job.getProperty(JiraProjectProperty.class)).thenReturn(jpp);
        when(jpp.getSite()).thenReturn(null);
        when(job.getParent()).thenReturn(null);
        when(jiraGlobalConfiguration.getSites()).thenReturn(Collections.emptyList());

        assertNull(JiraSite.get(job));
    }

    @Test
    void noProjectProperty(JenkinsRule j) throws Exception {
        JiraGlobalConfiguration.get().setSites(null);
        Job<?, ?> job = j.jenkins.createProject(FreeStyleProject.class, "foo");
        assertNull(JiraSite.get(job));
    }

    @Test
    void noProjectPropertyUpFoldersWithNoProperty(JenkinsRule j) throws Exception {
        JiraGlobalConfiguration jiraGlobalConfiguration = mock(JiraGlobalConfiguration.class);

        Folder folder2 = spy(createFolder(j, null));
        Folder folder1 = spy(createFolder(j, folder2));
        Job<?, ?> job = folder1.createProject(FreeStyleProject.class, "foo");

        DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> folder1Properties =
                spy(new DescribableList(Jenkins.get()));
        DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> folder2Properties =
                spy(new DescribableList(Jenkins.get()));

        doReturn(folder1Properties).when(folder1).getProperties();
        doReturn(folder2Properties).when(folder2).getProperties();
        doReturn(Collections.emptyList()).when(jiraGlobalConfiguration).getSites();

        assertNull(JiraSite.get(job));
    }

    @Test
    void noProjectPropertyFindFolderPropertyWithNullZeroLengthAndValidSites(JenkinsRule j) throws Exception {
        JiraSite jiraSite1 = new JiraSite(new URL("https://example1.org/").toExternalForm());
        JiraSite jiraSite2 = new JiraSite(new URL("https://example2.org/").toExternalForm());

        Folder folder1 = spy(createFolder(j, null));
        Job job = folder1.createProject(FreeStyleProject.class, "foo");
        DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> folder1Properties =
                new DescribableList(Jenkins.get());

        Folder folder2 = spy(createFolder(j, folder1));
        DescribableList<AbstractFolderProperty<?>, AbstractFolderPropertyDescriptor> folder2Properties =
                new DescribableList(Jenkins.get());
        JiraFolderProperty jfp = new JiraFolderProperty();
        jfp.setSites(Arrays.asList(jiraSite2, jiraSite1));
        folder1Properties.add(jfp);
        folder1.addProperty(folder1Properties.get(JiraFolderProperty.class));
        folder2Properties.add(jfp);
        folder2.addProperty(folder2Properties.get(JiraFolderProperty.class));

        assertEquals(jiraSite2.getUrl(), JiraSite.get(job).getUrl());
    }

    @Test
    void siteConfiguredGlobally(JenkinsRule j) throws Exception {
        JiraSite jiraSite = new JiraSite(new URL("https://foo.org/").toExternalForm());
        JiraGlobalConfiguration.get().setSites(Collections.singletonList(jiraSite));
        Job<?, ?> job = mock(Job.class);
        doReturn(null).when(job).getProperty(JiraProjectProperty.class);

        assertEquals(jiraSite.getUrl(), JiraSite.get(job).getUrl());
    }

    @Test
    @WithoutJenkins
    void getIssueWithoutSession() throws Exception {
        JiraSite jiraSite = new JiraSite(new URL("https://foo.org/").toExternalForm());
        // Verify that no session will be created
        assertNull(jiraSite.getSession(null));
        JiraIssue issue = jiraSite.getIssue("JIRA-1235");
        assertNull(issue);
    }

    private Folder createFolder(JenkinsRule j, Folder folder) throws IOException {
        return folder == null
                ? j.jenkins.createProject(
                        Folder.class, "folder" + j.jenkins.getItems().size())
                : folder.createProject(
                        Folder.class, "folder" + j.jenkins.getItems().size());
    }
}
