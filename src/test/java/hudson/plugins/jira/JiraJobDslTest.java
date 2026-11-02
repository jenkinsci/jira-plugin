package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class JiraJobDslTest {
    @Test
    public void testDescribableModelGuard() {
        org.jenkinsci.plugins.structs.describable.DescribableModel<JiraFolderProperty> model =
                new org.jenkinsci.plugins.structs.describable.DescribableModel<>(JiraFolderProperty.class);
        org.junit.jupiter.api.Assertions.assertNotNull(model);
    }

    @Test
    public void testFolderJiraSiteJobDsl(JenkinsRule j) throws Exception {
        // Given: A seed script using Job DSL symbols for folder property and jira site
        String dslScript = "folder('my-jira-folder') {\n" + "    properties {\n"
                + "        jiraFolderProperty {\n"
                + "            sites {\n"
                + "                jiraSite {\n"
                + "                    url('https://jira.example.com')\n"
                + "                    credentialsId('my-jira-creds')\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}";

        // Create a FreeStyleProject for seed execution
        FreeStyleProject job = j.createFreeStyleProject("seed-job");
        ExecuteDslScripts dslStep = new ExecuteDslScripts();
        dslStep.setScriptText(dslScript);
        dslStep.setRemovedJobAction(javaposse.jobdsl.plugin.RemovedJobAction.IGNORE);
        dslStep.setRemovedViewAction(javaposse.jobdsl.plugin.RemovedViewAction.IGNORE);
        job.getBuildersList().add(dslStep);

        // Run the seed job
        j.buildAndAssertSuccess(job);

        // Then: Assert the folder was created and contains the correct Jira configuration
        TopLevelItem item = j.jenkins.getItem("my-jira-folder");
        assertThat(item, notNullValue());
        assertTrue(item instanceof Folder);

        Folder folder = (Folder) item;
        JiraFolderProperty property = folder.getProperties().get(JiraFolderProperty.class);
        assertThat(property, notNullValue());

        JiraSite[] sites = property.getSites();
        assertThat(sites.length, is(1));
        assertThat(sites[0].getUrl().toExternalForm(), is("https://jira.example.com/"));
        assertThat(sites[0].getCredentialsId(), is("my-jira-creds"));
    }
}
