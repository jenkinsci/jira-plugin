import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;
import hudson.plugins.jira.soap.RemoteComment;
import hudson.plugins.jira.soap.RemoteFieldValue;

import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
public class Foo {
    public static void main(String[] args) throws Exception {
        JiraSoapServiceService jiraSoapServiceGetter = new JiraSoapServiceServiceLocator();

        JiraSoapService service = jiraSoapServiceGetter.getJirasoapserviceV2(new URL(new URL("http://issues.hudson-ci.org/"), "rpc/soap/jirasoapservice-v2"));
        String token = service.login("kohsuke","kohsuke");
        // if an issue doesn't exist an exception will be thrown
        service.getIssue(token, "HUDSON-2916");

        // add comment
        RemoteComment rc = new RemoteComment();
        rc.setBody("testing comment");
        service.addComment(token, "HUDSON-2916", rc);

        // resolve.
        // comment set here doesn't work. see http://jira.atlassian.com/browse/JRA-11278
        service.progressWorkflowAction(token,"HUDSON-2916","5" /*this is apparently the ID for "resolved"*/,
                new RemoteFieldValue[]{new RemoteFieldValue("comment",new String[]{"closing comment"})});
    }
}
