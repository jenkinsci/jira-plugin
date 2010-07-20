package hudson.plugins.jira;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.plugins.jira.soap.RemoteGroup;
import hudson.plugins.jira.soap.RemoteValidationException;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import org.apache.axis.AxisFault;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.servlet.ServletException;
import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;

/**
 * Associates {@link AbstractProject} with {@link JiraSite}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class JiraProjectProperty extends JobProperty<AbstractProject<?, ?>> {

	/**
	 * Used to find {@link JiraSite}. Matches {@link JiraSite#getName()}. Always
	 * non-null (but beware that this value might become stale if the system
	 * config is changed.)
	 */
	public final String siteName;

	@DataBoundConstructor
	public JiraProjectProperty(String siteName) {
		if (siteName == null) {
			// defaults to the first one
			JiraSite[] sites = DESCRIPTOR.getSites();
			if (sites.length > 0)
				siteName = sites[0].getName();
		}
		this.siteName = siteName;
	}

	/**
	 * Gets the {@link JiraSite} that this project belongs to.
	 * 
	 * @return null if the configuration becomes out of sync.
	 */
	public JiraSite getSite() {
		JiraSite[] sites = DESCRIPTOR.getSites();
		if (siteName == null && sites.length > 0)
			// default
			return sites[0];

		for (JiraSite site : sites) {
			if (site.getName().equals(siteName))
				return site;
		}
		return null;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends JobPropertyDescriptor {
		private final CopyOnWriteList<JiraSite> sites = new CopyOnWriteList<JiraSite>();

		public DescriptorImpl() {
			super(JiraProjectProperty.class);
			load();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean isApplicable(Class<? extends Job> jobType) {
			return AbstractProject.class.isAssignableFrom(jobType);
		}

		@Override
		public String getDisplayName() {
			return Messages.JiraProjectProperty_DisplayName();
		}

		public void setSites(JiraSite site) {
			sites.add(site);
		}

		public JiraSite[] getSites() {
			return sites.toArray(new JiraSite[0]);
		}

		@Override
		public JobProperty<?> newInstance(StaplerRequest req)
				throws FormException {
			JiraProjectProperty jpp = req.bindParameters(
					JiraProjectProperty.class, "jira.");
			if (jpp.siteName == null)
				jpp = null; // not configured
			return jpp;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) {
			sites.replaceBy(req.bindParametersToList(JiraSite.class, "jira."));
			save();
			return true;
		}

		/**
		 * Checks if the JIRA URL is accessible and exists.
		 */
		public FormValidation doUrlCheck(@QueryParameter final String value)
				throws IOException, ServletException {
			// this can be used to check existence of any file in any URL, so
			// admin only
			if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER))
				return FormValidation.ok();

			return new FormValidation.URLCheck() {
				@Override
				protected FormValidation check() throws IOException,
						ServletException {
					String url = Util.fixEmpty(value);
					if (url == null) {
						return FormValidation.error(Messages
								.JiraProjectProperty_JiraUrlMandatory());
					}

					try {
						if (findText(open(new URL(url)), "Atlassian JIRA"))
							return FormValidation.ok();
						else
							return FormValidation.error(Messages
									.JiraProjectProperty_NotAJiraUrl());
					} catch (IOException e) {
						LOGGER.log(Level.WARNING,
								"Unable to connect to " + url, e);
						return handleIOException(url, e);
					}
				}
			}.check();
		}

		/**
		 * Checks if the user name and password are valid.
		 */
		public FormValidation doLoginCheck(StaplerRequest request)
				throws IOException {
			String url = Util.fixEmpty(request.getParameter("url"));
			if (url == null) {// URL not entered yet
				return FormValidation.ok();
			}
			JiraSite site = new JiraSite(new URL(url), request
					.getParameter("user"), request.getParameter("pass"), false,
					false, null, false, request.getParameter("groupVisibility"));
			try {
				site.createSession();
				return FormValidation.ok();
			} catch (AxisFault e) {
				LOGGER.log(Level.WARNING, "Failed to login to JIRA at " + url,
						e);
				return FormValidation.error(e.getFaultString());
			} catch (ServiceException e) {
				LOGGER.log(Level.WARNING, "Failed to login to JIRA at " + url,
						e);
				return FormValidation.error(e.getMessage());
			}
		}

		public FormValidation doUserPatternCheck(StaplerRequest request)
				throws IOException {
			String userPattern = Util.fixEmpty(request
					.getParameter("userPattern"));
			if (userPattern == null) {// userPattern not entered yet
				return FormValidation.ok();
			}
			try {
				Pattern.compile(userPattern);
				return FormValidation.ok();
			} catch (PatternSyntaxException e) {
				return FormValidation.error(e.getMessage());
			}
		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(JiraProjectProperty.class.getName());
}
