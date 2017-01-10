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

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Stores number of issues matching a JQL query in a environment variable with a configurable name.
 *
 * @author Pawel Kozikowski
 */
public class JiraIssuesSizeEnvironmentVariableBuilder extends Builder implements SimpleBuildStep {
    private final String jqlSearch;
    private final String envVariableName;

    @DataBoundConstructor
    public JiraIssuesSizeEnvironmentVariableBuilder(String jqlSearch, String envVariableName) {
        this.jqlSearch = jqlSearch;
        this.envVariableName = envVariableName;
    }

    /**
     * @return the jql
     */
    public String getJqlSearch() {
        return jqlSearch;
    }

    /**
     * @return the envVariableName
     */
    public String getEnvVariableName() {
        return envVariableName;
    }

    JiraSite getSiteForProject(Job project) {
        return JiraSite.get(project);
    }

    /**
     * Performs saving the number of matching issues to environment variable.
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String realJql = Util.fixEmptyAndTrim(run.getEnvironment(listener).expand(jqlSearch));
        String realEnvVariableName = Util.fixEmptyAndTrim(envVariableName);

        JiraSite site = getSiteForProject(run.getParent());

        if (site == null) {
            throw new AbortException(Messages.NoJiraSite());
        }

        if (site.getSession() == null) {
            throw new AbortException(Messages.FailedToConnect());
        }

        int numberOfIssues = getNumberOfIssuesByJqlQuery(realJql, site);

        listener.getLogger().println(Messages.JiraIssuesSizeEnvironmentVariableBuilder_SettingVariable(realEnvVariableName, numberOfIssues));

        run.addAction(new JiraIssuesSizeEnvironmentContributingAction(numberOfIssues, realEnvVariableName));
    }

    int getNumberOfIssuesByJqlQuery(String realJql, JiraSite site) throws IOException {
        return site.getSession().getIssuesFromJqlSearch(realJql).size();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link JiraIssuesSizeEnvironmentVariableBuilder}.
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
                return FormValidation.error(Messages.JiraIssuesSizeEnvironmentVariableBuilder_NoJqlSearch());
            }

            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form field 'envVariableName'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckEnvVariableName(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error(Messages.JiraIssuesSizeEnvironmentVariableBuilder_NoEnvVariableName());
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
            return Messages.JiraIssuesSizeEnvironmentVariableBuilder_DisplayName();
        }
    }
}
