import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.plugins.jira.soap.RemoteProject;

import java.net.URL;

/**
 * Test bed to play with JIRA.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraTester {
    public static void main(String[] args) throws Exception {
        JiraSoapServiceService jiraSoapServiceGetter = new JiraSoapServiceServiceLocator();

        JiraSoapService service = jiraSoapServiceGetter.getJirasoapserviceV2(
            new URL("http://issues.apache.org/jira/rpc/soap/jirasoapservice-v2"));
        String token = service.login("kohsuke", "kohsuke");

        // key can be used.
        RemoteProject[] projects = service.getProjects(token);
        System.out.println(projects);

        RemoteIssue issue = service.getIssue(token, "HUDSON-1");
        System.out.println(issue);
        issue.getSummary(); // gives you title of the issue
    }
}
