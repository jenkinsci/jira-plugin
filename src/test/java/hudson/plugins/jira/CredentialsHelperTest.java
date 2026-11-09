package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Descriptor.FormException;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Zhenlei Huang
 */
@WithJenkins
class CredentialsHelperTest {

    @Test
    void lookupSystemCredentials(JenkinsRule r) throws IOException, FormException {
        assertNull(CredentialsHelper.lookupSystemCredentials("nonexistent-credentials-id", null));

        StandardUsernamePasswordCredentials c =
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, "username", "password");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), c);

        assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), null));
        assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), new URL("http://example.org")));
    }

    @Test
    void lookupSystemCredentialsWithDomainRestriction(JenkinsRule r) throws IOException, FormException {
        Domain domain = new Domain(
                "example",
                "test domain",
                Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c =
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, "username", "password");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, c);

        assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), null));
        assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), new URL("http://example.org")));
        assertNull(CredentialsHelper.lookupSystemCredentials(c.getId(), new URL("http://nonexistent.url")));
    }

    @Test
    void migrateCredentials(JenkinsRule r) throws MalformedURLException, FormException {
        assertThat(
                CredentialsProvider.lookupStores(r.jenkins).iterator().next().getCredentials(Domain.global()), empty());

        StandardUsernamePasswordCredentials c =
                CredentialsHelper.migrateCredentials("username", "password", new URL("http://example.org"));

        assertEquals("Migrated by Jira Plugin", c.getDescription());
        assertThat(
                CredentialsProvider.lookupStores(r.jenkins).iterator().next().getCredentials(Domain.global()),
                hasSize(1));
    }

    @Test
    void migrateCredentialsWithExsitingCredentials(JenkinsRule r) throws IOException, FormException {
        Domain domain = new Domain(
                "example",
                "test domain",
                Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
        StandardUsernamePasswordCredentials c =
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null, null, "username", "password");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, c);

        StandardUsernamePasswordCredentials cred =
                CredentialsHelper.migrateCredentials("username", "password", new URL("http://example.org"));

        assertEquals(c, cred);
    }

    @Test
    void doCheckFillCredentialsIdReturnsErrorForUnknownCredentialsId(JenkinsRule r) {
        FormValidation validation =
                CredentialsHelper.doCheckFillCredentialsId(null, "unknown-id", "http://example.org");
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }

    @Test
    void credentialsOperationsWithQueueTaskItem(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject();

        FormValidation validation =
                CredentialsHelper.doCheckFillCredentialsId(project, "unknown-id", "http://example.org");
        assertEquals(FormValidation.Kind.ERROR, validation.kind);

        Optional<StandardUsernamePasswordCredentials> notFound =
                CredentialsHelper.findCredentials(project, "unknown-id", "http://example.org");
        assertTrue(notFound.isEmpty());

        StandardUsernamePasswordCredentials c =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "cred-id", null, "username", "password");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), c);

        Optional<StandardUsernamePasswordCredentials> found =
                CredentialsHelper.findCredentials(project, "cred-id", "http://example.org");
        assertTrue(found.isPresent());
        assertEquals(c, found.get());

        ListBoxModel model = CredentialsHelper.doFillCredentialsIdItems(project, "cred-id", "http://example.org");
        assertThat(model.size(), is(greaterThan(0)));
    }
}
