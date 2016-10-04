/*
 * Copyright 2015 Hao Cheng Lee
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

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.api.domain.input.VersionInput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

public class JiraRestService {

    private static final Logger LOGGER = Logger.getLogger(JiraRestService.class.getName());

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    /**
     * Base URI path for a REST API call. It must be relative to site's base
     * URI.
     */
    public static final String BASE_API_PATH = "rest/api/2";

    private final URI uri;

    private final JiraRestClient jiraRestClient;

    private final ObjectMapper objectMapper;

    private final String authHeader;

    private final String baseApiPath;
    
    private final int timeout;

    @Deprecated
    public JiraRestService(URI uri, JiraRestClient jiraRestClient, String username, String password) {
    	this(uri, jiraRestClient, username, password, JiraSite.DEFAULT_TIMEOUT);
    }

    public JiraRestService(URI uri, JiraRestClient jiraRestClient, String username, String password, int timeout) {
        this.uri = uri;
        this.objectMapper = new ObjectMapper();
        this.timeout = timeout;
        final String login = username + ":" + password;
        try {
            byte[] encodeBase64 = Base64.encodeBase64(login.getBytes("UTF-8"));
            this.authHeader = "Basic " + new String(encodeBase64, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("jira rest encode username:password error. cause: " + e.getMessage());
            throw new RuntimeException("failed to encode username:password using Base64");
        }
        this.jiraRestClient = jiraRestClient;

        final StringBuilder builder = new StringBuilder();
        if (uri.getPath() != null) {
            builder.append(uri.getPath());
            if (!uri.getPath().endsWith("/")) {
                builder.append('/');
            }
        } else {
            builder.append('/');
        }
        builder.append(BASE_API_PATH);
        baseApiPath = builder.toString();
    }

    public void addComment(String issueId, String commentBody,
                                         String groupVisibility, String roleVisibility) {
        final URIBuilder builder = new URIBuilder(uri)
                .setPath(String.format("%s/issue/%s/comment", baseApiPath, issueId));

        final Comment comment;
        if (StringUtils.isNotBlank(groupVisibility)) {
            comment = Comment.createWithGroupLevel(commentBody, groupVisibility);
        } else if (StringUtils.isNotBlank(roleVisibility)) {
            comment = Comment.createWithRoleLevel(commentBody, roleVisibility);
        } else {
            comment = Comment.valueOf(commentBody);
        }

      try {
          jiraRestClient.getIssueClient().addComment(builder.build(), comment).get(timeout, TimeUnit.SECONDS);
      } catch (Exception e) {
          LOGGER.log(WARNING, "jira rest client add comment error. cause: " + e.getMessage(), e);
      }
    }
  
    public Issue getIssue(String issueKey) {
        try {
            return jiraRestClient.getIssueClient().getIssue(issueKey).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get issue error. cause: " + e.getMessage(), e);
            return null;
        }
    }

    public List<IssueType> getIssueTypes() {
        try {
            return Lists.newArrayList(jiraRestClient.getMetadataClient().getIssueTypes().get(timeout, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get issue types error. cause: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<String> getProjectsKeys() {
        Iterable<BasicProject> projects = Collections.emptyList();
        try {
            projects = jiraRestClient.getProjectClient().getAllProjects().get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get project keys error. cause: " + e.getMessage(), e);
        }
        final List<String> keys = new ArrayList<String>();
        for (BasicProject project : projects) {
            keys.add(project.getKey());
        }
        return keys;
    }

    public List<Issue> getIssuesFromJqlSearch(String jqlSearch, Integer maxResults) {
        try {
            final SearchResult searchResult = jiraRestClient.getSearchClient()
                                                            .searchJql(jqlSearch, maxResults, 0, null)
                                                            .get(timeout, TimeUnit.SECONDS);
            return Lists.newArrayList(searchResult.getIssues());
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get issue from jql search error. cause: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Version> getVersions(String projectKey) {
        final URIBuilder builder = new URIBuilder(uri)
                .setPath(String.format("%s/project/%s/versions", baseApiPath, projectKey));

        List<Map<String, Object>> decoded = Collections.emptyList();
        try {
            URI uri = builder.build();
            final Content content = buildGetRequest(uri)
                .execute()
                .returnContent();

            decoded = objectMapper.readValue(content.asString(), new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get versions error. cause: " + e.getMessage(), e);
        }

        final List<Version> versions = new ArrayList<Version>();
        for (Map<String, Object> decodedVersion : decoded) {
            final DateTime releaseDate = decodedVersion.containsKey("releaseDate") ? DATE_TIME_FORMATTER.parseDateTime((String) decodedVersion.get("releaseDate")) : null;
            final Version version = new Version(URI.create((String) decodedVersion.get("self")), Long.parseLong((String) decodedVersion.get("id")),
                    (String) decodedVersion.get("name"), (String) decodedVersion.get("description"), (Boolean) decodedVersion.get("archived"),
                    (Boolean) decodedVersion.get("released"), releaseDate);
            versions.add(version);
        }
        return versions;
    }


    public Version addVersion(String projectKey, String versionName) {
        final VersionInput versionInput = new VersionInput(projectKey, versionName, null, null, false, false);
        try {
            return jiraRestClient.getVersionRestClient()
                          .createVersion(versionInput).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client add version error. cause: " + e.getMessage(), e);
            return null;
        }
    }

    public void releaseVersion(String projectKey, Version version) {
        final URIBuilder builder = new URIBuilder(uri)
            .setPath(String.format("%s/version/%s", baseApiPath, version.getId()));

        final VersionInput versionInput = new VersionInput(projectKey, version.getName(), version.getDescription(), version
            .getReleaseDate(), version.isArchived(), version.isReleased());

        try {
            jiraRestClient.getVersionRestClient().updateVersion(builder.build(), versionInput).get(timeout, TimeUnit.SECONDS);
        }catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client release version error. cause: " + e.getMessage(), e);
        }
    }

    public BasicIssue createIssue(String projectKey, String description, String assignee, Iterable<String> components, String summary) {
        IssueInputBuilder builder = new IssueInputBuilder();
        builder.setProjectKey(projectKey)
                .setDescription(description)
                .setIssueTypeId(1L) // BUG
                .setSummary(summary);

        if (!assignee.equals(""))
            builder.setAssigneeName(assignee);
        if (Iterators.size(components.iterator()) > 0){
            builder.setComponentsNames(components);
        }

        final IssueInput issueInput = builder.build();

        try {
            return jiraRestClient.getIssueClient().createIssue(issueInput).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "JIRA REST createIssue error: " + e.getMessage(), e);
            return null;
        }
    }

    public User getUser(String username) {
        try {
            return jiraRestClient.getUserClient().getUser(username).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get user error. cause: " + e.getMessage(), e);
            return null;
        }
    }
 
    public void updateIssue(String issueKey, List<Version> fixVersions) {
        final IssueInput issueInput = new IssueInputBuilder().setFixVersions(fixVersions)
                                                             .build();
        try {
            jiraRestClient.getIssueClient().updateIssue(issueKey, issueInput).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client update issue error. cause: " + e.getMessage(), e);
        }
    }
    
    public void setIssueLabels(String issueKey, List<String> labels) {
        final IssueInput issueInput = new IssueInputBuilder()
        		.setFieldValue(IssueFieldId.LABELS_FIELD.id, labels)
                .build();
        try {
            jiraRestClient.getIssueClient().updateIssue(issueKey, issueInput).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client update labels error for issue "+issueKey, e);
        }
    }    
    
    public Issue progressWorkflowAction(String issueKey, Integer actionId) {
        final TransitionInput transitionInput = new TransitionInput(actionId);

        final Issue issue = getIssue(issueKey);

        try {
            jiraRestClient.getIssueClient().transition(issue, transitionInput).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client process workflow action error. cause: " + e.getMessage(), e);
        }
        return issue;
    }

    public List<Transition> getAvailableActions(String issueKey) {
        final Issue issue = getIssue(issueKey);

        try {
            final Iterable<Transition> transitions = jiraRestClient.getIssueClient()
                                                                   .getTransitions(issue)
                                                                   .get(timeout, TimeUnit.SECONDS);
            return Lists.newArrayList(transitions);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get available actions error. cause: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Status> getStatuses() {
        try {
            final Iterable<Status> statuses = jiraRestClient.getMetadataClient().getStatuses()
                                                            .get(timeout, TimeUnit.SECONDS);
            return Lists.newArrayList(statuses);
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client get statuses error. cause: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Component> getComponents(String projectKey) {
        final URIBuilder builder = new URIBuilder(uri)
            .setPath(String.format("%s/project/%s/components", baseApiPath, projectKey));

        try {
            final Content content = buildGetRequest(builder.build()).execute().returnContent();
            final List<Map<String, Object>> decoded = objectMapper.readValue(content.asString(),
                new TypeReference<List<Map<String, Object>>>() {
            });

            final List<Component> components = new ArrayList<Component>();
            for (final Map<String, Object> decodeComponent : decoded) {
                BasicUser lead = null;
                if (decodeComponent.containsKey("lead")) {
                    final Map<String, Object> decodedLead = (Map<String, Object>) decodeComponent.get("lead");
                    lead = new BasicUser(URI.create((String) decodedLead.get("self")), (String) decodedLead.get("name"), (String) decodedLead
                        .get("displayName"));
                }
                final Component component = new Component(
                    URI.create((String) decodeComponent.get("self")),
                    Long.parseLong((String) decodeComponent.get("id")),
                    (String) decodeComponent.get("name"),
                    (String) decodeComponent.get("description"),
                    lead);
                components.add(component);
            }

            return components;
        } catch (Exception e) {
            LOGGER.log(WARNING, "jira rest client process workflow action error. cause: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Request buildGetRequest(URI uri) {
        return Request.Get(uri)
                .connectTimeout(timeoutInMiliseconds())
                .socketTimeout(timeoutInMiliseconds())
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json");
    }

	protected int timeoutInMiliseconds() {
		return (int) TimeUnit.SECONDS.toMillis(timeout);
	}

    public String getBaseApiPath() {
        return baseApiPath;
    }

    /**
     * Get User's permissions
     *
     */
    public Permissions getMyPermissions() throws RestClientException {
        return jiraRestClient.getMyPermissionsRestClient().getMyPermissions(null).claim();
    }
}