import hudson.plugins.jira.soap.JiraSoapService;
import hudson.plugins.jira.soap.JiraSoapServiceService;
import hudson.plugins.jira.soap.JiraSoapServiceServiceLocator;
import hudson.plugins.jira.soap.RemoteField;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.plugins.jira.soap.RemoteNamedObject;
import hudson.plugins.jira.soap.RemoteProject;
import hudson.plugins.jira.soap.RemoteStatus;

import java.net.URL;

/**
 * Test bed to play with JIRA.
 * 
 * @author Kohsuke Kawaguchi
 */
public class JiraTester {
	public static void main(String[] args) throws Exception {
		JiraSoapServiceService jiraSoapServiceGetter = new JiraSoapServiceServiceLocator();

		JiraSoapService service = jiraSoapServiceGetter
				.getJirasoapserviceV2(new URL(JiraConfig.getUrl()));
		String token = service.login(JiraConfig.getUsername(),
				JiraConfig.getPassword());

		// key can be used.
		// RemoteProject[] projects = service.getProjectsNoSchemes(token);
		// for (RemoteProject p : projects) {
		// System.out.println(p.getKey());
		// }

		String issueId = "TESTPROJEKT-60";
		String actionId = "21";

		RemoteIssue issue = service.getIssue(token, "TESTPROJEKT-61");
		System.out.println("Issue Status: " + issue.getStatus());

		RemoteNamedObject[] actions = service.getAvailableActions(token,
				issueId);
		for (RemoteNamedObject action : actions) {
			System.out.println("Action: " + action.getId() + " - "
					+ action.getName());
		}

		RemoteField[] actionFields = service.getFieldsForAction(token, issueId,
				actionId);
		for (RemoteField actionField : actionFields) {
			System.out.println("ActionField: " + actionField.getId() + " - "
					+ actionField.getName());
		}

		RemoteField[] customFields = service.getCustomFields(token);
		for(RemoteField field:customFields){
			System.out.println("Field: "+field.getId() + " - "+field.getName());
		}

		RemoteIssue updatedIssues = service.progressWorkflowAction(token,
				issueId, actionId, null);
		System.out.println("Issue Status: " + updatedIssues.getStatus());

	}
}
