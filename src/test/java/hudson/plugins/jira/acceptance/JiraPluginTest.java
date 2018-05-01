package hudson.plugins.jira.acceptance;

import java.net.MalformedURLException;
import java.net.URL;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.Since;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.plugins.jira.JiraGlobalConfig;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

/**
 * @author Zhenlei Huang
 */
@WithPlugins("jira")
public class JiraPluginTest extends AbstractJUnitTest {

	@Test
	@Issue("JENKINS-48357")
	public void testDoValidate() throws MalformedURLException {
		String host = jenkins.getCurrentUrl(); // FIXME Shall point to Jira docker instance ?
		String username = "fakeuser";
		String password = "fakepass";

		jenkins.getConfigPage().open();
		JiraGlobalConfig config = new JiraGlobalConfig(jenkins);
		config.addSite(new URL(host), username, password);
		config.clickButton("Validate Settings");
		WebElement error = config.waitFor(By.className("error"));

		assertEquals("Failed to login to JIRA", error.getText());
	}
}
