package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import hudson.model.Result;
import hudson.util.XStream2;
import java.util.Collections;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class JiraVersionCreatorBuilderTest {

    private JiraSite site;

    private JiraSession session;

    private final XStream2 xStream2 = new XStream2();

    @BeforeEach
    void createMocks() {
        site = mock(JiraSite.class);
        session = mock(JiraSession.class);
    }

    @WithJenkins
    @Test
    void testPipelineWithJiraSite(JenkinsRule r) throws Exception {
        JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
        jiraGlobalConfiguration.setSites(Collections.singletonList(site));
        doReturn(session).when(site).getSession(any());
        WorkflowJob job = r.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("""
                        jiraCreateVersion(jiraVersion: 'Version', jiraProjectKey: 'project-key')
                """, true));
        WorkflowRun b = r.buildAndAssertStatus(Result.SUCCESS, job);
        r.assertLogContains("[Jira] Creating version Version in project project-key.", b);
    }

    @Test
    @WithoutJenkins
    void readResolveSetsFailIfAlreadyExistsWhenMissingInConfig() {
        String xml = """
    <hudson.plugins.jira.JiraVersionCreatorBuilder>
      <jiraVersion>1.0</jiraVersion>
      <jiraProjectKey>PROJ</jiraProjectKey>
    </hudson.plugins.jira.JiraVersionCreatorBuilder>
    """;
        JiraVersionCreatorBuilder builder = (JiraVersionCreatorBuilder) xStream2.fromXML(xml);

        assertTrue(builder.isFailIfAlreadyExists());

        xml = """
    <hudson.plugins.jira.JiraVersionCreator>
      <jiraVersion>1.2</jiraVersion>
      <jiraProjectKey>PROJ</jiraProjectKey>
    </hudson.plugins.jira.JiraVersionCreator>
    """;
        JiraVersionCreator notifier = (JiraVersionCreator) xStream2.fromXML(xml);

        assertThat(notifier.getJiraProjectKey(), is("PROJ"));
        assertThat(notifier.getJiraVersion(), is("1.2"));
        assertTrue(notifier.isFailIfAlreadyExists());
    }

    @Test
    @WithoutJenkins
    void readResolvePresentInConfig() {
        String xml = """
    <hudson.plugins.jira.JiraVersionCreatorBuilder>
      <jiraVersion>1.0</jiraVersion>
      <jiraProjectKey>PROJ</jiraProjectKey>
      <failIfAlreadyExists>false</failIfAlreadyExists>
    </hudson.plugins.jira.JiraVersionCreatorBuilder>
    """;
        JiraVersionCreatorBuilder builder = (JiraVersionCreatorBuilder) xStream2.fromXML(xml);

        assertFalse(builder.isFailIfAlreadyExists());

        xml = """
    <hudson.plugins.jira.JiraVersionCreator>
      <jiraVersion>1.0</jiraVersion>
      <jiraProjectKey>PROJ</jiraProjectKey>
      <failIfAlreadyExists>false</failIfAlreadyExists>
    </hudson.plugins.jira.JiraVersionCreator>
    """;
        JiraVersionCreator notifier = (JiraVersionCreator) xStream2.fromXML(xml);

        assertFalse(notifier.isFailIfAlreadyExists());
    }
}
