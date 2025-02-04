package hudson.plugins.jira.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.plugins.jira.JiraGlobalConfiguration;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.model.JiraIssueField;
import hudson.plugins.jira.selector.ExplicitIssueSelector;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** @author Dmitry Frolov tekillaz.dev@gmail.com */
@WithJenkinsConfiguredWithCode
@ExtendWith(MockitoExtension.class)
class IssueFieldUpdateStepTest {

    private static final String BUILD_NUMBER_VAR = "${BUILD_NUMBER}";

    @Mock(strictness = Mock.Strictness.LENIENT)
    JiraSite site;

    @Mock(strictness = Mock.Strictness.LENIENT)
    JiraSession session;

    @Mock
    PrintStream logger;

    @Mock(strictness = Mock.Strictness.LENIENT)
    AbstractBuild build;

    @Mock(strictness = Mock.Strictness.LENIENT)
    Launcher launcher;

    @Mock(strictness = Mock.Strictness.LENIENT)
    BuildListener listener;

    @Mock
    AbstractProject project;

    @Mock(strictness = Mock.Strictness.LENIENT)
    Job job;

    @BeforeEach
    void before(JenkinsConfiguredWithCodeRule r) {
        when(build.getParent()).thenReturn(project);
        when(build.getProject()).thenReturn(project);
        when(listener.getLogger()).thenReturn(logger);
        doReturn(session).when(site).getSession(any());
        JiraGlobalConfiguration.get().setSites(Collections.singletonList(site));
        //    when(jiraGlobalConfiguration.getSites()).thenReturn(sites);
        //    .getSession(build.getParent())).thenReturn(session);
        when(job.getBuild(any())).thenReturn(build);
    }

    @Test
    void checkPrepareFieldId() {

        List<String> field_test = Arrays.asList("10100", "customfield_10100", "field_10100");

        List<String> field_after = Arrays.asList("customfield_10100", "customfield_10100", "customfield_field_10100");

        IssueFieldUpdateStep jifu = new IssueFieldUpdateStep(null, null, "");
        for (int i = 0; i < field_test.size(); i++) {
            assertEquals(jifu.prepareFieldId(field_test.get(i)), field_after.get(i), "Check field id conversion #" + i);
        }
    }

    @Test
    void shouldFailIfSelectorIsNull() throws IOException, InterruptedException {
        IssueFieldUpdateStep jifu = spy(new IssueFieldUpdateStep(null, "", ""));
        assertThrows(IOException.class, () -> jifu.perform(build, null, launcher, listener));
    }

    //  @ConfiguredWithCode("single-site.yml")
    @Test
    void checkSubmit() throws InterruptedException, IOException {
        Random random = new Random();
        Integer randomBuildNumber = random.nextInt(85) + 15; // random number 15 < r < 99
        String issueId = "ISSUE-" + random.nextInt(1000) + 999;
        String beforeFieldid = "field" + random.nextInt(100) + 99;
        String beforeFieldValue = "Some comment, build #${BUILD_NUMBER}";

        EnvVars env = new EnvVars();
        env.put("BUILD_NUMBER", randomBuildNumber.toString());
        when(build.getEnvironment(listener)).thenReturn(env);

        final ExplicitIssueSelector issueSelector = new ExplicitIssueSelector(issueId);
        final List<String> issuesAfter = new ArrayList<String>();
        final List<JiraIssueField> fieldsAfter = new ArrayList<JiraIssueField>();

        doAnswer(invocation -> {
                    String id = (String) invocation.getArguments()[0];
                    List<JiraIssueField> jif = (List<JiraIssueField>) invocation.getArguments()[1];
                    issuesAfter.add(id);
                    fieldsAfter.addAll(jif);
                    return null;
                })
                .when(session)
                .addFields(anyString(), anyList());

        IssueFieldUpdateStep jifu = spy(new IssueFieldUpdateStep(issueSelector, beforeFieldid, beforeFieldValue));
        jifu.perform(build, null, launcher, listener);

        assertEquals(issuesAfter.get(0), issueId, "Check issue value");
        assertEquals("customfield_" + beforeFieldid, fieldsAfter.get(0).getId());
        assertThat(fieldsAfter.get(0).getValue().toString(), containsString("build #" + randomBuildNumber.toString()));

        jifu.perform(build, null, env, launcher, listener);
        assertThat(fieldsAfter.get(1).getValue().toString(), containsString("build #" + randomBuildNumber.toString()));
    }
}
