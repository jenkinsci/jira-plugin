package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by warden on 14.09.15.
 */
public class DescriptorImplTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    JiraSite.DescriptorImpl descriptor = new JiraSite.DescriptorImpl();

    @Test
    public void doValidate() throws Exception {
        FormValidation validation = descriptor.doValidate(null, null, null, null,
                                                          false, null,
                                                          JiraSite.DEFAULT_TIMEOUT, JiraSite.DEFAULT_READ_TIMEOUT, JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                                                          r.createFreeStyleProject());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);

        validation = descriptor.doValidate("invalid", null, null, null,
                                           false, null,
                                           JiraSite.DEFAULT_TIMEOUT, JiraSite.DEFAULT_READ_TIMEOUT, JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                                           r.createFreeStyleProject());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);

        validation = descriptor.doValidate("http://valid/", null, null, null,
                                           false, "invalid",
                                           JiraSite.DEFAULT_TIMEOUT, JiraSite.DEFAULT_READ_TIMEOUT, JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                                           r.createFreeStyleProject());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);

        validation = descriptor.doValidate("http://valid/", null, null, null,
                                           false, " ",
                                           JiraSite.DEFAULT_TIMEOUT, JiraSite.DEFAULT_READ_TIMEOUT, JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                                           r.createFreeStyleProject());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }

    @Test
    public void doFillCredentialsIdItems() throws IOException {
        Domain domain = new Domain("example", "test domain", Arrays.asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                "id1",
                null,
                "username",
                "password"
        );
        MockFolder dummy = r.createFolder("dummy");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, c);
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy as = new MockAuthorizationStrategy();
        as.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        as.grant(Item.READ).onItems(dummy).to("alice");
        r.jenkins.setAuthorizationStrategy(as);

        try (ACLContext ignored = ACL.as(User.get("admin"))) {
            ListBoxModel options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(null, "http://example.org");
            assertThat(options, hasSize(2));
            assertEquals(CredentialsNameProvider.name(c), options.get(1).name);

            options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(null, "http://nonexistent.url");
            assertThat(options, hasSize(1));
            assertEquals("", options.get(0).value);

            options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(dummy, "http://example.org");
            assertThat(options, hasSize(2));
            assertEquals(CredentialsNameProvider.name(c), options.get(1).name);
        }

        try (ACLContext ignored = ACL.as(User.get("alice"))) {
            ListBoxModel options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(dummy, "http://example.org");
            assertThat(options, empty());
        }
    }
    
    @Test
    public void doFillFolderCredentialsIdItems() throws Exception {
    	String jiraSiteUrl = "https://jirasite.org";
        Domain domain = new Domain("example", "test domain", Arrays.asList(new HostnameSpecification("jirasite.org", null)));
        
        //Global config
        StandardUsernamePasswordCredentials globalCredential = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                "global",
                null,
                "username1",
                "password1"
        );
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, globalCredential);
        
        //Folder 1 config
        StandardUsernamePasswordCredentials f1Credential = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                "folder1",
                null,
                "username2",
                "password2"
        );
        Folder f1 = r.createProject(Folder.class, "folder1");
        List<JiraSite> f1SitesList = new ArrayList<>();
        f1SitesList.add(new JiraSite(jiraSiteUrl));
        JiraFolderProperty f1Property = new JiraFolderProperty();
        f1Property.setSites(f1SitesList);
        f1.getProperties().add(f1Property);
        r.configRoundtrip(f1);
        assertTrue(CredentialsProvider.hasStores(f1));
        CredentialsProvider.lookupStores(r.jenkins.getItem("folder1")).iterator().next().addDomain(domain, f1Credential);
        
        //Folder 2 config
        StandardUsernamePasswordCredentials f2Credential = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                "folder2",
                null,
                "username2",
                "password2"
        );
        Folder f2 = r.createProject(Folder.class, "folder2");
        List<JiraSite> f2SitesList = new ArrayList<>();
        f2SitesList.add(new JiraSite(jiraSiteUrl));
        JiraFolderProperty f2Property = new JiraFolderProperty();
        f1Property.setSites(f2SitesList);
        f1.getProperties().add(f2Property);
        r.configRoundtrip(f2);
        assertTrue(CredentialsProvider.hasStores(f2));
        CredentialsProvider.lookupStores(r.jenkins.getItem("folder2")).iterator().next().addDomain(domain, f2Credential);
        
        //Wrong domain
        ListBoxModel options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(null, "http://nonexistent.url");
        assertThat(options, hasSize(1));
        assertEquals("", options.get(0).value);
        
        //Without context
        options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(null, jiraSiteUrl);
        assertThat(options, hasSize(2));
        assertEquals(CredentialsNameProvider.name(globalCredential), options.get(1).name);

        //With folder 1 context
        options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(f1, jiraSiteUrl);
        assertThat(options, hasSize(3));
        assertEquals(CredentialsNameProvider.name(globalCredential), options.get(1).name);
        assertEquals(CredentialsNameProvider.name(f1Credential), options.get(2).name);
        
        //With folder 2 context
        options = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class).doFillCredentialsIdItems(f2, jiraSiteUrl);
        assertThat(options, hasSize(3));
        assertEquals(CredentialsNameProvider.name(globalCredential), options.get(1).name);
        assertEquals(CredentialsNameProvider.name(f2Credential), options.get(2).name);
    }

}
