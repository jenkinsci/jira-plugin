import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import hudson.plugins.jira.JiraRestService;
import hudson.plugins.jira.JiraSite;

import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Test bed to play with JIRA.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraTester {
    public static void main(String[] args) throws Exception {

        final URI uri = new URL(JiraConfig.getUrl()).toURI();
        final JiraRestClient jiraRestClient = new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(uri, JiraConfig.getUsername(), JiraConfig.getPassword());

        final JiraRestService restService = new JiraRestService(uri, jiraRestClient, JiraConfig.getUsername(), JiraConfig.getPassword(), JiraSite.DEFAULT_TIMEOUT);

        final String projectKey = "TESTPROJECT";
        final String issueId = "TESTPROJECT-425";
        final Integer actionId = 21;

        final Issue issue = restService.getIssue(issueId);
        System.out.println("issue:" + issue);


        final List<Transition> availableActions = restService.getAvailableActions(issueId);
        for (Transition action : availableActions) {
            System.out.println("Action:" + action);
        }

        for (IssueType issueType : restService.getIssueTypes()) {
            System.out.println(" issue type: " + issueType);
        }

//        restService.addVersion("TESTPROJECT", "0.0.2");

        final List<Component> components = restService.getComponents(projectKey);
        for (Component component : components) {
            System.out.println("component: " + component);
        }

//        BasicComponent backendComponent = null;
//        final Iterable<BasicComponent> components1 = Lists.newArrayList(backendComponent);
//        restService.createIssue("TESTPROJECT", "This is a test issue created using JIRA jenkins plugin. Please ignore it.", "TESTUSER", components1, "test issue from JIRA jenkins plugin");

        final List<Issue> searchResults = restService.getIssuesFromJqlSearch("project = \"TESTPROJECT\"", 3);
        for (Issue searchResult : searchResults) {
            System.out.println("JQL search result: " + searchResult);
        }

        final List<String> projectsKeys = restService.getProjectsKeys();
        for (String projectsKey : projectsKeys) {
            System.out.println("project key: " + projectsKey);
        }

        final List<Status> statuses = restService.getStatuses();
        for (Status status : statuses) {
            System.out.println("status:" + status);
        }

        final User user = restService.getUser("TESTUSER");
        System.out.println("user: " + user);

        final List<Version> versions = restService.getVersions(projectKey);
        for (Version version : versions) {
            System.out.println("version: "  + version);
        }

//        Version releaseVersion = new Version(version.getSelf(), version.getId(), version.getName(),
//                version.getDescription(), version.isArchived(), true, new DateTime());
//        System.out.println(" >>>> release version 0.0.2");
//        restService.releaseVersion("TESTPROJECT", releaseVersion);

//        System.out.println(" >>> update issue TESTPROJECT-425");
//        restService.updateIssue(issueId, Collections.singletonList(releaseVersion));

//        final Issue updatedIssue = restService.progressWorkflowAction(issueId, actionId);
//        System.out.println("Updated issue:" + updatedIssue);



        for(int i=0;i<10;i++){
            callUniq( restService );
        }

        for(int i=0;i<10;i++){
            callDuplicate( restService );
        }

    }

    private static void callUniq(final JiraRestService restService) throws Exception {
        long start = System.currentTimeMillis();
        List<Issue> issues = restService.getIssuesFromJqlSearch( "key in ('JENKINS-53320','JENKINS-51057')", Integer.MAX_VALUE );
        long end = System.currentTimeMillis();
        System.out.println( "time uniq " + (end -start) );
    }

    private static void callDuplicate(final JiraRestService restService) throws Exception {
        long start = System.currentTimeMillis();
        List<Issue> issues = restService.getIssuesFromJqlSearch( "key in ('JENKINS-53320','JENKINS-53320','JENKINS-53320','JENKINS-53320','JENKINS-53320','JENKINS-51057','JENKINS-51057','JENKINS-51057','JENKINS-51057','JENKINS-51057')", Integer.MAX_VALUE );
        long end = System.currentTimeMillis();
        System.out.println( "time duplicate " + (end -start) );
    }

}
