package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.xml.rpc.ServiceException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * When a build fails it creates jira issues.
 * Repeated failures does not create a new issue but update the existing issue until the issue is closed.
 *
 * @author Rupali Behera rupali@vertisinfotech.com
 */
public class JiraCreateIssueNotifier extends Notifier {

    private static final Logger LOG = Logger.getLogger(JiraCreateIssueNotifier.class.getName());

    private String projectKey;
    private String testDescription;
    private String assignee;
    private String component;

    @DataBoundConstructor
    public JiraCreateIssueNotifier(String projectKey, String testDescription, String assignee, String component) {
        if (projectKey == null) throw new IllegalArgumentException("Project key cannot be null");
        this.projectKey = projectKey;

        this.testDescription = testDescription;
        this.assignee = assignee;
        this.component = component;
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        String jobDirPath = Jenkins.getInstance().getBuildDirFor(build.getProject()).getPath();
        String filename = jobDirPath + File.separator + "issue.txt";

        EnvVars environmentVariable = build.getEnvironment(TaskListener.NULL);

        Result currentBuildResult = build.getResult();

        Result previousBuildResult = null;
        AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();

        if (previousBuild != null) {
            previousBuildResult = previousBuild.getResult();
        }

        if (currentBuildResult != Result.ABORTED && previousBuild != null) {
            if (currentBuildResult == Result.FAILURE) {
                currentBuildResultFailure(build, listener, previousBuildResult, filename, environmentVariable);
            }

            if (currentBuildResult == Result.SUCCESS) {
                currentBuildResultSuccess(build, listener, previousBuildResult, filename, environmentVariable);
            }
        }
        return true;
    }

    /**
     * It creates a issue in the given project, with the given description, assignee,components and summary.
     * The created issue ID is saved to the file at "filename".
     *
     * @param build
     * @param filename
     * @return issue id
     * @throws ServiceException
     * @throws IOException
     * @throws InterruptedException
     */
    private Issue createJiraIssue(AbstractBuild<?, ?> build, String filename) throws ServiceException, IOException, InterruptedException {

        EnvVars environmentVariable = build.getEnvironment(TaskListener.NULL);

        String buildURL = environmentVariable.get("BUILD_URL");
        String buildNumber = environmentVariable.get("BUILD_NUMBER");
        String jobName = environmentVariable.get("JOB_NAME");
        String jenkinsURL = Jenkins.getInstance().getRootUrl();

        String checkDescription = (this.testDescription.equals("")) ? "No description is provided" : this.testDescription;
        String description = String.format("The test %s has failed. \n\n%s\n\n* First failed run : [%s|%s]\n** [console log|%s]",
                jobName, checkDescription, buildNumber, buildURL, buildURL.concat("console"));

        List<Component> components = getJiraComponents(build, this.component);

        String summary = "Test " + jobName + " failure - " + jenkinsURL;

        JiraSession session = getJiraSession(build);
        Issue issue = session.createIssue(projectKey, description, assignee, components, summary);

        //writing the issue-id to the file, which is present in job's directory.
        writeInFile(filename, issue);
        return issue;
    }

    /**
     * Returns the status of the issue.
     *
     * @param build
     * @param id
     * @return Status of the issue
     * @throws ServiceException
     * @throws IOException
     */
    private String getStatus(AbstractBuild<?, ?> build, String id) throws ServiceException, IOException {

        JiraSession session = getJiraSession(build);
        Issue issue = session.getIssueByKey(id);
        return issue.getStatus().getName();
    }

    /**
     * Adds a comment to the existing issue.
     *
     * @param build
     * @param id
     * @param comment
     * @throws ServiceException
     * @throws IOException
     */
    private void addComment(AbstractBuild<?, ?> build, String id, String comment) throws ServiceException, IOException {

        JiraSession session = getJiraSession(build);
        session.addCommentWithoutConstrains(id, comment);
    }

    /**
     * Returns an Array of componets given by the user
     *
     * @param build
     * @param component
     * @return Array of component
     * @throws ServiceException
     * @throws IOException
     */
    private List<Component> getJiraComponents(AbstractBuild<?, ?> build, String component) throws ServiceException, IOException {

        if (Util.fixEmpty(component) == null) {
            return null;
        }

        JiraSession session = getJiraSession(build);
        List<Component> availableComponents = session.getComponents(projectKey);

        //converting the user input as a string array
        List<String> inputComponents = Arrays.asList(component.split(","));
        int numberOfComponents = inputComponents.size();

        final List<Component> jiraComponents = new ArrayList<Component>(numberOfComponents);

        for (final Component availableComponent : availableComponents) {
            if (inputComponents.contains(availableComponent.getName())) {
                jiraComponents.add(availableComponent);
            }
        }

        return jiraComponents;
    }

