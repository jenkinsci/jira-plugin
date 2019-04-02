package hudson.plugins.jira;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

/**
 * Helper class for vary credentials operations.
 *
 * @author Zhenlei Huang
 */
public class CredentialsHelper {
	private static final Logger LOGGER = Logger.getLogger(CredentialsHelper.class.getName());

	@CheckForNull
	public static StandardUsernamePasswordCredentials lookupSystemCredentials(@CheckForNull String credentialsId, @CheckForNull URL url) {
		if (credentialsId == null) {
			return null;
		}
		return CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(
						StandardUsernamePasswordCredentials.class,
						Jenkins.getInstance(),
						ACL.SYSTEM,
						URIRequirementBuilder.fromUri(url != null ? url.toExternalForm() : null).build()
				),
				CredentialsMatchers.withId(credentialsId)
		);
	}

	public static StandardUsernamePasswordCredentials migrateCredentials(@Nonnull String username, String password, @CheckForNull URL url) {
		List<StandardUsernamePasswordCredentials> credentials = CredentialsMatchers.filter(
				CredentialsProvider.lookupCredentials(
						StandardUsernamePasswordCredentials.class,
						Jenkins.getInstance(),
						ACL.SYSTEM,
						URIRequirementBuilder.fromUri(url != null ? url.toExternalForm() : null).build()
				),
				CredentialsMatchers.withUsername(username)
		);
		for (StandardUsernamePasswordCredentials c : credentials) {
			if (StringUtils.equals(password, Secret.toString(c.getPassword()))) {
				return c; // found
			}
		}

		// Create new credentials with the principal and secret if we couldn't find any existing credentials
		StandardUsernamePasswordCredentials newCredentials = new UsernamePasswordCredentialsImpl(
				CredentialsScope.SYSTEM,
				null,
				"Migrated by JIRA Plugin",
				username,
				password
		);
		SystemCredentialsProvider.getInstance().getCredentials().add(newCredentials);
		try {
			SystemCredentialsProvider.getInstance().save();
			LOGGER.log(Level.INFO, "Provided username and password were successfully migrated and stored as {0}", newCredentials.getId());
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to store migrated credentials", e);
		}

		return newCredentials;
	}

}
