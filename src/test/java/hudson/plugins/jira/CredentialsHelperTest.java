package hudson.plugins.jira;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author Zhenlei Huang
 */
public class CredentialsHelperTest {
	@Rule
	public JenkinsRule r = new JenkinsRule();

	@Test
	public void testLookupSystemCredentials() throws IOException {
		assertNull(CredentialsHelper.lookupSystemCredentials("nonexistent-credentials-id", null));

		StandardUsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
				CredentialsScope.SYSTEM,
				null,
				null,
				"username",
				"password"
		);
		CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), c);

		assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), null));
		assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), new URL("http://example.org")));
	}

	@Test
	public void testLookupSystemCredentialsWithDomainRestriction() throws IOException {
		Domain domain = new Domain("example", "test domain", Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
		StandardUsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
				CredentialsScope.SYSTEM,
				null,
				null,
				"username",
				"password"
		);
		CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, c);

		assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), null));
		assertEquals(c, CredentialsHelper.lookupSystemCredentials(c.getId(), new URL("http://example.org")));
		assertNull(CredentialsHelper.lookupSystemCredentials(c.getId(), new URL("http://nonexistent.url")));
	}

	@Test
	public void testMigrateCredentials() throws MalformedURLException {
		assertThat(CredentialsProvider.lookupStores(r.jenkins).iterator().next().getCredentials(Domain.global()), empty());

		StandardUsernamePasswordCredentials c = CredentialsHelper.migrateCredentials("username", "password", new URL("http://example.org"));

		assertEquals("Migrated by JIRA Plugin", c.getDescription());
		assertThat(CredentialsProvider.lookupStores(r.jenkins).iterator().next().getCredentials(Domain.global()), hasSize(1));
	}

	@Test
	public void testMigrateCredentialsWithExsitingCredentials() throws IOException {
		Domain domain = new Domain("example", "test domain", Arrays.<DomainSpecification>asList(new HostnameSpecification("example.org", null)));
		StandardUsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
				CredentialsScope.SYSTEM,
				null,
				null,
				"username",
				"password"
		);
		CredentialsProvider.lookupStores(r.jenkins).iterator().next().addDomain(domain, c);

		StandardUsernamePasswordCredentials cred = CredentialsHelper.migrateCredentials("username", "password", new URL("http://example.org"));

		assertEquals(c, cred);
	}
}
