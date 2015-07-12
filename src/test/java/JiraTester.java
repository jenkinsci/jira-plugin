import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import hudson.plugins.jira.JiraRestService;

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

        final JiraRestService restService = new JiraRestService(uri, jiraRestClient, JiraConfig.getUsername(), JiraConfig.getPassword());

        String issueId = "TESTPROJECT-60";
        Integer actionId = 21;

        final Issue issue = restService.getIssue(issueId);
        System.out.println("issue:" + issue);


        final List<Transition> availableActions = restService.getAvailableActions(issueId);
        for (Transition action : availableActions) {
            System.out.println("Action:" + action);
        }


//        final Issue updatedIssue = restService.progressWorkflowAction(issueId, actionId);
//        System.out.println("Updated issue:" + updatedIssue);

    }
}
