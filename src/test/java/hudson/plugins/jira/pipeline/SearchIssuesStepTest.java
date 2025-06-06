package hudson.plugins.jira.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.inject.Inject;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.pipeline.SearchIssuesStep.SearchStepExecution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SearchIssuesStepTest {

    private JenkinsRule jenkinsRule;

    @Inject
    SearchIssuesStep.DescriptorImpl descriptor;

    @BeforeEach
    void setUp(JenkinsRule jenkinsRule) throws Exception {
        this.jenkinsRule = jenkinsRule;
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    void configRoundTrip() throws Exception {
        configRoundTrip("");
        configRoundTrip("key='EXAMPLE-1'");
    }

    private void configRoundTrip(String jql) throws Exception {
        SearchIssuesStep step = new StepConfigTester(jenkinsRule).configRoundTrip(new SearchIssuesStep(jql));
        assertEquals(jql, step.getJql());
    }

    @Test
    void callGetIssuesFromJqlSearch() throws Exception {
        JiraSession session = mock(JiraSession.class);
        String jql = "key='EXAMPLE-1'";
        Issue issue = mock(Issue.class);
        when(issue.getKey()).thenReturn("EXAMPLE-1");

        final List<Issue> assertCalledList = new ArrayList<>();
        when(session.getIssuesFromJqlSearch(jql)).then(invocation -> {
            Issue issue2 = mock(Issue.class);
            when(issue2.getKey()).thenReturn("EXAMPLE-1");
            assertCalledList.add(issue2);
            return assertCalledList;
        });

        JiraSite site = mock(JiraSite.class);

        AbstractProject mockProject = mock(FreeStyleProject.class);
        Run mockRun = mock(Run.class);
        Job mockJob = mock(Job.class);
        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);

        when(jiraProjectProperty.getSite()).thenReturn(site);
        when(site.getSession(mockProject)).thenReturn(session);
        when(mockRun.getParent()).thenReturn(mockProject);
        when(mockRun.getParent().getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);

        Map<String, Object> r = new HashMap<>();
        r.put("jql", jql);
        SearchIssuesStep step = (SearchIssuesStep) descriptor.newInstance(r);

        StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(Run.class)).thenReturn(mockRun);

        SearchStepExecution start = (SearchStepExecution) step.start(ctx);
        List<String> returnedList = start.run();
        assertThat(assertCalledList, hasSize(1));
        assertThat(returnedList, hasSize(1));
        assertThat(assertCalledList.iterator().next().getKey(), equalTo("EXAMPLE-1"));
        assertThat(returnedList.iterator().next(), equalTo("EXAMPLE-1"));
    }
}
