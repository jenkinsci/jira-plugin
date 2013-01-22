package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.jira.soap.RemoteComponent;
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

import javax.xml.rpc.ServiceException;
import java.io.*;
import java.util.Set;
import java.util.HashMap;

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
    private String component;

    @DataBoundConstructor
    public JiraCreateIssueNotifier(String projectKey,String testDescription,String assignee,String component) {
        if(projectKey == null) throw new IllegalArgumentException("Project key cannot be null");
        this.projectKey = projectKey;

        this.testDescription=testDescription;
        this.assignee=assignee;
        this.component=component;
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

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
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
        System.out.println("Env Variables::"+build.getEnvironment(TaskListener.NULL));
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
                if(issueId!=null) {
                 listener.getLogger().println("*************************Test fails again*****************************");
                 try{
                 //The status of the issue which was filed when the previous build failed
                 String Status=getStatus(build,issueId);
                 System.out.println("In perform method Status::"+Status);
                    //Status=1=Open OR Status=5=Resolved
                    if(Status.equals("1")||Status.equals("5")){
                        listener.getLogger().println("The previous build also failed creating issue with issue ID"+
                                " "+issueId);
                        listener.getLogger().println("The status of the Issue is opened or resolved");
                        addComment(build,issueId,comment);
                    }
                    if(Status.equals("6")){
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
                        listener.getLogger().println( "So Creating jira issue with issue ID"+" "+issue.getKey());
                    }
                }catch(ServiceException e){
                  e.printStackTrace();
                }
                }
            }else{
                try{
                    RemoteIssue issue=createJiraIssue(build);
                    listener.getLogger().println("**************************Test Fails******************************");
                    listener.getLogger().println( "Creating jira issue with issue ID"+" "+issue.getKey());

                }catch(ServiceException e)  {
                    System.out.print("Service Exception");
                    e.printStackTrace();
                }
            }
        }
        if(currentBuildResult==Result.SUCCESS && previousBuild!=null)  {
            if(previousBuild!=null && (previousBuildResult==Result.FAILURE || previousBuildResult==Result.SUCCESS)){
                String comment="- Job is not falling but the issue is still open."+"\n"+"- Passed run : ["+
                        buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+buildURL.concat("console")+"]";
                String issueId=getIssue(build);
                //if issue exists it will check the status and comment or delete the file accordingly
                if(issueId!=null){
                try{
                  String Status=getStatus(build,issueId);
                 //Status=1=Open OR Status=5=Resolved
                 if(Status.equals("1") ||Status.equals("5")){
                    System.out.println("The Issue is in opened or resolved status");
                    addComment(build, issueId, comment);
                 }
                //if issue is in closed status
                 if(Status.equals("6")){
                    System.out.println("The Issue is in closed status");
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
                }//end If
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
        String jenkinsURL="";
        RemoteComponent components[]=null;
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
            if(key=="JENKINS_URL"){
              jenkinsURL=environmentVariable.get(key);
            }
        }
        String checkDescription=(this.testDescription=="") ? "No description is provided" : this.testDescription;
        String description="The test "+jobName+" has failed."+"\n\n"+checkDescription+"\n\n"+ "* First failed run : ["+
                buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+buildURL.concat("console")+"]"+"\n\n\n\n"+
                "If it is false alert please notify to QA tools :"+"\n"+"# Move to the OTA project and"+"\n" +
                "# Set the component to Tools-Jenkins-Jira Integration.";
        if(this.component==""){
            components=null;
        }else{
            components=getComponent(build,this.component);
            System.out.println("In create issue, components of project::"+components);
        }
        String assignee = (this.assignee=="") ? "" : this.assignee;
        String summary="Test "+jobName+" failure - "+jenkinsURL;
        JiraSession session = getJiraSession(build);
        RemoteIssue issue = session.createIssue(projectKey,description,assignee,components,summary);
        String jobDirPath=Jenkins.getInstance().getBuildDirFor(build.getProject()).getPath();
        //creating a file in jobs directory and saving the issue-Id
        String filename=jobDirPath+"/"+"issue.txt";
        PrintWriter writer = new PrintWriter(filename);
        writer.println(issue.getKey());
        writer.close();
        return issue;
    }

    public String getStatus(AbstractBuild<?, ?> build,String id) throws ServiceException,IOException{
        JiraSession session = getJiraSession(build);
        RemoteIssue issue=session.getIssueByKey(id);
        String status=issue.getStatus();
        return status;
    }

    public void addComment(AbstractBuild<?, ?> build,String id,String comment) throws ServiceException,IOException{
        JiraSession session = getJiraSession(build);
        session.addCommentWithoutConstrains(id,comment);
    }

    public RemoteComponent[] getComponent(AbstractBuild<?, ?> build,String component) throws
            ServiceException,IOException{
        JiraSession session = getJiraSession(build);
        RemoteComponent availableComponents[]= session.getComponents(projectKey);
        //To store all the componets of the particular project
        HashMap<String,String> components=new HashMap<String, String>();
        //converting the user input as a string array
        String inputComponents[]=component.split(",");
        int numberOfComponents=inputComponents.length;
        System.out.println(numberOfComponents);
        RemoteComponent allcomponents[]=new RemoteComponent[numberOfComponents];
        for(RemoteComponent rc:availableComponents)  {
            String name=rc.getName();
            String id=rc.getId();
            components.put(name,id);
        }
        int i=0;
        while(i<numberOfComponents){
            RemoteComponent componentIssue=new RemoteComponent();
            String userInput= inputComponents[i];
            String id="";
            for(String key:components.keySet()) {
                if(userInput.equalsIgnoreCase(key)){
                    id=components.get(key);
                }
            }
            componentIssue.setName(userInput);
            componentIssue.setId(id);
            allcomponents[i]=componentIssue;
            i++;
        }
       return allcomponents;
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
            return issueId;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("There is no such file...!!");
            return null;
        }

    }

    public JiraSession getJiraSession(AbstractBuild<?, ?> build)throws ServiceException,IOException {
        JiraSite site = JiraSite.get(build.getProject());
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "
                + build.getFullDisplayName());
        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");
        return session;
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
