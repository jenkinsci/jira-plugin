package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Descriptor.FormException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
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
}
