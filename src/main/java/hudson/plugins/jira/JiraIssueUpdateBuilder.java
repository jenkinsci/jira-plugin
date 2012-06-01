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

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.xml.rpc.ServiceException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Build step that will mass-update all issues matching a JQL query, using the specified workflow
 * action name (e.g., "Resolve Issue", "Close Issue").
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class JiraIssueUpdateBuilder extends Builder {
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

    /**
     * Performs the actual update based on job configuration.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String realComment = Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(comment));
        String realJql = Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(jqlSearch));

        JiraSite site = JiraSite.get(build.getProject());

        if (site == null) {
            listener.getLogger().println(Messages.Updater_NoJiraSite());
            build.setResult(Result.FAILURE);
            return true;
        }

        listener.getLogger().println(Messages.JiraIssueUpdateBuilder_UpdatingWithAction(workflowActionName));
        listener.getLogger().println("[JIRA] JQL: " + realJql);

        try {
            if (!site.progressMatchingIssues(realJql, workflowActionName, realComment, listener.getLogger())) {
                listener.getLogger().println(Messages.JiraIssueUpdateBuilder_SomeIssuesFailed());
                build.setResult(Result.UNSTABLE);
            }
        } catch (ServiceException e) {
            listener.getLogger().println(Messages.JiraIssueUpdateBuilder_Failed());
            e.printStackTrace(listener.getLogger());
            return false;
        }

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
        public FormValidation doCheckJqlSearch(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error(Messages.JiraIssueUpdateBuilder_NoJqlSearch());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckWorkflowActionName(@QueryParameter String value) {
            if (Util.fixNull(value).trim().length() == 0) {
                return FormValidation.error(Messages.JiraIssueUpdateBuilder_NoWorkflowAction());
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
