package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.*;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;

@WithJenkinsConfiguredWithCode
class JiraProjectPropertyTest {

    private JenkinsRule r;

    private FreeStyleProject freeStyleProject;
    private Folder folder;
    private List<JiraSite> firstList;

    @BeforeEach
    void initialize(JenkinsConfiguredWithCodeRule r) throws Exception {
        this.r = r;
        folder = r.jenkins.createProject(Folder.class, "first");
        JiraFolderProperty jiraFolderProperty = new JiraFolderProperty();
        firstList = new ArrayList<>();
        firstList.add(new JiraSite("https://first.com/"));
        jiraFolderProperty.setSites(firstList);
        folder.getProperties().add(jiraFolderProperty);
    }

    @Test
    void getSitesNullWithoutFolder() throws Exception {
        FreeStyleProject freeStyleProject = r.createFreeStyleProject();
        JiraProjectProperty prop = new JiraProjectProperty(null);
        freeStyleProject.addProperty(prop);
        JiraProjectProperty actual = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(actual);
        assertNull(actual.getSite());
    }

    @Test
    void getSitesNullWithFolder() throws Exception {
        freeStyleProject = folder.createProject(FreeStyleProject.class, "something");
        JiraProjectProperty prop = new JiraProjectProperty(null);
        freeStyleProject.addProperty(prop);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNull(property.getSite());
    }

    @Test
    @ConfiguredWithCode("single-site.yml")
    void getSiteFromProjectProperty() {
        JiraProjectProperty prop = new JiraProjectProperty(null);
        JiraSite site = prop.getSite();
        @SuppressWarnings("ConstantConditions")
        String actual = site.getUrl().toExternalForm();
        assertEquals("https://jira.com/", actual);
    }

    @Test
    @ConfiguredWithCode("single-site.yml")
    void getSiteFromSingleEntry() throws Exception {
        freeStyleProject = r.createFreeStyleProject();
        JiraSite expected = JiraGlobalConfiguration.get().getSites().get(0);
        JiraProjectProperty prop = new JiraProjectProperty(null);
        freeStyleProject.addProperty(prop);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNotNull(property.getSite());
        assertEquals(expected.getName(), property.siteName);
        r.assertEqualDataBoundBeans(expected, property.getSite());
    }

    @Test
    @ConfiguredWithCode("multiple-sites.yml")
    void getSiteFromFirstGlobalMultipleEntryMultipleSites() throws Exception {
        freeStyleProject = r.createFreeStyleProject();
        JiraSite expected = JiraGlobalConfiguration.get().getSites().get(0);
        JiraProjectProperty prop = new JiraProjectProperty(null);
        freeStyleProject.addProperty(prop);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNotNull(property.getSite());
        assertEquals(expected.getName(), property.siteName);
        r.assertEqualDataBoundBeans(expected, property.getSite());
    }

    @Test
    @ConfiguredWithCode("multiple-sites.yml")
    void getSiteFromSecondGlobalEntryMultipleSites() throws Exception {
        freeStyleProject = r.createFreeStyleProject();
        JiraSite expected = new JiraSite("https://jira.com/");
        JiraProjectProperty prop = new JiraProjectProperty(expected.getName());
        freeStyleProject.addProperty(prop);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNotNull(property.getSite());
        assertEquals(expected.getName(), property.siteName);
        r.assertEqualDataBoundBeans(expected, property.getSite());
    }

    @Test
    @ConfiguredWithCode("single-site.yml")
    void getSiteFromFirstFolderLayer() throws Exception {
        freeStyleProject = folder.createProject(FreeStyleProject.class, "something");
        JiraSite expected = firstList.get(0);
        JiraProjectProperty prop = new JiraProjectProperty(expected.getName());
        freeStyleProject.addProperty(prop);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNotNull(property.getSite());
        assertEquals(expected.getName(), property.siteName);
        r.assertEqualDataBoundBeans(expected, property.getSite());
    }

    @Test
    @ConfiguredWithCode("single-site.yml")
    void getSiteFromNestedFolderLayer() throws Exception {
        Folder secondFolder = folder.createProject(Folder.class, "second");
        freeStyleProject = secondFolder.createProject(FreeStyleProject.class, "something");
        // testing we can get value from folder above.
        JiraSite expected = firstList.get(0);
        JiraProjectProperty prop = new JiraProjectProperty(expected.getName());
        freeStyleProject.addProperty(prop);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNotNull(property.getSite());
        assertEquals(expected.getName(), property.siteName);
        r.assertEqualDataBoundBeans(expected, property.getSite());
    }
}
