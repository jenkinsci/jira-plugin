package hudson.plugins.jira;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.jira.model.JiraIssue;
import jenkins.branch.MultiBranchProject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JiraJobAction is to store a reference to the {@link JiraIssue} that represents work
 * being done for a {@link WorkflowJob} (branch or PR) belonging to a {@link MultiBranchProject}
 *
 * Any branches with the whole key in the name or after a prefix will have this action attached.
 * e.g. "JENKINS-1234" and "feature/JENKINS-1234" will have this action with the issue JENKINS-1234 referenced
 */
@ExportedBean
public class JiraJobAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(JiraJobAction.class.getName());

    private final Job<?, ?> owner;
    private final JiraIssue issue;

    @DataBoundConstructor
    public JiraJobAction(Job<?, ?> owner, JiraIssue issue) {
        this.owner = owner;
        this.issue = issue;
    }

    /**
     * @return issue representing the job
     */
    @Exported
    public JiraIssue getIssue() {
        return issue;
    }

    /**
     * @return url of the JIRA server
     */
    @Exported
    @Nullable
    public String getServerURL() {
        JiraSite jiraSite = JiraSite.get(owner);
        URL url = jiraSite != null ? jiraSite.getUrl() : null;
        return url != null ? url.toString() : null;
    }

    /**
     * Adds a {@link JiraJobAction} to a {@link WorkflowJob} if it belongs to a {@link MultiBranchProject}
     * and its name contains an JIRA issue key
     * @param job to add the property to
     * @param site to fetch issue data
     * @throws IOException if something goes wrong fetching the JIRA issue
     */
    public static void setAction(@Nonnull Job<?, ?> job, @Nonnull JiraSite site) throws IOException {
        // If there is already a action set then skip
        if (job.getAction(JiraJobAction.class) != null) {
            return;
        }

        // Exclude all non-multibranch workflow jobs
        if (!(job.getParent() instanceof MultiBranchProject)) {
            return;
        }

        // Find the first JIRA issue key in the branch name
        // If it exists, create the action and set it
        Pattern pattern = site.getIssuePattern();
        // Pipeline will URL encode job names if the branch or PR name contains a '/' or other non-URL safe characters
        String decodedJobName = URLDecoder.decode(job.getName(), "UTF-8");
        String issueKey = null;
        Matcher m = pattern.matcher(decodedJobName);
        while (m.find()) {
            if (m.groupCount() >= 1) {
                issueKey = m.group(1);
                break;
            }
        }

        if (issueKey != null) {
            JiraIssue issue = site.getIssue(issueKey);
            if (issue != null) {
                job.addAction(new JiraJobAction(job, issue));
                job.save();
            }
        }
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "JIRA";
    }

    @Override
    public String getUrlName() {
        return "jira";
    }

    @Extension
    public static final class RunListenerImpl extends RunListener<WorkflowRun> {
        @Override
        public void onStarted(WorkflowRun workflowRun, TaskListener listener) {
            WorkflowJob parent = workflowRun.getParent();
            JiraSite site = JiraSite.get(parent);
            if (site != null) {
                try {
                    setAction(parent, site);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not set JiraJobAction for <" + parent.getFullName() + ">", e);
                }
            }
        }
    }
}