    /**
     * Returns the issue id
     *
     * @param filename
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private String getIssue(String filename) throws IOException, InterruptedException {

        String issueId = "";
        BufferedReader br = null;
        try {
            String issue;
            br = new BufferedReader(new FileReader(filename));

            while ((issue = br.readLine()) != null) {
                issueId = issue;
            }
            return issueId;
        } catch (FileNotFoundException e) {
            System.out.println("There is no such file...!!");
            return null;
        } finally {
            if (br != null) {
                br.close();
            }
        }

    }

    /**
     * Returns the jira session.
     *
     * @param build
     * @return JiraSession
     * @throws ServiceException
     * @throws IOException
     */
    private JiraSession getJiraSession(AbstractBuild<?, ?> build) throws ServiceException, IOException {

        JiraSite site = JiraSite.get(build.getProject());
        if (site == null) {
            throw new IllegalStateException("JIRA site needs to be configured in the project " + build.getFullDisplayName());
        }

        JiraSession session = site.createSession();
        if (session == null) {
            throw new IllegalStateException("Remote SOAP access for JIRA isn't configured in Jenkins");
        }

        return session;
    }

    /**
     * @param filename
     */
    private void deleteFile(String filename, TaskListener listener) {
        File file = new File(filename);
        if (file.exists()) {
            if (!file.delete()) {
                if (file.exists()) {
                    listener.getLogger().println("WARNING: couldn't delete file: " + filename);
                }
                // else: race condition? Someone else deleted it for us
            }
        }
    }

    /**
     * write's the issue id in the file, which is stored in the Job's directory
     *
     * @param Filename
     * @param issue
     * @throws FileNotFoundException
     */
    private void writeInFile(String Filename, Issue issue) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(Filename);
        writer.println(issue.getKey());
        writer.close();
    }

    /**
     * when the current build fails it checks for the previous build's result,
     * creates jira issue if the result was "success" and adds comment if the result was "fail".
     * It adds comment until the previously created issue is closed.
     */
    private void currentBuildResultFailure(AbstractBuild<?, ?> build, BuildListener listener, Result previousBuildResult,
                                           String filename, EnvVars environmentVariable) throws InterruptedException, IOException {

        String buildURL = environmentVariable.get("BUILD_URL");
        String buildNumber = environmentVariable.get("BUILD_NUMBER");
        if (previousBuildResult == Result.FAILURE) {
            String comment = String.format("- Job is still failing.\n- Failed run : [%s|%s]\n** [console log|%s]",
                    buildNumber, buildURL, buildURL.concat("console"));
            //Get the issue-id which was filed when the previous built failed
            String issueId = getIssue(filename);
            if (issueId != null) {
                listener.getLogger().println("*************************Test fails again******************************");
                try {
                    //The status of the issue which was filed when the previous build failed
                    String Status = getStatus(build, issueId);

                    //Status=1=Open OR Status=5=Resolved
                    if (Status.equals("1") || Status.equals("5")) {
                        listener.getLogger().println("The previous build also failed creating issue with issue ID " + issueId);
                        addComment(build, issueId, comment);
                    }

                    if (Status.equals("6")) {
                        listener.getLogger().println("The previous build also failed but the issue is closed");
                        deleteFile(filename, listener);
                        Issue issue = createJiraIssue(build, filename);
                        listener.getLogger().println("Creating jira issue with issue ID " + issue.getKey());
                    }
                } catch (ServiceException e) {
                    e.printStackTrace();
                }
            }
        }

        if (previousBuildResult == Result.SUCCESS || previousBuildResult == Result.ABORTED) {
            try {
                Issue issue = createJiraIssue(build, filename);
                listener.getLogger().println("**************************Test Fails******************************");
                listener.getLogger().println("Creating jira issue with issue ID"
                        + " " + issue.getKey());

            } catch (ServiceException e) {
                listener.error("Error creating JIRA issue : " + e.getMessage());
                LOG.warning("Error creating JIRA issue\n" + e);
            }
        }
    }

    /**
     * when the current build's result is "success",
     * it checks for the previous build's result and adds comment until the previously created issue is closed.
     *
     * @param build
     * @param previousBuildResult
     * @param filename
     * @param environmentVariable
     * @throws InterruptedException
     * @throws IOException
     */
    private void currentBuildResultSuccess(AbstractBuild<?, ?> build, BuildListener listener, Result previousBuildResult,
                                           String filename, EnvVars environmentVariable) throws InterruptedException, IOException {
        String buildURL = environmentVariable.get("BUILD_URL");
        String buildNumber = environmentVariable.get("BUILD_NUMBER");

        if (previousBuildResult == Result.FAILURE || previousBuildResult == Result.SUCCESS) {
            String comment = String.format("- Job is not failing but the issue is still open \n - Passed run : [%s|%s]\n **[console log|%s]",
                    buildNumber, buildURL, buildURL.concat("console"));
            String issueId = getIssue(filename);

            //if issue exists it will check the status and comment or delete the file accordingly
            if (issueId != null) {
                try {
                    String status = getStatus(build, issueId);

                    //Status=1=Open OR Status=5=Resolved
                    if (status.equals("1") || status.equals("5")) {
                        addComment(build, issueId, comment);
                    }

                    //if issue is in closed status
                    if (status.equals("6")) {
                        deleteFile(filename, listener);
                    }
                } catch (ServiceException e) {
                    listener.error("Error updating JIRA issue " + issueId + " : " + e.getMessage());
                    LOG.warning("Error updating JIRA issue " + issueId + "\n" + e);
                }
            }
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
            return "Create Jira Issue";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help-jira-create-issue.html";
        }
    }
}