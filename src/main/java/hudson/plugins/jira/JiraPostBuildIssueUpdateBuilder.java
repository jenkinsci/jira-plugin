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
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Build step that will mass-update all issues matching a JQL query, using the specified workflow
 * action name (e.g., "Resolve Issue", "Close Issue").
 *
 * @author Joe Hansche jhansche@myyearbook.com
 */
public class JiraPostBuildIssueUpdateBuilder extends Recorder implements SimpleBuildStep {
    private final String jqlSearch;
    private final String workflowActionName;
    private final String comment;

    @DataBoundConstructor
    public JiraPostBuildIssueUpdateBuilder(String jqlSearch, String workflowActionName, String comment) {
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
        String realWorkflowActionName = Util.fixEmptyAndTrim(run.getEnvironment(listener).expand(workflowActionName));

        JiraSite site = getSiteForJob(run.getParent());

        if (site == null) {
            listener.getLogger().println(Messages.NoJiraSite());
            run.setResult(Result.FAILURE);
            return;
        }

        if (StringUtils.isNotEmpty(realWorkflowActionName)) {
            listener.getLogger().println(Messages.JiraIssueUpdateBuilder_UpdatingWithAction(realWorkflowActionName));
        }

        try {
            if (!site.progressMatchingIssuesPostBuild(listener, run, realWorkflowActionName, realComment, listener.getLogger())) {
                listener.getLogger().println(Messages.JiraIssueUpdateBuilder_SomeIssuesFailed());
                run.setResult(Result.UNSTABLE);
            }
        } catch (TimeoutException e) {
            listener.getLogger().println(Messages.JiraIssueUpdateBuilder_Failed());
            e.printStackTrace(listener.getLogger());
            run.setResult(Result.FAILURE);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Descriptor for {@link JiraPostBuildIssueUpdateBuilder}.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
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
