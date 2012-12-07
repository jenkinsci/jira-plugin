package hudson.plugins.jira.createissueparameter;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import javax.xml.rpc.ServiceException;
import java.io.IOException;

import static hudson.Util.fixNull;

/**
 * Created with IntelliJ IDEA.
 * User: rupali
 * Date: 6/12/12
 * Time: 2:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class JiraCreateIssueParameterDefinition extends ParameterDefinition {
    private String projectkey;
    private String assignee;

    @DataBoundConstructor
    public JiraCreateIssueParameterDefinition(String name, String description, String projectkey, String assignee) {
        super(name, description);
        this.projectkey = projectkey;
        this.assignee = assignee;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String[] values = req.getParameterValues(getName());
        if (values == null || values.length != 1) {
            return null;
        }
        return new JiraCreateIssueParameterValue(getName(),values[0]);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject formData) {
        JiraCreateIssueParameterValue value = req.bindJSON(JiraCreateIssueParameterValue.class, formData) ;
        return value;
    }

    public void createIssues() throws IOException, ServiceException {
        AbstractProject<?, ?> context = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);

        JiraSite site = JiraSite.get(context);
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "+context.getFullDisplayName());

        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");

       session.createIssue(projectkey,assignee);

    }


    public String getProjectkey() {
        return projectkey;
    }

    public void setProjectkey(String projectkey) {
        this.projectkey = projectkey;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "JIRA Create Issue Parameter";
        }
    }



}
