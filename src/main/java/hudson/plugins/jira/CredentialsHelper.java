package hudson.plugins.jira;

import javax.annotation.CheckForNull;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.security.ACL;
import jenkins.model.Jenkins;

/**
 * Helper class for vary credentials operations.
 *
 * @author Zhenlei Huang
 */
public class CredentialsHelper {

	@CheckForNull
	public static StandardUsernamePasswordCredentials lookupSystemCredentials(@CheckForNull String credentialsId, String url) {
		if (credentialsId == null) {
			return null;
		}
		return CredentialsMatchers.firstOrNull(
				CredentialsProvider
						.lookupCredentials(
								StandardUsernamePasswordCredentials.class,
								Jenkins.getInstance(),
								ACL.SYSTEM,
								URIRequirementBuilder.fromUri(url).build()
						),
				CredentialsMatchers.withId(credentialsId)
		);
	}
}
