package hudson.plugins.jira;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JiraProjectPropertyTest {

    @Rule
    public final JenkinsRule r = new JenkinsRule();

    private FreeStyleProject freeStyleProject;
    private Folder folder;
    private List<JiraSite> firstList;

    @Before
    public void initialize() throws Exception {
        folder = r.jenkins.createProject(Folder.class, "first");
        JiraFolderProperty jiraFolderProperty = new JiraFolderProperty();
        firstList = new ArrayList<>();
        firstList.add(new JiraSite("https://first.com/"));
        jiraFolderProperty.setSites(firstList);
        folder.getProperties().add(jiraFolderProperty);
    }

    @Test
    public void getSitesNullWithoutFolder() throws Exception {
        FreeStyleProject freeStyleProject = r.createFreeStyleProject();
        JiraProjectProperty prop = new JiraProjectProperty(null);
        freeStyleProject.addProperty(prop);
        JiraSite site = prop.getSite();
        assertNull(site);
        JiraProjectProperty actual = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(actual);
        assertNull(actual.getSite());
    }

    @Test
    public void getSitesNullWithFolder() throws Exception {
        freeStyleProject = folder.createProject(FreeStyleProject.class, "something");
        JiraProjectProperty prop = new JiraProjectProperty(null);
        freeStyleProject.addProperty(prop);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNull(property.getSite());
    }

    @Test
    public void testNoInitialDefault() throws Exception {
        JiraProjectProperty prop = new JiraProjectProperty(null);
        setupGlobalConfig("single-site.yml");
        JiraSite site = prop.getSite();
        assertNotNull(site);
        assertEquals("https://jira.com/", site.getName());
    }

    @Test
    public void testDefault() throws Exception {
        freeStyleProject = r.createFreeStyleProject();
        testProperty("single-site.yml", null);
    }

    @Test
    public void testDefaultWithTwoEntries() throws Exception {
        freeStyleProject = r.createFreeStyleProject();
        testProperty("multiple-sites.yml", null);
    }

    @Test
    public void testChooseWithOutFolder() throws Exception {
        freeStyleProject = r.createFreeStyleProject();
        JiraSite jiraSite = new JiraSite("https://jira.com/");
        testProperty("multiple-sites.yml", jiraSite);
    }

    @Test
    public void testChooseWithFolder() throws Exception {
        freeStyleProject = folder.createProject(FreeStyleProject.class, "something");
        testProperty("single-site.yml", firstList.get(0));
    }

    @Test
    public void configRoundtripWithNestedFolder() throws Exception {
        r.configRoundtrip(folder);
        Folder secondFolder = folder.createProject(Folder.class, "second");
        r.configRoundtrip(secondFolder);
        freeStyleProject = secondFolder.createProject(FreeStyleProject.class, "something");
        // testing we can get value from folder above.
        testProperty("single-site.yml", firstList.get(0), true);
    }

    private void testProperty(String input, JiraSite expected) throws Exception {
        testProperty(input, expected, false);
    }

     private void testProperty(String input, JiraSite expected, boolean roundTrip) throws Exception {
        setupGlobalConfig(input);
        assertNull(freeStyleProject.getProperty(JiraProjectProperty.class));
        if (expected != null) {
            JiraProjectProperty prop = new JiraProjectProperty(expected.getName());
            freeStyleProject.addProperty(prop);
        } else {
            JiraProjectProperty prop = new JiraProjectProperty(null);
            freeStyleProject.addProperty(prop);
        }
        if (roundTrip) r.configRoundtrip(freeStyleProject);
        JiraProjectProperty property = freeStyleProject.getProperty(JiraProjectProperty.class);
        assertNotNull(property);
        assertNotNull(property.getSite());
        if (expected == null) {
            expected = JiraGlobalConfiguration.get().getSites().get(0);
        }
        assertEquals(expected.getName(), property.siteName);
        r.assertEqualDataBoundBeans(expected, property.getSite());
    }

    private void setupGlobalConfig(String input) throws Exception {
        ConfigurationAsCode.get().configure(
            ConfigAsCodeTest.class.getResource(input).toString());
    }
}
