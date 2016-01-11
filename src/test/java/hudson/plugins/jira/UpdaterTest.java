package hudson.plugins.jira;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.User;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.ChangeLogSet.Entry;
import jenkins.model.Jenkins;
import jenkins.model.Jenkins.JenkinsHolder;


/**
 * Test case for the JIRA {@link Updater}.
 *
 * @author kutzi
 */
@SuppressWarnings("unchecked")
public class UpdaterTest {

	private Updater updater;
	
    private static class MockEntry extends Entry {

        private final String msg;

        public MockEntry(String msg) {
            this.msg = msg;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return null;
        }

        @Override
        public User getAuthor() {
            return null;
        }

        @Override
        public String getMsg() {
            return this.msg;
        }
    }

    @Before
    public void prepare() {
    	this.updater = new Updater(mock(SCM.class));
    }
    
    /**
     * Tests that the JiraIssueParameters are identified as updateable JIRA
     * issues.
     */
    @Test
    @Bug(12312)
    public void testFindIssuesWithJiraParameters() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);

        JiraIssueParameterValue parameter = mock(JiraIssueParameterValue.class);
        JiraIssueParameterValue parameterTwo = mock(JiraIssueParameterValue.class);
        ParametersAction action = mock(ParametersAction.class);
        List<ParameterValue> parameters = new ArrayList<ParameterValue>();

        when(changeLogSet.iterator()).thenReturn(
                Collections.EMPTY_LIST.iterator());
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getAction(ParametersAction.class)).thenReturn(action);
        when(action.getParameters()).thenReturn(parameters);
        when(parameter.getValue()).thenReturn("JIRA-123");
        when(parameterTwo.getValue()).thenReturn("JIRA-321");

        Set<String> ids = new HashSet<String>();

        // Initial state contains zero parameters
        updater.findIssues(build, ids, null, listener);
        Assert.assertTrue(ids.isEmpty());

        ids = new HashSet<String>();
        parameters.add(parameter);
        updater.findIssues(build, ids, JiraSite.DEFAULT_ISSUE_PATTERN, listener);
        Assert.assertEquals(1, ids.size());
        Assert.assertEquals("JIRA-123", ids.iterator().next());

        ids = new TreeSet<String>();
        parameters.add(parameterTwo);
        updater.findIssues(build, ids, JiraSite.DEFAULT_ISSUE_PATTERN, listener);
        Assert.assertEquals(2, ids.size());
        Set<String> expected = Sets.newTreeSet(Sets.newHashSet("JIRA-123",
                "JIRA-321"));
        Assert.assertEquals(expected, ids);
    }

    @Test
    @Bug(4132)
    public void testProjectNamesAllowed() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);

        Set<? extends Entry> entries = Sets.newHashSet(
                new MockEntry("Fixed JI123-4711"),
                new MockEntry("Fixed foo_bar-4710"),
                new MockEntry("Fixed FoO_bAr-4711"),
                new MockEntry("Fixed someting.\nJFoO_bAr_MULTI-4718"),
                new MockEntry("TR-123: foo"),
                new MockEntry("[ABC-42] hallo"),
                new MockEntry("#123: this one must not match"),
                new MockEntry("ABC-: this one must also not match"),
                new MockEntry("ABC-: \n\nABC-127:\nthis one should match"),
                new MockEntry("ABC-: \n\nABC-128:\nthis one should match"),
                new MockEntry("ABC-: \n\nXYZ-10:\nXYZ-20 this one too"),
                new MockEntry("Fixed DOT-4."),
                new MockEntry("Fixed DOT-5. Did it right this time")
        );

        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(changeLogSet.iterator()).thenReturn(entries.iterator());
        
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        Set<String> expected = Sets.newHashSet(
                "JI123-4711",
                "FOO_BAR-4710",
                "FOO_BAR-4711",
                "JFOO_BAR_MULTI-4718",
                "TR-123",
                "ABC-42",
                "ABC-127",
                "ABC-128",
                "XYZ-10",
                "XYZ-20",
                "DOT-4",
                "DOT-5"
        );

        Set<String> result = new HashSet<String>();
        updater.findIssues(build, result, JiraSite.DEFAULT_ISSUE_PATTERN, listener);

        Assert.assertEquals(expected.size(), result.size());
        Assert.assertEquals(expected,result);
    }

    /**
     * Tests that the generated comment matches the expectations -
     * especially that the JIRA id is not stripped from the comment.
     */
    @Test
    @Bug(4572)
    public void testComment() throws Exception {
        // mock JIRA session:
        JiraSession session = mock(JiraSession.class);
        when(session.existsIssue(Mockito.anyString())).thenReturn(Boolean.TRUE);
        final Issue mockIssue = Mockito.mock(Issue.class);
        when(session.getIssue(Mockito.anyString())).thenReturn(mockIssue);

        final List<String> comments = new ArrayList<String>();

        Answer answer = new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                comments.add((String) invocation.getArguments()[1]);
                return null;
            }
        };
        doAnswer(answer).when(session).addComment(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // mock build:
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        FreeStyleProject project = mock(FreeStyleProject.class);
        when(build.getProject()).thenReturn(project);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getResult()).thenReturn(Result.SUCCESS);

        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed FOOBAR-4711"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);
        
        // test:
        List<JiraIssue> ids = Lists.newArrayList(new JiraIssue("FOOBAR-4711", "Title"));
        Updater updaterCurrent = new Updater(build.getParent().getScm());
        updaterCurrent.submitComments(build,
                System.out, "http://jenkins", ids, session, false, false, "", "");

        Assert.assertEquals(1, comments.size());
        String comment = comments.get(0);

        Assert.assertTrue(comment.contains("FOOBAR-4711"));

        // must also work case-insensitively (JENKINS-4132)
        comments.clear();
        entries = Sets.newHashSet(new MockEntry("Fixed Foobar-4711"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());
        ids = Lists.newArrayList(new JiraIssue("FOOBAR-4711", "Title"));
                
        updaterCurrent.submitComments(build,
                System.out, "http://jenkins", ids, session, false, false, "", "");

        Assert.assertEquals(1, comments.size());
        comment = comments.get(0);

        Assert.assertTrue(comment.contains("Foobar-4711"));

    }

    /**
     * Tests that the default pattern doesn't match strings like
     * 'project-1.1'.
     * These patterns are used e.g. by the maven release plugin.
     */
    @Test
    public void testDefaultPattertNotToMatchMavenRelease() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        // commit messages like the one from the Maven release plugin must not match
        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("prepare release project-4.7.1"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        Set<String> ids = new HashSet<String>();
        updater.findIssues(build, ids, JiraSite.DEFAULT_ISSUE_PATTERN, null);
        Assert.assertEquals(0, ids.size());
    }

    @Test
    @Bug(6043)
    public void testUserPatternNotMatch() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed FOO_BAR-4711"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        Set<String> ids = new HashSet<String>();
        updater.findIssues(build, ids, Pattern.compile("[(w)]"), mock(BuildListener.class));

        Assert.assertEquals(0, ids.size());
    }

    @Test
    @Bug(6043)
    public void testUserPatternMatch() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed toto [FOOBAR-4711]"), new MockEntry("[TEST-9] with [dede]"), new MockEntry("toto [maven-release-plugin] prepare release foo-2.2.3"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());
        
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);
        
        Set<String> ids = new HashSet<String>();
        Pattern pat = Pattern.compile("\\[(\\w+-\\d+)\\]");
        updater.findIssues(build, ids, pat, mock(BuildListener.class));
        Assert.assertEquals(2, ids.size());
        Assert.assertTrue(ids.contains("TEST-9"));
        Assert.assertTrue(ids.contains("FOOBAR-4711"));
    }

    @Test
    @Bug(6043)
    public void testUserPatternMatchTwoIssuesInOneComment() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed toto [FOOBAR-4711]  [FOOBAR-21] "), new MockEntry("[TEST-9] with [dede]"), new MockEntry("toto [maven-release-plugin] prepare release foo-2.2.3"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);
        
        Set<String> ids = new HashSet<String>();
        Pattern pat = Pattern.compile("\\[(\\w+-\\d+)\\]");
        updater.findIssues(build, ids, pat, mock(BuildListener.class));
        Assert.assertEquals(3, ids.size());
        Assert.assertTrue(ids.contains("TEST-9"));
        Assert.assertTrue(ids.contains("FOOBAR-4711"));
        Assert.assertTrue(ids.contains("FOOBAR-21"));
    }
    
    /**
     * Checks if issues are correctly removed from the carry over list.
     * @throws RemoteException
     */
    @Test
    @Bug(17156)
    public void testIssueIsRemovedFromCarryOverListAfterSubmission() throws RestClientException {
        // mock build:
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        FreeStyleProject project = mock(FreeStyleProject.class);
        when(build.getProject()).thenReturn(project);
        ChangeLogSet changeLogSet = ChangeLogSet.createEmpty(build);
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getResult()).thenReturn(Result.SUCCESS);

        final JiraIssue firstIssue = new JiraIssue("FOOBAR-1", "Title");
        final JiraIssue secondIssue = new JiraIssue("ALIBA-1", "Title");
        final JiraIssue thirdIssue = new JiraIssue("MOONA-1", "Title");
        final JiraIssue deletedIssue = new JiraIssue("FOOBAR-2", "Title");
        final JiraIssue forbiddenIssue = new JiraIssue("LASSO-17", "Title");

        // assume that there is a following list of jira issues from scm commit messages out of hudson.plugins.jira.JiraCarryOverAction
        List<JiraIssue> issues = Lists.newArrayList(firstIssue, secondIssue, forbiddenIssue, thirdIssue);

        // mock JIRA session:
        JiraSession session = mock(JiraSession.class);
        when(session.existsIssue(Mockito.anyString())).thenReturn(Boolean.TRUE);
