/*
 * Copyright 2012 MeetMe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.UNSTABLE;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Build step that will mass-update all issues matching a JQL query, using the specified workflow
 * action name (e.g., "Resolve Issue", "Close Issue").
 *
 * @author Joe Hansche jhansche@myyearbook.com
 */
public class JiraIssueUpdateBuilder extends Builder implements SimpleBuildStep {
    private static final Logger LOGGER = Logger.getLogger(JiraIssueUpdateBuilder.class.getName());

    private final String jqlSearch;
    private final String workflowActionName;
    private final String comment;

    @DataBoundConstructor
    public JiraIssueUpdateBuilder(String jqlSearch, String workflowActionName, String comment) {
        this.jqlSearch = Util.fixEmptyAndTrim(jqlSearch);
        this.workflowActionName = Util.fixEmptyAndTrim(workflowActionName);
        this.comment = Util.fixEmptyAndTrim(comment);
    }

    /**
     * @return the jql
     */
    public String getJqlSearch() {
        return jqlSearch;
    }

    /**
     * @return the workflowActionName
     */
    public String getWorkflowActionName() {
        return workflowActionName;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    JiraSite getSiteForJob(Job<?, ?> job) {
        return JiraSite.get(job);
    }

    /**
     * Performs the actual update based on job configuration.
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String realComment = Util.fixEmptyAndTrim(run.getEnvironment(listener).expand(comment));
        String realJql = Util.fixEmptyAndTrim(run.getEnvironment(listener).expand(jqlSearch));
        String realWorkflowActionName = Util.fixEmptyAndTrim(run.getEnvironment(listener).expand(workflowActionName));

        JiraSite site = getSiteForJob(run.getParent());

        if (site == null) {
            listener.getLogger().println(Messages.NoJiraSite());
            run.setResult(FAILURE);
            return;
        }

        if (StringUtils.isNotEmpty(realWorkflowActionName)) {
            listener.getLogger().println(Messages.JiraIssueUpdateBuilder_UpdatingWithAction(realWorkflowActionName));
        }

        listener.getLogger().println("[JIRA] JQL: " + realJql);

        try {
            if (!progressMatchingIssues(site, realJql, realWorkflowActionName, realComment, listener.getLogger())) {
                listener.getLogger().println(Messages.JiraIssueUpdateBuilder_SomeIssuesFailed());
                run.setResult(UNSTABLE);
            }
        } catch (TimeoutException e) {
            listener.getLogger().println(Messages.JiraIssueUpdateBuilder_Failed());
            e.printStackTrace(listener.getLogger());
            run.setResult(FAILURE);
        }
    }

    /**
     * Progresses all issues matching the JQL search, using the given workflow action. Optionally
     * adds a comment to the issue(s) at the same time.
     *
     * @param jqlSearch
     * @param workflowActionName
     * @param comment
     * @param console
     * @throws TimeoutException
     */
    public boolean progressMatchingIssues(
            JiraSite site,
            String jqlSearch,
            String workflowActionName,
            String comment,
            PrintStream console) throws TimeoutException {

        if (isEmpty(workflowActionName)) {
            console.println("[JIRA] No workflow action was specified, " +
                    "thus no status update will be made for any of the matching issues.");
        }

        JiraSession session = site.getSession();

        if (session == null) {
            LOGGER.warning("JIRA session could not be established");
            console.println(Messages.FailedToConnect());
            return false;
        }

        List<Issue> issues = session.getIssuesFromJqlSearch(jqlSearch);

        boolean success = true;
        for (Issue issue : issues) {
            commentIssue(session, issue, comment);
            if (!progressIssue(session, issue, workflowActionName, console)) {
                success = false;
            }
        }

        return success;
    }

    protected void commentIssue(JiraSession session, Issue issue, String comment) {
        if (isNotEmpty(comment)) {
            session.addComment(issue.getKey(), comment, null, null);
        }
    }

    protected boolean progressIssue(
            JiraSession session, Issue issue, String workflowActionName, PrintStream console) {

        String issueKey = issue.getKey();

        if (isEmpty(workflowActionName)) {
            return true;
        }

        Integer actionId = session.getActionIdForIssue(issueKey, workflowActionName);

        if (actionId == null) {
            LOGGER.fine(String.format(
                    "Invalid workflow action '%s' for issue '%s'; issue status = '%s'.",
                    workflowActionName,
                    issueKey,
                    issue.getStatus()));
            console.println(Messages.JiraIssueUpdateBuilder_UnknownWorkflowAction(issueKey, workflowActionName));
            return false;
        }

        String newStatus = session.progressWorkflowAction(issueKey, actionId);

        console.println(String.format("[JIRA] Issue '%s' transitioned to '%s' due to action '%s'.",
                issueKey, newStatus, workflowActionName));

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link JiraIssueUpdateBuilder}.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Performs on-the-fly validation of the form field 'Jql'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckJqlSearch(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(Messages.JiraIssueUpdateBuilder_NoJqlSearch());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckWorkflowActionName(@QueryParameter String value) {
            if (Util.fixNull(value).trim().length() == 0) {
                return FormValidation.warning(Messages.JiraIssueUpdateBuilder_NoWorkflowAction());
            }

            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> klass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.JiraIssueUpdateBuilder_DisplayName();
        }
    }
}
