package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Permissions;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

/**
 * Created by warden on 14.09.15.
 */
@WithJenkins
class DescriptorImplTest {

    AbstractBuild build = Mockito.mock(FreeStyleBuild.class);
    Run run = mock(Run.class);
    AbstractProject project = mock(FreeStyleProject.class);

    JiraSite site = mock(JiraSite.class);
    JiraSession session = mock(JiraSession.class);

    JiraSite.DescriptorImpl descriptor = spy(new JiraSite.DescriptorImpl());
    JiraSite.Builder builder = spy(new JiraSite.Builder());

    @Test
    void doFillCredentialsIdItems(JenkinsRule r) throws IOException, FormException {

        MockFolder dummy = r.createFolder("dummy");
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy as = new MockAuthorizationStrategy();
        as.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        as.grant(Item.READ).onItems(dummy).to("alice");
        as.grant(Item.CONFIGURE).onItems(dummy).to("dev");
        r.jenkins.setAuthorizationStrategy(as);

        Domain domain =
                new Domain("example", "test domain", Arrays.asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c1 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, "username", "password");
        CredentialsStore credentialsStore =
                CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        credentialsStore.addDomain(domain, c1);

        StandardUsernamePasswordCredentials c2 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null, "uid", "pwd");

        credentialsStore = CredentialsProvider.lookupStores(dummy).iterator().next();
        credentialsStore.addCredentials(domain, c2);

        JiraSite.DescriptorImpl descriptor = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class);

        try (ACLContext ignored = ACL.as(User.getById("admin", true))) {
            ListBoxModel options = descriptor.doFillCredentialsIdItems(null, null, "http://example.org");
            assertThat(options.toString(), options, hasSize(3));
            assertTrue(listBoxModelContainsName(options, CredentialsNameProvider.name(c1)), options.toString());

            options = descriptor.doFillCredentialsIdItems(null, null, "http://nonexistent.url");
            assertThat(options.toString(), options, hasSize(1));
            assertEquals("", options.get(0).value);

            options = descriptor.doFillCredentialsIdItems(dummy, null, "http://example.org");
            assertThat(options.toString(), options, hasSize(2));
            assertTrue(listBoxModelContainsName(options, CredentialsNameProvider.name(c2)), options.toString());
        }

        try (ACLContext ignored = ACL.as(User.getById("alice", true))) {
            ListBoxModel options = descriptor.doFillCredentialsIdItems(dummy, null, "http://example.org");
            assertThat(options.toString(), options, hasSize(1));
        }
        try (ACLContext ignored = ACL.as(User.getById("dev", true))) {
            ListBoxModel options = descriptor.doFillCredentialsIdItems(dummy, null, "http://example.org");
            assertThat(options.toString(), options, hasSize(2));
        }
    }

    private boolean listBoxModelContainsName(ListBoxModel options, String name) {
        return options.stream()
                .filter(option -> name.equals(option.name))
                .findFirst()
                .isPresent();
    }

    @Test
    void validateFormConnectionErrors(JenkinsRule r) throws Exception {

        builder.withMainURL(new URL("http://test.com"));

        when(descriptor.getBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(site);
        when(build.getParent()).thenReturn(project);
        when(site.getSession(project, true)).thenReturn(session);
        when(session.getMyPermissions()).thenThrow(RestClientException.class);

        FormValidation validation = descriptor.doValidate(
                "http://localhost:8080",
                null,
                null,
                null,
                false,
                null,
                JiraSite.DEFAULT_TIMEOUT,
                JiraSite.DEFAULT_READ_TIMEOUT,
                JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                false,
                project);

        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        verify(site).getSession(project, true);

        validation = descriptor.doValidate(
                "http://localhost:8080",
                null,
                null,
                null,
                false,
                null,
                -1,
                JiraSite.DEFAULT_READ_TIMEOUT,
                JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                false,
                project);
        assertEquals(Messages.JiraSite_timeoutMinimunValue("1"), validation.getLocalizedMessage());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        verify(site).getSession(project, true);

        validation = descriptor.doValidate(
                "http://localhost:8080",
                null,
                null,
                null,
                false,
                null,
                JiraSite.DEFAULT_TIMEOUT,
                -1,
                JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                false,
                project);

        assertEquals(Messages.JiraSite_readTimeoutMinimunValue("1"), validation.getMessage());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        verify(site).getSession(project, true);

        validation = descriptor.doValidate(
                "http://localhost:8080",
                null,
                null,
                null,
                false,
                null,
                JiraSite.DEFAULT_TIMEOUT,
                JiraSite.DEFAULT_READ_TIMEOUT,
                -1,
                false,
                project);
        assertEquals(Messages.JiraSite_threadExecutorMinimunSize("1"), validation.getMessage());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        verify(site).getSession(project, true);
    }

    @Test
    void validateFormConnectionOK(JenkinsRule r) throws Exception {
        builder.withMainURL(new URL("http://test.com"));

        when(descriptor.getBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(site);
        when(site.getSession(project, true)).thenReturn(session);
        when(session.getMyPermissions()).thenReturn(mock(Permissions.class));

        FormValidation validation = descriptor.doValidate(
                "http://localhost:8080",
                null,
                null,
                null,
                false,
                null,
                JiraSite.DEFAULT_TIMEOUT,
                JiraSite.DEFAULT_READ_TIMEOUT,
                JiraSite.DEFAULT_THREAD_EXECUTOR_NUMBER,
                false,
                project);

        verify(builder).build();
        verify(site).getSession(project, true);
        assertEquals(FormValidation.Kind.OK, validation.kind);
    }
}
