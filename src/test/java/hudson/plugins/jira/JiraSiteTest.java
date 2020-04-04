package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.util.DescribableList;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JiraSiteTest {

    private static final String ANY_USER = "Kohsuke";
    private static final String ANY_PASSWORD = "Kawaguchi";

    @Rule
    public JenkinsRule r = new JenkinsRule();


    @Test
    public void createSessionReturnsNullIfCredentialsIsNull() {
        JiraSite site = new JiraSite("https://valid.url.com");
        site.setTimeout(1);
        JiraSession session = site.getSession();
        assertEquals(session, site.getSession());
        assertNull(session);
    }

    @Test
    public void deserialization() throws IOException {
        Domain domain = new Domain("example", "test domain", Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                null,
                null,
                ANY_USER,
                ANY_PASSWORD
        );
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, credentials);

        JiraSite origSite = new JiraSite.JiraSiteBuilder()
                .withMainURL(new URL("https://example.com"))
                .withCredentialsId(credentials.getId())
                .build();

        XStream2 xStream2 = new XStream2();
        String xml = xStream2.toXML(origSite);

        assertNotNull(credentials.getId());
        assertThat(xml, not(containsString("userName")));
        assertThat(xml, not(containsString("password")));
        assertThat(xml, containsString("credentialsId"));
        assertThat(xml, containsString(credentials.getId()));
        assertThat(xml, containsString("https://example.com/"));


        JiraSite deserializedSite = (JiraSite)xStream2.fromXML(xml);
        assertNotNull(deserializedSite.credentialsId);
        assertEquals("https://example.com/", deserializedSite.getUrl().toExternalForm());
    }

    @Test
    @WithoutJenkins
    public void testUrlHandling() throws MalformedURLException {
        JiraSite jiraSite = new JiraSite(new URL("http://example.com").toExternalForm());
        jiraSite.setAlternativeUrl(new URL("http://alt.com").toExternalForm());

        assertTrue(jiraSite.getUrl().toExternalForm().endsWith("/"));
        assertTrue(jiraSite.getAlternativeUrl().toExternalForm().endsWith("/"));
    }

    @Test
    @WithoutJenkins
    public void urlNulls() throws MalformedURLException {
        JiraSite jiraSite = new JiraSite(new URL("http://example.com").toExternalForm());
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

    @WithoutJenkins
    @Test(expected = AssertionError.class)
    public void malformedUrl() {
        new JiraSite("malformed.url");
    }

    @WithoutJenkins
    @Test(expected = AssertionError.class)
    public void malformedAlternativeUrl() throws MalformedURLException {
        JiraSite jiraSite = new JiraSite(new URL("https://example.com").toExternalForm());
        jiraSite.setAlternativeUrl("malformed.url");
    }

    @Test
    public void credentials() throws IOException {
        JiraSite jiraSite = new JiraSite(new URL("https://example.com").toExternalForm());
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
        assertEquals(credentials.getUsername(), CredentialsHelper.lookupSystemCredentials(cred, null).getUsername());
        assertEquals(credentials.getPassword(), CredentialsHelper.lookupSystemCredentials(cred, null).getPassword());
    }

    @Test
    @WithoutJenkins
    public void siteAsProjectProperty() throws Exception {
        JiraSite jiraSite = new JiraSite(new URL("https://foo.org/").toExternalForm());
        Job<?, ?> job = mock( Job.class);
        JiraProjectProperty jpp = mock(JiraProjectProperty.class);
        when( job.getProperty(JiraProjectProperty.class)).thenReturn( jpp );
        when( jpp.getSite() ).thenReturn( jiraSite );

        assertEquals( jiraSite.getUrl(), JiraSite.get( job ).getUrl() );
    }

    @Test
    public void projectPropertySiteAndParentBothNull() {
        JiraGlobalConfiguration jiraGlobalConfiguration = mock(JiraGlobalConfiguration.class);
        Job<?, ?> job = mock( Job.class);
        JiraProjectProperty jpp = mock(JiraProjectProperty.class);

        when( job.getProperty(JiraProjectProperty.class)).thenReturn( jpp );
        when( jpp.getSite() ).thenReturn( null );
        when( job.getParent()).thenReturn( null );
        when( jiraGlobalConfiguration.getSites() ).thenReturn( Collections.emptyList() );

        assertNull( JiraSite.get( job ) );
    }

    @Test
    public void noProjectPropertyParentNotFolderAndNotItem() {
        JiraGlobalConfiguration.get().setSites( null );
        Job<?, ?> job = mock( Job.class);
        ItemGroup nonFolderParent = mock( ItemGroup.class );

        when( job.getProperty(JiraProjectProperty.class)).thenReturn( null );
        when( job.getParent()).thenReturn( nonFolderParent );

        assertNull( JiraSite.get( job ) );
    }

    @Test
    public void noProjectPropertyUpFoldersWithNoProperty() {
        JiraGlobalConfiguration jiraGlobalConfiguration = mock(JiraGlobalConfiguration.class);
        Job<?, ?> job = mock( Job.class);
        ItemGroup nonFolderParent = mock( ItemGroup.class );
        AbstractFolder folder1 = mock( AbstractFolder.class );
        DescribableList folder1Properties = mock( DescribableList.class );
        AbstractFolder folder2 = mock( AbstractFolder.class );
        DescribableList folder2Properties = mock( DescribableList.class );

        when( job.getProperty(JiraProjectProperty.class)).thenReturn( null );
        when( job.getParent()).thenReturn( folder1 );
        when( folder1.getProperties() ).thenReturn( folder1Properties );
        when( folder1Properties.get( JiraFolderProperty.class ) ).thenReturn( null );
        when( folder1.getParent() ).thenReturn( folder2 );
        when( folder2.getProperties() ).thenReturn( folder2Properties );
        when( folder2Properties.get( JiraFolderProperty.class ) ).thenReturn( null );
        when( folder2.getParent() ).thenReturn( nonFolderParent );
        when( jiraGlobalConfiguration.getSites() ).thenReturn( Collections.emptyList() );

        assertNull( JiraSite.get( job ) );
    }


    @Test
    public void noProjectPropertyFindFolderPropertyWithNullZeroLengthAndValidSites()  throws Exception {
        JiraSite jiraSite1 = new JiraSite(new URL("https://example1.org/").toExternalForm());
        JiraSite jiraSite2 = new JiraSite(new URL("https://example2.org/").toExternalForm());

        Job<?, ?> job = mock( Job.class);

        AbstractFolder folder1 = mock( AbstractFolder.class );
        DescribableList folder1Properties = mock( DescribableList.class );
        AbstractFolder folder2 = mock( AbstractFolder.class );
        DescribableList folder2Properties = mock( DescribableList.class );
        AbstractFolder folder3 = mock( AbstractFolder.class );
        DescribableList folder3Properties = mock( DescribableList.class );
        JiraFolderProperty jfp = mock(JiraFolderProperty.class);

        when( job.getProperty(JiraProjectProperty.class)).thenReturn( null );
        when( job.getParent()).thenReturn( folder1 );
        when( folder1.getProperties() ).thenReturn( folder1Properties );
        when( folder1Properties.get( JiraFolderProperty.class ) ).thenReturn( jfp );
        when( folder1.getParent() ).thenReturn( folder2 );
        when( folder2.getProperties() ).thenReturn( folder2Properties );
        when( folder2Properties.get( JiraFolderProperty.class ) ).thenReturn( jfp );
        when( folder3.getParent() ).thenReturn( folder3 );
        when( folder3.getProperties() ).thenReturn( folder3Properties );
        when( folder3Properties.get( JiraFolderProperty.class ) ).thenReturn( jfp );
        when( folder3.getParent() ).thenReturn( Jenkins.get() );
        when( jfp.getSites() ).thenReturn( new JiraSite[]{jiraSite2,jiraSite1} );

        assertEquals( jiraSite2.getUrl(), JiraSite.get( job ).getUrl() );
    }

    @Test
    public void siteConfiguredGlobally() throws Exception {
        JiraSite jiraSite = new JiraSite(new URL("https://foo.org/").toExternalForm());
        JiraGlobalConfiguration.get().setSites( Collections.singletonList( jiraSite ) );
        Job<?, ?> job = mock( Job.class);
        ItemGroup nonFolderParent = mock( ItemGroup.class );

        when( job.getProperty(JiraProjectProperty.class)).thenReturn( null );
        when( job.getParent() ).thenReturn( nonFolderParent );

        assertEquals( jiraSite.getUrl(), JiraSite.get(job).getUrl() );
    }

}
