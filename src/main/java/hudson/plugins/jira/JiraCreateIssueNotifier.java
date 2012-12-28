package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.tasks.Notifier;
import hudson.util.FormValidation;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: rupali
 * Date: 13/12/12
 * Time: 7:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class JiraCreateIssueNotifier extends Notifier{

    private String projectKey;
    private String testDescription;

    @DataBoundConstructor
    public JiraCreateIssueNotifier(String projectKey,String testDescription) {
        if(projectKey == null) throw new IllegalArgumentException("Project key cannot be null");
        this.projectKey = projectKey;

        this.testDescription=testDescription;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getTestDescription() {
        return testDescription;
    }

    public void setTestDescription(String testDescription) {
        this.testDescription = testDescription;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        Result currentBuildResult= build.getResult();
        Result previousBuildResult=null;
        System.out.println(currentBuildResult);
        AbstractBuild previousBuild=build.getPreviousBuild();
        System.out.print(previousBuild);
        if(previousBuild!=null){
            previousBuildResult= previousBuild.getResult();
        }
        System.out.println(previousBuildResult);
        if (Result.FAILURE==currentBuildResult)  {
            if(previousBuild!=null &&  previousBuildResult==Result.FAILURE)
            {   //Do nothing
                listener.getLogger().println("*************************Test fails again*****************************");
                listener.getLogger().println("The previous build also failed");
            }else{
                try{
                    RemoteIssue issue=createJiraIssue(build);
                    listener.getLogger().println("**************************Test fails******************************");
                    listener.getLogger().println( "Creating jira issue with issue ID"+" "+issue.getKey());

                }catch(ServiceException e)  {
                    System.out.print("Service Exception");
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public RemoteIssue createJiraIssue(AbstractBuild<?, ?> build) throws ServiceException,IOException,
            InterruptedException{
        String buildURL= getBuildURL(build)  ;
        String checkDescription=(testDescription=="") ? "No Description is provided" : testDescription;
        String description="As the test fails on jenkins this issue is created." +
                " Description of the test : "+checkDescription+ ". For more details please check here:" + buildURL+
                ". If it is false alert please notify to QA tools : 1.Move the project to OTA and" +
                " 2.Add the component as Tools-Jenkins-Jira Integration.";
        JiraSite site = JiraSite.get(build.getProject());
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "
                + build.getFullDisplayName());
        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");
        RemoteIssue issue = session.createIssue(projectKey,description,buildURL);

        return issue;
    }
     public String getBuildURL(AbstractBuild<?, ?> build) throws IOException,InterruptedException{
         EnvVars env = build.getEnvironment();
         String buildURL="";
         Set<String> keys=env.keySet();
         for(String key:keys){
             if(key=="BUILD_URL"){
                 buildURL=env.get(key);
                 System.out.println(buildURL);
             }
         }
         return buildURL;
     }

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(JiraCreateIssueNotifier.class);
        }
        public FormValidation doCheckProjectKey(@QueryParameter String value)
                throws IOException {
            if (value.length() == 0) {
                return FormValidation.error("Please set the project key");
            }
            return FormValidation.ok();
        }

        @Override
        public JiraCreateIssueNotifier newInstance(StaplerRequest req,
                                             JSONObject formData) throws FormException {
            return req.bindJSON(JiraCreateIssueNotifier.class, formData);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Create Jira Issue" ;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help-jira-create-issue.html";
        }
    }
}
