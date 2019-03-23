package hudson.plugins.jira;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.cloudbees.hudson.plugins.folder.Folder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JiraFolderPropertyTest {
    @Rule
    public final JenkinsRule r = new JenkinsRule();

    @Test
    public void configRoundtrip() throws Exception {
        Folder d = r.jenkins.createProject(Folder.class, "d");
        r.configRoundtrip(d);
        assertNull(d.getProperties().get(JiraFolderProperty.class));
        List<JiraSite> list = new ArrayList<>();
        list.add(new JiraSite("https://test.com"));
        JiraFolderProperty foo = new JiraFolderProperty();
        foo.setSites(list);
        foo.setSites(new JiraSite("https://otherTest.com"));
        d.getProperties().add(foo);
        r.configRoundtrip(d);
        JiraFolderProperty prop = d.getProperties().get(JiraFolderProperty.class);
        assertNotNull(prop);
        List<JiraSite> actual = Arrays.asList(prop.getSites());
        r.assertEqualDataBoundBeans(list, actual);
    }
}
