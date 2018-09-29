package hudson.plugins.jira;

import java.io.IOException;
import java.util.Arrays;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
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
import org.jvnet.hudson.test.WithoutJenkins;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
    public void testDoFillCredentialsIdItems() throws IOException {
        Domain domain = new Domain("example", "test domain", Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                null,
                null,
                "username",
                "password"
        );
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, c);

        MockFolder dummy = r.createFolder("dummy");
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

}
