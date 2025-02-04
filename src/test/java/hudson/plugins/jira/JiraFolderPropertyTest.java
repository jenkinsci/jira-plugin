package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class JiraFolderPropertyTest {

    @Test
    void configRoundtrip(JenkinsRule r) throws Exception {
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

    public static CredentialsStore getFolderStore(Folder f) {
        return StreamSupport.stream(CredentialsProvider.lookupStores(f).spliterator(), false)
                .filter(s -> s.getProvider() instanceof FolderCredentialsProvider && s.getContext() == f)
                .findFirst()
                .orElse(null);
    }
}
