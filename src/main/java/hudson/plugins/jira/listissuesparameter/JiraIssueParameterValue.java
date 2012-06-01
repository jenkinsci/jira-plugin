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

import hudson.EnvVars;
import hudson.model.ParameterValue;
import hudson.model.AbstractBuild;
import hudson.util.VariableResolver;

import org.kohsuke.stapler.DataBoundConstructor;

public class JiraIssueParameterValue extends ParameterValue {
	private static final long serialVersionUID = -1078274709338167211L;

	private String issue;

	@DataBoundConstructor
	public JiraIssueParameterValue(final String name, final String issue) {
		super(name);
		this.issue = issue;
	}

	@Override
	public void buildEnvVars(final AbstractBuild<?, ?> build, final EnvVars env) {
		env.put(getName(), getIssue());
	}

	@Override
	public VariableResolver<String> createVariableResolver(
			final AbstractBuild<?, ?> build) {
		return new VariableResolver<String>() {
			public String resolve(final String name) {
				return JiraIssueParameterValue.this.name.equals(name) ? getIssue()
						: null;
			}
		};
	}

	public void setIssue(final String issue) {
		this.issue = issue;
	}

	public String getIssue() {
		return issue;
	}

    @Override
    public String toString() {
        return "(JiraIssueParameterValue) " + getName() + "='" + issue + "'";
    }
}
