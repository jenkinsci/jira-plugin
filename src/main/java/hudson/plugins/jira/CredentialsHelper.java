package hudson.plugins.jira;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

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
	public static StandardUsernamePasswordCredentials lookupSystemCredentials(@CheckForNull String credentialsId, String url) {
		if (credentialsId == null) {
			return null;
		}
		return CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(
						StandardUsernamePasswordCredentials.class,
						Jenkins.getInstance(),
						ACL.SYSTEM,
						URIRequirementBuilder.fromUri(url).build()
				),
				CredentialsMatchers.withId(credentialsId)
		);
	}

	public static StandardUsernamePasswordCredentials migrateCredentials(String username, String password, String url) {
		StandardUsernamePasswordCredentials cred = null;

		List<StandardUsernamePasswordCredentials> credentials = CredentialsMatchers.filter(
				CredentialsProvider.lookupCredentials(
						StandardUsernamePasswordCredentials.class,
						Jenkins.getInstance(),
						ACL.SYSTEM,
						URIRequirementBuilder.fromUri(url).build()
				),
				CredentialsMatchers.withUsername(username)
		);
		for (StandardUsernamePasswordCredentials c : credentials) {
			if (StringUtils.equals(password, Secret.toString(c.getPassword()))) {
				cred = c;
				break; // found
			}
		}
		if (cred == null) {
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
				LOGGER.log(Level.INFO, "Migrated credentials");
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Unable to store migrated credentials", e);
			}
			cred = newCredentials;
		}
		return cred;
	}
}
