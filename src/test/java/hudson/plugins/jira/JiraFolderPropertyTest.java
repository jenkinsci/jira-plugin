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

    @Test
    void setSitesSingleSiteOnDefaultInstanceDoesNotThrow() {
        JiraFolderProperty property = new JiraFolderProperty();
        JiraSite site = new JiraSite("https://example.com");
        property.setSites(site);
        JiraSite[] sites = property.getSites();
        assertNotNull(sites);
        assertEquals(1, sites.length);
        assertEquals(site, sites[0]);
    }

    @Test
    void setSitesWhenSitesIsNull() {
        JiraFolderProperty property = new JiraFolderProperty();
        property.setSites((List<JiraSite>) null);
        JiraSite site = new JiraSite("https://example.com");
        property.setSites(site);
        JiraSite[] sites = property.getSites();
        assertNotNull(sites);
        assertEquals(1, sites.length);
        assertEquals(site, sites[0]);
    }

    @Test
    void setSitesWhenSitesIsUnmodifiableList() {
        JiraFolderProperty property = new JiraFolderProperty();
        JiraSite initialSite = new JiraSite("https://initial.com");
        property.setSites(Collections.singletonList(initialSite));
        JiraSite additionalSite = new JiraSite("https://additional.com");
        property.setSites(additionalSite);
        JiraSite[] sites = property.getSites();
        assertNotNull(sites);
        assertEquals(2, sites.length);
        assertEquals(initialSite, sites[0]);
        assertEquals(additionalSite, sites[1]);
    }

    @Test
    void setSitesWhenSitesIsAlreadyArrayList() {
        JiraFolderProperty property = new JiraFolderProperty();
        List<JiraSite> list = new ArrayList<>();
        JiraSite first = new JiraSite("https://first.com");
        list.add(first);
        property.setSites(list);
        JiraSite second = new JiraSite("https://second.com");
        property.setSites(second);
        JiraSite[] sites = property.getSites();
        assertNotNull(sites);
        assertEquals(2, sites.length);
        assertEquals(first, sites[0]);
        assertEquals(second, sites[1]);
    }

    public static CredentialsStore getFolderStore(Folder f) {
        return StreamSupport.stream(CredentialsProvider.lookupStores(f).spliterator(), false)
                .filter(s -> s.getProvider() instanceof FolderCredentialsProvider && s.getContext() == f)
                .findFirst()
                .orElse(null);
    }
}
