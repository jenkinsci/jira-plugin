package hudson.plugins.jira;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.thoughtworks.xstream.XStream;
import hudson.plugins.jira.JiraProjectProperty.DescriptorImpl;
import hudson.util.XStream2;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JiraGlobalConfigurationTest {
    @Rule
    public final JenkinsRule r = new JenkinsRule();

    @Test
    public void migrateOldConfiguration() throws Exception {
        String url = "https://backwardsCompatURL.com/";
        String oldConfiguration = "<hudson.plugins.jira.JiraProjectProperty_-DescriptorImpl>\n"
            + "  <sites>\n"
            + "    <hudson.plugins.jira.JiraSite>\n"
            + "      <url>" + url + "</url>\n"
            + "      <useHTTPAuth>false</useHTTPAuth>\n"
            + "      <supportsWikiStyleComment>false</supportsWikiStyleComment>\n"
            + "      <recordScmChanges>false</recordScmChanges>\n"
            + "      <disableChangelogAnnotations>false</disableChangelogAnnotations>\n"
            + "      <updateJiraIssueForAllStatus>false</updateJiraIssueForAllStatus>\n"
            + "      <timeout>10</timeout>\n"
            + "      <readTimeout>30</readTimeout>\n"
            + "      <threadExecutorNumber>10</threadExecutorNumber>\n"
            + "      <dateTimePattern></dateTimePattern>\n"
            + "      <appendChangeTimestamp>false</appendChangeTimestamp>\n"
            + "    </hudson.plugins.jira.JiraSite>\n"
            + "  </sites>\n"
            + "</hudson.plugins.jira.JiraProjectProperty_-DescriptorImpl>";

        XStream xstream = new XStream2();

        JiraSite expected = new JiraSite(url);
        DescriptorImpl instance = (DescriptorImpl)xstream.fromXML(oldConfiguration);
        assertNotNull(instance);
        assertNull(instance.sites);
        JiraSite actual = JiraGlobalConfiguration.get().getSites().get(0);
        assertEquals(url, actual.getName());
        r.assertEqualDataBoundBeans(expected, actual);
    }

    @Test
    public void testNullCase() throws Exception {
        DescriptorImpl instance = new DescriptorImpl();
        assertNotNull(instance);
        assertNull(instance.sites);
        instance.readResolve();
        assertEquals(0, JiraGlobalConfiguration.get().getSites().size());
    }
}
