package hudson.plugins.jira;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.CopyOnWriteList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

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
		public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			JiraProjectProperty jpp = req.bindParameters(
					JiraProjectProperty.class, "jira.");
			if (jpp.siteName == null)
				jpp = null; // not configured
			return jpp;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) {
			sites.replaceBy(req.bindJSONToList(JiraSite.class, formData.get("sites")));
			save();
			return true;
		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(JiraProjectProperty.class.getName());
}
