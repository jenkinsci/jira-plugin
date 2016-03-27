package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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

    enum finishedStatuses {
        Closed,
        Done,
        Resolved
    }

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

        String jobDirPath = build.getProject().getBuildDir().getPath();
        String filename = jobDirPath + File.separator + "issue.txt";

        EnvVars vars = build.getEnvironment(TaskListener.NULL);

        Result currentBuildResult = build.getResult();

        Result previousBuildResult = null;
        AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();

        if (previousBuild != null) {
            previousBuildResult = previousBuild.getResult();
        }

        if (currentBuildResult != Result.ABORTED && previousBuild != null) {
            if (currentBuildResult == Result.FAILURE) {
                currentBuildResultFailure(build, listener, previousBuildResult, filename, vars);
            }

            if (currentBuildResult == Result.SUCCESS) {
                currentBuildResultSuccess(build, listener, previousBuildResult, filename, vars);
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
     * @throws IOException
     * @throws InterruptedException
     */
    private Issue createJiraIssue(AbstractBuild<?, ?> build, String filename) throws IOException, InterruptedException {

        EnvVars vars = build.getEnvironment(TaskListener.NULL);
        JiraSession session = getJiraSession(build);

        String buildName = getBuildName(vars);
        String summary = String.format("Build %s failed", buildName);
        String description = String.format(
                "%s\n\nThe build %s has failed.\nFirst failed run: %s",
                (this.testDescription.equals("")) ? "No description is provided" : this.testDescription,
                buildName,
                getBuildDetailsString(vars)
        );
        Iterable<String> components = Splitter.on(",").trimResults().omitEmptyStrings().split(component);

        Issue issue = session.createIssue(projectKey, description, assignee, components, summary);

        writeInFile(filename, issue);
        return issue;
    }

    /**
     * Returns the status of the issue.
     *
     * @param build
     * @param id
     * @return Status of the issue
     * @throws IOException
     */
    private Status getStatus(AbstractBuild<?, ?> build, String id) throws IOException {

        JiraSession session = getJiraSession(build);
        Issue issue = session.getIssueByKey(id);
        return issue.getStatus();
    }

    /**
     * Adds a comment to the existing issue.
     *
     * @param build
     * @param id
     * @param comment
     * @throws IOException
     */
    private void addComment(AbstractBuild<?, ?> build, String id, String comment) throws IOException {

        JiraSession session = getJiraSession(build);
        session.addCommentWithoutConstrains(id, comment);
    }

    /**
     * Returns an Array of componets given by the user
     *
     * @param build
     * @param component
     * @return Array of component
     * @throws IOException
     */
    private List<BasicComponent> getJiraComponents(AbstractBuild<?, ?> build, String component) throws IOException {

        if (Util.fixEmpty(component) == null) {
            return Collections.emptyList();
        }

        JiraSession session = getJiraSession(build);
        List<Component> availableComponents = session.getComponents(projectKey);

        //converting the user input as a string array
        Splitter splitter = Splitter.on(",").trimResults().omitEmptyStrings();
        List<String> inputComponents = Lists.newArrayList(splitter.split(component));
        int numberOfComponents = inputComponents.size();

        final List<BasicComponent> jiraComponents = new ArrayList<BasicComponent>(numberOfComponents);

        for (final BasicComponent availableComponent : availableComponents) {
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
            return null;
        } finally {
            if (br != null) {
                br.close();
            }
        }

    }

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }

    /**
     * Returns the jira session.
     *
     * @param build
     * @return JiraSession
     * @throws IOException
     */
    private JiraSession getJiraSession(AbstractBuild<?, ?> build) throws IOException {

        JiraSite site = getSiteForProject(build.getProject());

        if (site == null) {
            throw new IllegalStateException("JIRA site needs to be configured in the project " + build.getFullDisplayName());
        }

        JiraSession session = site.getSession();
        if (session == null) {
            throw new IllegalStateException("Remote access for JIRA isn't configured in Jenkins");
        }

        return session;
    }

    /**
     * @param filename
     */
    private void deleteFile(String filename) {
        File file = new File(filename);
        if (file.exists() && !file.delete()) {
            LOG.warning("WARNING: couldn't delete file: " + filename);
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
                                           String filename, EnvVars vars) throws InterruptedException, IOException {

        if (previousBuildResult == Result.FAILURE) {
            String comment = String.format("Build is still failing.\nFailed run: %s", getBuildDetailsString(vars));

            //Get the issue-id which was filed when the previous built failed
            String issueId = getIssue(filename);
            if (issueId != null) {
                try {
                    //The status of the issue which was filed when the previous build failed
                    Status status = getStatus(build, issueId);

                    // Issue Closed, need to open new one
                    if  (   status.getName().equalsIgnoreCase(finishedStatuses.Closed.toString()) ||
                            status.getName().equalsIgnoreCase(finishedStatuses.Done.toString()) ) {

                        listener.getLogger().println("The previous build also failed but the issue is closed");
                        deleteFile(filename);
                        Issue issue = createJiraIssue(build, filename);
                        LOG.info(String.format("[%s] created.", issue.getKey()));

                    }else {
                        addComment(build, issueId, comment);
                        LOG.info(String.format("[%s] The previous build also failed, comment added.", issueId));
                    }
                } catch (IOException e) {
                    LOG.warning(String.format("[%s] - error processing JIRA change: %s", issueId, e.getMessage()));
                }
            }
        }

        if (previousBuildResult == Result.SUCCESS || previousBuildResult == Result.ABORTED) {
            try {
                Issue issue = createJiraIssue(build, filename);
                listener.getLogger().println("Build failed, created JIRA issue " + issue.getKey());
            } catch (IOException e) {
                listener.error("Error creating JIRA issue : " + e.getMessage());
                LOG.warning("Error creating JIRA issue\n" + e.getMessage());
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
     * @param vars
     * @throws InterruptedException
     * @throws IOException
     */
    private void currentBuildResultSuccess(AbstractBuild<?, ?> build, BuildListener listener, Result previousBuildResult,
                                           String filename, EnvVars vars) throws InterruptedException, IOException {

        if (previousBuildResult == Result.FAILURE || previousBuildResult == Result.SUCCESS) {
            String comment = String.format("Previously failing build now is OK.\n Passed run: %s", getBuildDetailsString(vars));
            String issueId = getIssue(filename);

            //if issue exists it will check the status and comment or delete the file accordingly
            if (issueId != null) {
                try {
                    Status status = getStatus(build, issueId);

                    //if issue is in closed status
                    if  (   status.getName().equalsIgnoreCase(finishedStatuses.Closed.toString()) ||
                            status.getName().equalsIgnoreCase(finishedStatuses.Done.toString()) ) {
                        LOG.info(String.format("%s is closed", issueId));
                        deleteFile(filename);
                    } else {
                        LOG.info(String.format("%s is not Closed, comment was added.", issueId));
                        addComment(build, issueId, comment);
                    }

                } catch (IOException e) {
                    listener.error("Error updating JIRA issue " + issueId + " : " + e.getMessage());
                    LOG.warning("Error updating JIRA issue " + issueId + "\n" + e);
                }
            }

        }
    }

    /**
     * Returns build details string in wiki format, with hyperlinks.
     *
     * @param vars
     * @return
     */
    private String getBuildDetailsString(EnvVars vars){
        final String buildURL = vars.get("BUILD_URL");
        return String.format("[%s|%s] [console log|%s]", getBuildName(vars), buildURL, buildURL.concat("console"));
    }

    /**
     * Returns build name in format BUILD#10
     *
     * @param vars
     * @return String
     */
    private String getBuildName(EnvVars vars){
        final String jobName = vars.get("JOB_NAME");
        final String buildNumber = vars.get("BUILD_NUMBER");
        return String.format("%s #%s", jobName, buildNumber);
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(JiraCreateIssueNotifier.class);
        }

        public FormValidation doCheckProjectKey(@QueryParameter String value) throws IOException {
            if (value.length() == 0) {
                return FormValidation.error("Please set the project key");
            }
            return FormValidation.ok();
        }

        @Override
        public JiraCreateIssueNotifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(JiraCreateIssueNotifier.class, formData);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraCreateIssueNotifier_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help-jira-create-issue.html";
        }
    }
}