//        when(session.getIssue(Mockito.anyString())).thenReturn( new Issue());
//        when(session.getGroup(Mockito.anyString())).thenReturn(new Group("Software Development", null));

        final List<Comment> comments = new ArrayList<Comment>();

        Answer answer = new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Comment c = Comment.createWithGroupLevel((String) invocation.getArguments()[0], (String) invocation.getArguments()[1]);
                comments.add(c);
                return null;
            }
        };

        doAnswer(answer).when(session).addComment(eq(firstIssue.id), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doAnswer(answer).when(session).addComment(eq(secondIssue.id), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doAnswer(answer).when(session).addComment(eq(thirdIssue.id), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // issue for the caught exception
        doThrow(new RestClientException(new Throwable(), 404)).when(session).addComment(eq(deletedIssue.id), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doThrow(new RestClientException(new Throwable(), 403)).when(session).addComment(eq(forbiddenIssue.id), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());


        final String groupVisibility = "";
        final String roleVisibility = "";

        Updater updaterCurrent = new Updater(build.getParent().getScm());
        
        updaterCurrent.submitComments(
                build, System.out, "http://jenkins", issues, session, false, false, groupVisibility, roleVisibility
        );

        // expected issue list
        final List<JiraIssue> expectedIssuesToCarryOver = new ArrayList<JiraIssue>();
        expectedIssuesToCarryOver.add(forbiddenIssue);
        Assert.assertThat(issues, is(expectedIssuesToCarryOver));
    }
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    /**
     * Test that workflow job - run instance of type WorkflowJob - can
     * return changeSets using java reflection api
     * @throws IOException 
     *
     */
    @Test
    public void testGetChangesUsingReflectionForWorkflowJob() throws IOException {
    	Jenkins jenkins = mock(Jenkins.class);
    	
    	when(jenkins.getRootDirFor(Mockito.<TopLevelItem>anyObject())).thenReturn(folder.getRoot());
    	WorkflowJob workflowJob = new WorkflowJob(jenkins, "job");
    	WorkflowRun workflowRun = new WorkflowRun(workflowJob);
    	
    	ChangeLogSet changeLogSet = ChangeLogSet.createEmpty(workflowRun);
    	
    	List<ChangeLogSet<? extends Entry>> changesUsingReflection = Updater.getChangesUsingReflection(workflowRun);
    	Assert.assertNotNull(changesUsingReflection);
    	Assert.assertTrue(changesUsingReflection.isEmpty());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetChangesUsingReflectionForunknownJob() throws IOException {
    	Run run = mock(Run.class);
    	List<ChangeLogSet<? extends Entry>> changesUsingReflection = Updater.getChangesUsingReflection(run);
    }
    
}
