/*
 * Copyright 2011-2012 Insider Guides, Inc., MeetMe, Inc.
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
package hudson.plugins.jira.listissuesparameter;

import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static hudson.Util.fixNull;

public class JiraIssueParameterDefinition extends ParameterDefinition {
    private static final long serialVersionUID = 3927562542249244416L;

    private String jiraIssueFilter;

    @DataBoundConstructor
    public JiraIssueParameterDefinition(String name, String description, String jiraIssueFilter) {
        super(name, description);

        this.jiraIssueFilter = jiraIssueFilter;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String[] values = req.getParameterValues(getName());
        if (values == null || values.length != 1) {
            return null;
        }

        return new JiraIssueParameterValue(getName(), values[0]);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject formData) {
        JiraIssueParameterValue value = req.bindJSON(
                JiraIssueParameterValue.class, formData);
        return value;
    }

    @Override
    public ParameterValue createValue(CLICommand command, String value) throws IOException, InterruptedException {
        return new JiraIssueParameterValue(getName(), value);
    }

    public List<JiraIssueParameterDefinition.Result> getIssues() throws IOException, TimeoutException {
        Job<?, ?> context = Stapler.getCurrentRequest().findAncestorObject(Job.class);

        JiraSite site = JiraSite.get(context);
        if (site == null)
            throw new IllegalStateException("JIRA site needs to be configured in the project " + context.getFullDisplayName());

        JiraSession session = site.getSession();
        if (session == null) throw new IllegalStateException("Remote access for JIRA isn't configured in Jenkins");

        List<Issue> issues = session.getIssuesFromJqlSearch(jiraIssueFilter);

        List<Result> issueValues = new ArrayList<>();

        for (Issue issue : fixNull(issues)) {
            issueValues.add(new Result(issue));
        }

        return issueValues;
    }

    public String getJiraIssueFilter() {
        return jiraIssueFilter;
    }

    public void setJiraIssueFilter(String jiraIssueFilter) {
        this.jiraIssueFilter = jiraIssueFilter;
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "JIRA Issue Parameter";
        }
    }

    public static class Result {
        public final String key;
        public final String summary;

        public Result(final Issue issue) {
            this.key = issue.getKey();
            this.summary = issue.getSummary();
        }
    }
}
