package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.jira.soap.RemoteComponent;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;
import hudson.util.FormValidation;

import javax.xml.rpc.ServiceException;
import java.io.*;
import java.util.HashMap;

/**
 * <p>
 *  When a build fails it creates jira issues.
 * @author rupali
 */
public class JiraCreateIssueNotifier extends Notifier{

    private String projectKey;
    private String testDescription;
    private String assignee;
    private String component;

    @DataBoundConstructor
    public JiraCreateIssueNotifier(String projectKey,String testDescription,String assignee,
                                   String component) {
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
            throws IOException {

        String jobDirPath=Jenkins.getInstance().getBuildDirFor(build.getProject()).getPath();
        String filename=jobDirPath+"/"+"issue.txt";
        try {
            EnvVars environmentVariable = build.getEnvironment(TaskListener.NULL);
            System.out.println("Env Variables::"+environmentVariable);

            Result currentBuildResult= build.getResult();
            System.out.println("current result::"+currentBuildResult);

            Result previousBuildResult=null;
            AbstractBuild previousBuild=build.getPreviousBuild();

            if(previousBuild!=null) {
            previousBuildResult= previousBuild.getResult();
            System.out.println("previous result::"+previousBuildResult);
            }

            if(currentBuildResult!=Result.ABORTED && previousBuild!=null) {
                if (currentBuildResult==Result.FAILURE) {
                currentBuildResultFailure(build,listener,previousBuildResult,filename,environmentVariable);
                }

                if(currentBuildResult==Result.SUCCESS) {
                currentBuildResultSuccess(build,previousBuildResult,filename,environmentVariable);
                }
            }//end If(currentBuildResult!=Result.ABORTED && previousBuild!=null)
       }catch(InterruptedException e) {
          System.out.print("Build is aborted..!!!");
          e.printStackTrace();
       }
       return true;
    }//end perform

    public RemoteIssue createJiraIssue(AbstractBuild<?, ?> build,String filename) throws ServiceException,
            IOException,InterruptedException {
        EnvVars environmentVariable = build.getEnvironment(TaskListener.NULL);
        String checkDescription;
        RemoteComponent components[]=null;
        String buildURL=environmentVariable.get("BUILD_URL");
        String buildNumber=environmentVariable.get("BUILD_NUMBER");
        String jobName=environmentVariable.get("JOB_NAME");
        String jenkinsURL=Jenkins.getInstance().getRootUrl();

        checkDescription=(this.testDescription=="") ? "No description is provided" : this.testDescription;
        String description="The test "+jobName+" has failed."+"\n\n"+checkDescription+"\n\n"+
                "* First failed run : ["+buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+
                buildURL.concat("console")+"]"+"\n\n\n\n"+"If it is false alert please notify to QA tools :"
                +"\n"+"# Move to the OTA project and"+"\n"
                +"# Set the component to Tools-Jenkins-Jira Integration.";

        String assignee = (this.assignee=="") ? "" : this.assignee;

        if(this.component=="") {
            components=null;
        }else{
            components=getComponent(build,this.component);
        }

        String summary="Test "+jobName+" failure - "+jenkinsURL;

        JiraSession session = getJiraSession(build);
        RemoteIssue issue = session.createIssue(projectKey,description,assignee,components,summary);
        //writing the issue-id to the file, which is present in job's directory.
        writeInFile(filename,issue);
        return issue;
    }

    public String getStatus(AbstractBuild<?, ?> build,String id) throws ServiceException,IOException {
        JiraSession session = getJiraSession(build);
        RemoteIssue issue=session.getIssueByKey(id);
        String status=issue.getStatus();
        return status;
    }

    public void addComment(AbstractBuild<?, ?> build,String id,String comment)
            throws ServiceException,IOException {
        JiraSession session = getJiraSession(build);
        session.addCommentWithoutConstrains(id,comment);
    }

    public RemoteComponent[] getComponent(AbstractBuild<?, ?> build,String component) throws
            ServiceException,IOException {
        JiraSession session = getJiraSession(build);
        RemoteComponent availableComponents[]= session.getComponents(projectKey);
        //To store all the componets of the particular project
        HashMap<String,String> components=new HashMap<String, String>();
        //converting the user input as a string array
        String inputComponents[]=component.split(",");
        int numberOfComponents=inputComponents.length;
        RemoteComponent allcomponents[]=new RemoteComponent[numberOfComponents];
        for(RemoteComponent rc:availableComponents) {
            String name=rc.getName();
            String id=rc.getId();
            components.put(name,id);
        }
        int i=0;
        while(i<numberOfComponents) {
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

    public String getIssue(String filename) throws IOException,InterruptedException {

        String issueId="";
        try {
            BufferedReader br = null;
            String issue;
            br = new BufferedReader(new FileReader(filename));

            while ((issue = br.readLine()) != null) {
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

    public JiraSession getJiraSession(AbstractBuild<?, ?> build) throws ServiceException,IOException {
        JiraSite site = JiraSite.get(build.getProject());
        if (site==null)  throw new IllegalStateException("JIRA site needs to be configured in the project "
                + build.getFullDisplayName());
        JiraSession session = site.createSession();
        if (session==null)  throw new IllegalStateException("Remote SOAP access for JIRA isn't " +
                "configured in Jenkins");
        return session;
    }

    public void deleteFile(String filename) {
        File file=new File(filename);
        if(file.exists()) {
            if(file.delete()) {
                System.out.println("File deleted successfully...!!!");
            }else{
                System.out.println("File do not deleted :( ...!!!");
            }
        }
    }

    public void writeInFile(String Filename,RemoteIssue issue) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(Filename);
        writer.println(issue.getKey());
        writer.close();
    }

    public void currentBuildResultFailure(AbstractBuild<?, ?> build,BuildListener listener,
                    Result previousBuildResult,String filename,EnvVars environmentVariable)
                throws InterruptedException,IOException {

        String buildURL=environmentVariable.get("BUILD_URL");
        String buildNumber=environmentVariable.get("BUILD_NUMBER");
        if(previousBuildResult==Result.FAILURE) {
            System.out.println("Current result failed and previous built also failed");
            String comment="- Job is still failing."+"\n"+"- Failed run : ["+
                    buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+buildURL.concat("console")+"]";
            //Get the issue-id which was filed when the previous built failed
            String issueId=getIssue(filename);
            if(issueId!=null) {
                listener.getLogger().println("*************************Test fails again****************"+
                        "**************");
                try{
                    //The status of the issue which was filed when the previous build failed
                    String Status=getStatus(build,issueId);
                    System.out.println("In perform method Status::"+Status);
                    //Status=1=Open OR Status=5=Resolved
                    if(Status.equals("1")||Status.equals("5")) {
                        listener.getLogger().println("The previous build also failed creating issue with "+
                                "issue ID"+" "+issueId);
                        System.out.println("The status of the Issue is opened or resolved");
                        addComment(build,issueId,comment);
                    }
                    if(Status.equals("6")) {
                        listener.getLogger().println("The previous build also failed but the issue " +
                                "is closed");
                        deleteFile(filename);
                        RemoteIssue issue=createJiraIssue(build,filename);
                        listener.getLogger().println( "So Creating jira issue with issue ID"+
                                " "+issue.getKey());
                    }
                }catch(ServiceException e) {
                    e.printStackTrace();
                }
            }
        } //end if(previousBuildResult==Result.FAILURE)

        if(previousBuildResult==Result.SUCCESS || previousBuildResult==Result.ABORTED) {
            System.out.println("Creating issue");
            try{
                RemoteIssue issue=createJiraIssue(build,filename);
                listener.getLogger().println("**************************Test Fails************" +
                        "******************");
                listener.getLogger().println( "Creating jira issue with issue ID"
                        +" "+issue.getKey());

            }catch(ServiceException e) {
                System.out.print("Service Exception");
                e.printStackTrace();
            }
        } //end if(previousBuildResult==Result.SUCCESS || previousBuildResult==Result.ABORTED)
    }

    public void currentBuildResultSuccess(AbstractBuild<?, ?> build,Result previousBuildResult,
                    String filename,EnvVars environmentVariable) throws InterruptedException,IOException {
        String buildURL=environmentVariable.get("BUILD_URL");
        String buildNumber=environmentVariable.get("BUILD_NUMBER");

        if(previousBuildResult==Result.FAILURE || previousBuildResult==Result.SUCCESS) {
            System.out.println("Current result success and previous built also failed or success");
            String comment="- Job is not falling but the issue is still open."+"\n"+"- Passed run : ["+
                    buildNumber+"|"+buildURL+"]"+"\n"+ "** [console log|"+buildURL.concat("console")+"]";
            String issueId=getIssue(filename);
            //if issue exists it will check the status and comment or delete the file accordingly
            if(issueId!=null){
                try{
                    String Status=getStatus(build,issueId);
                    //Status=1=Open OR Status=5=Resolved
                    if(Status.equals("1") ||Status.equals("5")) {
                        System.out.println("The Issue is in opened or resolved status");
                        addComment(build, issueId, comment);
                    }
                    //if issue is in closed status
                    if(Status.equals("6")){
                        System.out.println("The Issue is in closed status");
                        deleteFile(filename);
                    }
                }catch(ServiceException e){
                    System.out.println("Service Exception");
                    e.printStackTrace();
                }
            }//end If(issueId!=null)
        }
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
