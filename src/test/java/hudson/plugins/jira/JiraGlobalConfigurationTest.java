package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.*;

import com.thoughtworks.xstream.XStream;
import hudson.plugins.jira.JiraProjectProperty.DescriptorImpl;
import hudson.util.XStream2;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JiraGlobalConfigurationTest {

    @Test
    void migrateOldConfiguration(JenkinsRule r) throws Exception {
        String url = "https://backwardsCompatURL.com/";
        XStream xstream = new XStream2();
        JiraSite expected = new JiraSite(url);
        InputStream resource = getClass().getResourceAsStream("oldJiraProjectProperty.xml");
        DescriptorImpl instance = (DescriptorImpl) xstream.fromXML(resource);
        assertNotNull(instance);
        assertNull(instance.sites);
        JiraSite actual = JiraGlobalConfiguration.get().getSites().get(0);
        assertEquals(url, actual.getName());
        r.assertEqualDataBoundBeans(expected, actual);
    }
}
