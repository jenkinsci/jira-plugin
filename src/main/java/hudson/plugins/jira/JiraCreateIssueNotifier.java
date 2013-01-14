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
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.tasks.Notifier;
import hudson.util.FormValidation;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.Servlet;
import javax.xml.rpc.ServiceException;
import java.io.*;
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
        String buildURL="";
        String buildNumber="";
        EnvVars environmentVariable = build.getEnvironment(TaskListener.NULL);
        Set<String> keys=environmentVariable.keySet();
        for(String key:keys){
            if(key=="BUILD_URL"){
                buildURL=environmentVariable.get(key);
            }
            if(key=="BUILD_NUMBER") {
                buildNumber=environmentVariable.get(key);
            }
        }
        String jobDirPath=Jenkins.getInstance().getBuildDirFor(build.getProject()).getPath();
        String filename=jobDirPath+"/"+"issue.txt";
        if (currentBuildResult==Result.FAILURE)  {
            if(previousBuild!=null &&  previousBuildResult==Result.FAILURE) {

                String comment="- Job is still failing."+"\n"+"- Failed run : ["+
                        buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+buildURL.concat("console")+"]";
                //Get the issue-id which was filed when the previous built failed
                String issueId=getIssue(build);
                listener.getLogger().println("*************************Test fails again*****************************");
                //Checking the status of the issue.
                try{
                String Status=getStatus(build,issueId);
                    System.out.println("In perform method Status::"+Status);
                    //Status=1=Open OR Status=5=Resolved
                    if(Status.equals("1")||Status.equals("5")){
                        listener.getLogger().println("The previous build also failed creating issue with issue ID"+" "+issueId);
                        System.out.println("When Issue is opened or resolved");
                        addComment(build,issueId,comment);
                    }
                    if(Status.equals("6")){
                        System.out.println("When Issue is closed");
                        listener.getLogger().println("The previous build also failed but the issue is closed");
                        File file=new File(filename);
                        if(file.exists()){
                            if(file.delete()){
                                System.out.println("File deleted successfully...!!!");
                            }else{
                                System.out.println("File do not deleted :( ...!!!");
                            }
                        }

                        RemoteIssue issue=createJiraIssue(build);
                        listener.getLogger().println( "Creating jira issue with issue ID"+" "+issue.getKey());
                    }
                }catch(ServiceException e){
                  e.printStackTrace();
                }
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
        if(currentBuildResult==Result.SUCCESS && previousBuild!=null)  {
            if(previousBuild!=null && previousBuildResult==Result.FAILURE){
                //get the issue id, check for Status
                String comment="- Job is not falling but the issue is still open."+"\n"+"- Passed run : ["+
                        buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+buildURL.concat("console")+"]";
                String issueId=getIssue(build);
                try{
                String Status=getStatus(build,issueId);
                System.out.println("In perform Status::"+Status);
                //Status=1=Open OR Status=5=Resolved
                if(Status.equals("1") ||Status.equals("5")){
                    System.out.println("When Issue is opened or resolved");
                    addComment(build, issueId, comment);
                }
                //if issue is in closed status
                if(Status.equals("6")){
                    System.out.println("When Issue is closed");
                        File file=new File(filename);
                        if(file.exists()){
                            if(file.delete()){
                                System.out.println("File deleted successfully...!!!");
                            }else{
                                System.out.println("File do not deleted :( ...!!!");
                            }
                        }
                    }
                }catch(ServiceException e){
                    System.out.println("Service Exception");
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
        String description="The test "+jobName+" has failed."+"\n\n"+checkDescription+"\n\n"+ "* First failed run : ["+
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
        String jobDirPath=Jenkins.getInstance().getBuildDirFor(build.getProject()).getPath();
        String filename=jobDirPath+"/"+"issue.txt";
        PrintWriter writer = new PrintWriter(filename);
        writer.println(issue.getKey());
        writer.close();
        return issue;
    }

    public String getStatus(AbstractBuild<?, ?> build,String id) throws ServiceException,IOException{
        JiraSite site = JiraSite.get(build.getProject());
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "
                + build.getFullDisplayName());
        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");
        RemoteIssue issue=session.getIssueByKey(id);
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

    public String getIssue(AbstractBuild<?, ?> build) throws IOException,InterruptedException{

        String jobDirPath=Jenkins.getInstance().getBuildDirFor(build.getProject()).getPath();
        String filename=jobDirPath+"/"+"issue.txt";
        String issueId="";
        try {
            BufferedReader br = null;
            String issue;
            br = new BufferedReader(new FileReader(filename));

            while ((issue = br.readLine()) != null) {
                System.out.println(issue);
                issueId=issue;
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return issueId;
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
