package hudson.plugins.jira;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.google.common.base.Optional;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Helper class for vary credentials operations.
 *
 * @author Zhenlei Huang
 */
public class CredentialsHelper {
	private static final Logger LOGGER = Logger.getLogger(CredentialsHelper.class.getName());

	@CheckForNull
	protected static StandardUsernamePasswordCredentials lookupSystemCredentials(@CheckForNull String credentialsId, @CheckForNull URL url) {
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

	protected static StandardUsernamePasswordCredentials migrateCredentials(@Nonnull String username, String password, @CheckForNull URL url) {
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
				"Migrated by Jira Plugin",
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

	protected static ListBoxModel doFillCredentialsIdItems( Item item, String credentialsId, String uri) {
		StandardListBoxModel result = new StandardListBoxModel();
		if (item == null) {
			if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
				return result.includeCurrentValue(credentialsId);
			}
		} else {
			if (!item.hasPermission(Item.EXTENDED_READ)
				&& !item.hasPermission(CredentialsProvider.USE_ITEM)) {
				return result.includeCurrentValue(credentialsId);
			}
		}
		return result //
			.includeEmptyValue() //
            .includeMatchingAs(
                item instanceof Queue.Task ? Tasks.getAuthenticationOf( (Queue.Task) item) : ACL.SYSTEM,
                item,
                StandardCredentials.class,
                URIRequirementBuilder.fromUri( uri).build(),
                CredentialsMatchers.anyOf(
					CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
					CredentialsMatchers.instanceOf(UsernamePasswordCredentials.class)))
			.includeCurrentValue(credentialsId);
	}

	protected static FormValidation doCheckFillCredentialsId(
		Item item, String credentialsId, String uri) {
		if (item == null) {
			if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
				return FormValidation.ok();
			}
		} else {
			if (!item.hasPermission(Item.EXTENDED_READ)
				&& !item.hasPermission(CredentialsProvider.USE_ITEM)) {
				return FormValidation.ok();
			}
		}
		if (isNullOrEmpty(credentialsId)) {
			return FormValidation.ok();
		}
		if (!(findCredentials(item, credentialsId, uri).isPresent())) {
			return FormValidation.error("Cannot find currently selected credentials");
		}
		return FormValidation.ok();
	}

	protected static Optional<StandardUsernamePasswordCredentials> findCredentials(
		Item item, String credentialsId, String uri) {
		if (isNullOrEmpty(credentialsId)) {
			return absent();
		}
		return fromNullable(
			CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class,
					item,
					item instanceof Queue.Task
						? Tasks.getAuthenticationOf( (Queue.Task) item)
						: ACL.SYSTEM,
					URIRequirementBuilder.fromUri(uri).build()),
				CredentialsMatchers.allOf(
					CredentialsMatchers.withId(credentialsId),
					CredentialsMatchers.anyOf(
						CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)))));
	}

}
