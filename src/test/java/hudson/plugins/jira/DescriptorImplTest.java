package hudson.plugins.jira;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
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
import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

/**
 * Created by warden on 14.09.15.
 */
public class DescriptorImplTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    JiraSite.DescriptorImpl descriptor = new JiraSite.DescriptorImpl();

    @Test
    public void testDoValidate() throws Exception {
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

        MockFolder dummy = r.createFolder("dummy");
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy as = new MockAuthorizationStrategy();
        as.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        as.grant(Item.READ).onItems(dummy).to("alice");
        as.grant(Item.CONFIGURE).onItems(dummy).to("dev");
        r.jenkins.setAuthorizationStrategy(as);

        Domain domain = new Domain("example", "test domain", Arrays.asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c1 = new UsernamePasswordCredentialsImpl(
            CredentialsScope.SYSTEM,
            null,
            null,
            "username",
            "password"
        );
        CredentialsStore credentialsStore = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        credentialsStore.addDomain(domain, c1);

        StandardUsernamePasswordCredentials c2 = new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL,
            null,
            null,
            "uid",
            "pwd"
        );

        credentialsStore = CredentialsProvider.lookupStores(dummy).iterator().next();
        credentialsStore.addCredentials( domain, c2 );

        JiraSite.DescriptorImpl descriptor = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class);

        try (ACLContext ignored = ACL.as(User.get("admin"))) {
            ListBoxModel options = descriptor.doFillCredentialsIdItems(null,null, "http://example.org");
            assertThat(options.toString(), options, hasSize(3));
            assertTrue(options.toString(), listBoxModelContainsName(options,CredentialsNameProvider.name(c1)));

            options = descriptor.doFillCredentialsIdItems(null, null, "http://nonexistent.url");
            assertThat(options.toString(), options, hasSize(1));
            assertEquals("", options.get(0).value);

            options = descriptor.doFillCredentialsIdItems(dummy, null, "http://example.org");
            assertThat(options.toString(), options, hasSize(2));
            assertTrue(options.toString(), listBoxModelContainsName(options,CredentialsNameProvider.name(c2)));
        }

        try (ACLContext ignored = ACL.as(User.get("alice"))) {
            ListBoxModel options = descriptor.doFillCredentialsIdItems(dummy, null,"http://example.org");
            assertThat(options.toString(), options, hasSize(1));
        }
        try (ACLContext ignored = ACL.as(User.get("dev"))) {
            ListBoxModel options = descriptor.doFillCredentialsIdItems(dummy, null,"http://example.org");
            assertThat(options.toString(), options, hasSize(2));
        }
    }

    private boolean listBoxModelContainsName(ListBoxModel options, String name) {
        return options.stream().filter( option -> name.equals(option.name) ).findFirst().isPresent();
    }

}
