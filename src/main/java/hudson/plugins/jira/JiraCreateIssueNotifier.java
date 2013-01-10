package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.jira.soap.RemoteComment;
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
    private String assignee;

    @DataBoundConstructor
    public JiraCreateIssueNotifier(String projectKey,String testDescription,String assignee) {
        if(projectKey == null) throw new IllegalArgumentException("Project key cannot be null");
        this.projectKey = projectKey;

        this.testDescription=testDescription;
        this.assignee=assignee;
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

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
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
            throws InterruptedException, IOException{
        Result currentBuildResult= build.getResult();
        System.out.println("variables::"+build.getEnvironment(TaskListener.NULL));
        Result previousBuildResult=null;
        AbstractBuild previousBuild=build.getPreviousBuild();
        if(previousBuild!=null){
            previousBuildResult= previousBuild.getResult();
        }
        System.out.println("console::"+previousBuild.getLog());
       /* if(previousBuildResult==Result.FAILURE){
               String consoleLog=previousBuild.getLog();
               int i=consoleLog.indexOf("issue ID");
               String sub=consoleLog.substring(i+8,i+16).trim();
               System.out.println("substring::" + sub);
        }  */
        if (Result.FAILURE==currentBuildResult)  {
            if(previousBuild!=null &&  previousBuildResult==Result.FAILURE)
            {   //Do nothing
                 String comment="";
                //need to improve persisting of issue-id
                String consoleLog=previousBuild.getLog();
                int i=consoleLog.indexOf("issue ID");
                String issueId=consoleLog.substring(i+8,i+16).trim();
                System.out.println("substring::"+issueId);
                listener.getLogger().println("*************************Test fails again*****************************");
                listener.getLogger().println("The previous build also failed creating issue with issue ID"+" "+issueId);
                //check for the issue status and then comment on it
                try{
                String Status=getStatus(build,"OJRA-97");
                    System.out.println("Status::-"+Status);
                    if(Status=="1" ||Status=="5"){
                        addComment(build,issueId,comment);
                    }
                }catch(ServiceException e){
                  e.printStackTrace();
                }
            }else{
                try{
                    RemoteIssue issue=createJiraIssue(build);
                    listener.getLogger().println("**************************Test fails******************************");
                    listener.getLogger().println( "Creating jira issue with issue ID"+" "+issue.getKey());
                    System.out.println( "Status of issue "+issue.getKey()+" ::"+issue.getStatus());

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
        EnvVars environmentVariable = build.getEnvironment(TaskListener.NULL);
        String buildURL="";
        String buildNumber="";
        String jobName="";
        Set<String> keys=environmentVariable.keySet();
        for(String key:keys){
            if(key=="BUILD_URL"){
                buildURL=environmentVariable.get(key);
            }
            if(key=="BUILD_NUMBER") {
                buildNumber=environmentVariable.get(key);
            }
            if(key=="JOB_NAME"){
                jobName=environmentVariable.get(key);
            }
        }
        String checkDescription=(this.testDescription=="") ? "No description is provided" : this.testDescription;
        String description="The test "+jobName+" has failed."+"\n\n"+checkDescription+ "* First failed run : ["+
                buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+buildURL.concat("console")+"]"+"\n\n\n\n"+
                "If it is false alert please notify to QA tools :"+"\n"+"# Move to the OTA project and"+"\n" +
                "# Set the component to Tools-Jenkins-Jira Integration.";
        String assignee = (this.assignee=="") ? "" : this.assignee;
        JiraSite site = JiraSite.get(build.getProject());
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "
                + build.getFullDisplayName());
        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");
        RemoteIssue issue = session.createIssue(projectKey,description,assignee);

        return issue;
    }

    public String getStatus(AbstractBuild<?, ?> build,String id) throws ServiceException,IOException{
        JiraSite site = JiraSite.get(build.getProject());
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "
                + build.getFullDisplayName());
        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");
        RemoteIssue issue=session.getIssueById(id);
        String status=issue.getStatus();
        return status;
    }

    public void addComment(AbstractBuild<?, ?> build,String id,String comment) throws ServiceException,IOException{
        JiraSite site = JiraSite.get(build.getProject());
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "
                + build.getFullDisplayName());
        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");
        session.addCommentWithoutConstrains(id,comment);
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