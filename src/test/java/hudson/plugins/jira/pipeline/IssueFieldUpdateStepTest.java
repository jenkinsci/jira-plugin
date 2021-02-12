package hudson.plugins.jira.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.plugins.jira.JiraGlobalConfiguration;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.model.JiraIssueField;
import hudson.plugins.jira.selector.ExplicitIssueSelector;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/** @author Dmitry Frolov tekillaz.dev@gmail.com */
@RunWith(MockitoJUnitRunner.class)
public class IssueFieldUpdateStepTest {

  private static final String BUILD_NUMBER_VAR = "${BUILD_NUMBER}";

  @Rule public JenkinsRule r = new JenkinsConfiguredWithCodeRule();

  @Mock JiraSite site;
  @Mock JiraSession session;
  @Mock PrintStream logger;
  @Mock AbstractBuild build;
  @Mock Launcher launcher;
  @Mock BuildListener listener;
  @Mock AbstractProject project;
  @Mock Job job;

  @Before
  public void before() {
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
  public void checkPrepareFieldId() {

    List<String> field_test = Arrays.asList("10100", "customfield_10100", "field_10100");

    List<String> field_after =
        Arrays.asList("customfield_10100", "customfield_10100", "customfield_field_10100");

    IssueFieldUpdateStep jifu = new IssueFieldUpdateStep(null, null, "");
    for (int i = 0; i < field_test.size(); i++)
      assertEquals(
          "Check field id conversion #" + i,
          jifu.prepareFieldId(field_test.get(i)),
          field_after.get(i));
  }

  @Test(expected = IOException.class)
  public void shouldFailIfSelectorIsNull() throws InterruptedException, IOException {
    IssueFieldUpdateStep jifu = spy(new IssueFieldUpdateStep(null, "", ""));
    jifu.perform(build, null, launcher, listener);
    assertSame("Check selector is null", Result.FAILURE, build.getResult());
  }

  @Test
  //  @ConfiguredWithCode("single-site.yml")
  public void checkSubmit() throws InterruptedException, IOException {
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

    doAnswer(
            invocation -> {
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

    assertEquals("Check issue value", issuesAfter.get(0), issueId);
    assertEquals("customfield_" + beforeFieldid, fieldsAfter.get(0).getId());
    assertThat(
        fieldsAfter.get(0).getValue().toString(),
        containsString("build #" + randomBuildNumber.toString()));

    jifu.perform(build, null, env, launcher, listener);
    assertThat(
        fieldsAfter.get(1).getValue().toString(),
        containsString("build #" + randomBuildNumber.toString()));
  }
}
