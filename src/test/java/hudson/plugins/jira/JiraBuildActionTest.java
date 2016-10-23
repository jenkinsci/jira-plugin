package hudson.plugins.jira;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;

import hudson.plugins.jira.model.JiraIssue;
import org.junit.Test;

import hudson.model.FreeStyleBuild;
import hudson.plugins.jira.deprecated.DeprecatedJiraBuildAction;
import hudson.util.XStream2;

/**
 * Test if existing serialized JiraBuildAction information
 * will be loaded after upgrading jira plugin version to with
 * new JiraBuildAction class version introduced in PR-72.
 *
 */
public class JiraBuildActionTest {

    @Test
    public void testBinaryCompatibility() {
        XStream2 xStream2 = new XStream2();

        FreeStyleBuild b = mock(FreeStyleBuild.class);
        DeprecatedJiraBuildAction deprecatedJiraBuildAction = new DeprecatedJiraBuildAction(b, new ArrayList<JiraIssue>());

        String xml = xStream2.toXML(deprecatedJiraBuildAction);
        System.out.println(xml);
        xml = xml.replaceAll("hudson.plugins.jira.deprecated.DeprecatedJiraBuildAction", "hudson.plugins.jira.JiraBuildAction");
        Object fromXML = xStream2.fromXML(xml);
        System.out.println(fromXML.getClass().getName());
        JiraBuildAction jiraBuildAction = (JiraBuildAction) fromXML;
        System.out.println(jiraBuildAction.getDisplayName());
    }

}
