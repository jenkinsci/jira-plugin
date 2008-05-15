package hudson.plugins.jira;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.CopyOnWriteList;
import hudson.util.FormFieldValidator;
import org.apache.axis.AxisFault;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.servlet.ServletException;
import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Associates {@link AbstractProject} with {@link JiraSite}.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraProjectProperty extends JobProperty<AbstractProject<?,?>> {

    /**
     * Used to find {@link JiraSite}. Matches {@link JiraSite#getName()}.
     * Always non-null (but beware that this value might become stale
     * if the system config is changed.)
     */
    public final String siteName;

    @DataBoundConstructor
    public JiraProjectProperty(String siteName) {
        if(siteName==null) {
            // defaults to the first one
            JiraSite[] sites = DESCRIPTOR.getSites();
            if(sites.length>0)
                siteName = sites[0].getName();
        }
        this.siteName = siteName;
    }

    /**
     * Gets the {@link JiraSite} that this project belongs to.
     *
     * @return
     *      null if the configuration becomes out of sync.
     */
    public JiraSite getSite() {
        JiraSite[] sites = DESCRIPTOR.getSites();
        if(siteName==null && sites.length>0)
            // default
            return sites[0];

        for( JiraSite site : sites ) {
            if(site.getName().equals(siteName))
                return site;
        }
        return null;
    }

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends JobPropertyDescriptor {
        private final CopyOnWriteList<JiraSite> sites = new CopyOnWriteList<JiraSite>();

        public DescriptorImpl() {
            super(JiraProjectProperty.class);
            load();
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public String getDisplayName() {
            return Messages.JiraProjectProperty_DisplayName();
        }

        public JiraSite[] getSites() {
            return sites.toArray(new JiraSite[0]);
        }
        
        public JobProperty<?> newInstance(StaplerRequest req) throws FormException {
            JiraProjectProperty jpp = req.bindParameters(JiraProjectProperty.class, "jira.");
            if(jpp.siteName==null)
                jpp = null; // not configured
            return jpp;
        }

        public boolean configure(StaplerRequest req) {
            sites.replaceBy(req.bindParametersToList(JiraSite.class,"jira."));
            save();
            return true;
        }

        /**
         * Checks if the JIRA URL is accessible and exists.
         */
        public void doUrlCheck(final StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            // this can be used to check existence of any file in any URL, so admin only
            new FormFieldValidator.URLCheck(req,rsp) {
                protected void check() throws IOException, ServletException {
                    String url = Util.fixEmpty(request.getParameter("value"));
                    if(url==null) {
                        error(Messages.JiraProjectProperty_JiraUrlMandatory());
                        return;
                    }

                    try {
                        if(findText(open(new URL(url)),"Atlassian JIRA"))
                            ok();
                        else
                            error(Messages.JiraProjectProperty_NotAJiraUrl());
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Unable to connect to "+url, e);
                        handleIOException(url,e);
                    }
                }
            }.process();
        }

        /**
         * Checks if the user name and password are valid.
         */
        public void doLoginCheck(final StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    String url = Util.fixEmpty(request.getParameter("url"));
                    if(url==null) {// URL not entered yet
                        ok();
                        return;
                    }
                    JiraSite site = new JiraSite(new URL(url),
                        request.getParameter("user"),
                        request.getParameter("pass"),false);
                    try {
                        site.createSession();
                        ok();
                    } catch (AxisFault e) {
                        LOGGER.log(Level.WARNING, "Failed to login to JIRA at "+url,e);
                        error(e.getFaultString());
                    } catch (ServiceException e) {
                        LOGGER.log(Level.WARNING, "Failed to login to JIRA at "+url,e);
                        error(e.getMessage());
                    }
                }
            }.process();
        }

        public void save() {
            super.save();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(JiraProjectProperty.class.getName());
}
