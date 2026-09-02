package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        List<JiraSite> expected = new ArrayList<>();
        expected.add(new JiraSite("https://test.com"));
        expected.add(new JiraSite("https://otherTest.com"));

        JiraFolderProperty foo = new JiraFolderProperty();
        foo.setSites(Collections.singletonList(new JiraSite("https://test.com")));
        foo.setSites(new JiraSite("https://otherTest.com"));

        d.getProperties().add(foo);
        r.configRoundtrip(d);
        JiraFolderProperty prop = d.getProperties().get(JiraFolderProperty.class);
        assertNotNull(prop);
        List<JiraSite> actual = Arrays.asList(prop.getSites());
        r.assertEqualDataBoundBeans(expected, actual);
    }

    @Test
    void aSiteCanBeAddedToAFreshlyConstructedProperty(JenkinsRule r) {
        JiraFolderProperty property = new JiraFolderProperty();

        // The field defaulted to Collections.emptyList(), so this threw UnsupportedOperationException
        // on every freshly constructed property - which is every property Jenkins builds from the form.
        property.setSites(new JiraSite("https://test.com"));

        assertEquals(1, property.getSites().length);
        assertEquals("https://test.com/", property.getSites()[0].getName());
    }

    @Test
    void setSitesDoesNotAliasTheCallersList(JenkinsRule r) {
        List<JiraSite> caller = Arrays.asList(new JiraSite("https://test.com"));
        JiraFolderProperty property = new JiraFolderProperty();
        property.setSites(caller);

        // Used to mutate `caller` itself, and to throw because Arrays.asList is fixed size.
        property.setSites(new JiraSite("https://otherTest.com"));

        assertEquals(1, caller.size());
        assertEquals(2, property.getSites().length);
    }

    @Test
    void setSitesToleratesNull(JenkinsRule r) {
        JiraFolderProperty property = new JiraFolderProperty();

        property.setSites((List<JiraSite>) null);

        assertEquals(0, property.getSites().length);
    }

    public static CredentialsStore getFolderStore(Folder f) {
        return StreamSupport.stream(CredentialsProvider.lookupStores(f).spliterator(), false)
                .filter(s -> s.getProvider() instanceof FolderCredentialsProvider && s.getContext() == f)
                .findFirst()
                .orElse(null);
    }
}
