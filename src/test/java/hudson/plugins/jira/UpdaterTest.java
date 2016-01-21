package hudson.plugins.jira;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hamcrest.Matchers;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.scm.SCM;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import jenkins.model.Jenkins;

/**
 * Test case for the JIRA {@link Updater}.
 *
 * @author kutzi
 */
@SuppressWarnings("unchecked")
public class UpdaterTest {

        @Rule
        public JenkinsRule rule = new JenkinsRule();

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
        SCM scm = mock(SCM.class);
        this.updater = new Updater(scm);
    }

    @Test
    @WithoutJenkins
    public void testGetScmCommentsFromPreviousBuilds() throws Exception {
        final FreeStyleProject project = mock(FreeStyleProject.class);
        final FreeStyleBuild build1 = mock(FreeStyleBuild.class);
        final MockEntry entry1 = new MockEntry("FOOBAR-1: The first build");
        {
            ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
            when(build1.getChangeSet()).thenReturn(changeLogSet);
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
            changeSets.add(changeLogSet);
            when(build1.getChangeSets()).thenReturn(changeSets);
            when(build1.getResult()).thenReturn(Result.FAILURE);
            doReturn(project).when(build1).getProject();

            doReturn(new JiraCarryOverAction(Lists.newArrayList(new JiraIssue("FOOBAR-1", null))))
                    .when(build1).getAction(JiraCarryOverAction.class);

            final Set<? extends Entry> entries = Sets.newHashSet(entry1);
            when(changeLogSet.iterator()).thenAnswer(new Answer<Object>() {

                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    return entries.iterator();
                }
            });
        }

        final FreeStyleBuild build2 = mock(FreeStyleBuild.class);
        final MockEntry entry2 = new MockEntry("FOOBAR-2: The next build");
        {
            ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
            when(build2.getChangeSet()).thenReturn(changeLogSet);
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
            changeSets.add(changeLogSet);
            when(build2.getChangeSets()).thenReturn(changeSets);
            when(build2.getPreviousBuild()).thenReturn(build1);
            when(build2.getResult()).thenReturn(Result.SUCCESS);
            doReturn(project).when(build2).getProject();

            final Set<? extends Entry> entries = Sets.newHashSet(entry2);
            when(changeLogSet.iterator()).thenAnswer(new Answer<Object>() {

                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    return entries.iterator();
                }

            });
        }

        final List<Comment> comments = Lists.newArrayList();
        final JiraSession session = mock(JiraSession.class);
        doAnswer(new Answer<Object>() {

            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Comment rc = Comment.createWithGroupLevel((String) invocation.getArguments()[1], (String) invocation.getArguments()[2]);
                comments.add(rc);
                return null;
            }

        }).when(session).addComment(anyString(), anyString(), anyString(), anyString());

        this.updater = new Updater(build2.getProject().getScm());        
        
        final List<JiraIssue> ids = Lists.newArrayList(new JiraIssue("FOOBAR-1", null), new JiraIssue("FOOBAR-2", null));
        updater.submitComments(build2, System.out, "http://jenkins", ids, session, false, false, "", "");

        Assert.assertEquals(2, comments.size());
        Assert.assertThat(comments.get(0).getBody(), Matchers.containsString(entry1.getMsg()));
        Assert.assertThat(comments.get(1).getBody(), Matchers.containsString(entry2.getMsg()));
    }

    /**
     * Tests that the generated comment matches the expectations -
     * especially that the JIRA id is not stripped from the comment.
     */
    @Test
    @Bug(4572)
    @WithoutJenkins
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
    /**
     * Checks if issues are correctly removed from the carry over list.
     * @throws RemoteException
     */
    @Test
    @Bug(17156)
    @WithoutJenkins
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
    @WithoutJenkins
    public void testGetChangesUsingReflectionForWorkflowJob() throws IOException {
        Jenkins jenkins = mock(Jenkins.class);
        
        when(jenkins.getRootDirFor(Mockito.<TopLevelItem>anyObject())).thenReturn(folder.getRoot());
        WorkflowJob workflowJob = new WorkflowJob(jenkins, "job");
        WorkflowRun workflowRun = new WorkflowRun(workflowJob);
        
        ChangeLogSet changeLogSet = ChangeLogSet.createEmpty(workflowRun);
        
        List<ChangeLogSet<? extends Entry>> changesUsingReflection = RunScmChangeExtractor.getChangesUsingReflection(workflowRun);
        Assert.assertNotNull(changesUsingReflection);
        Assert.assertTrue(changesUsingReflection.isEmpty());
    }
    
    @WithoutJenkins
    @Test(expected=IllegalArgumentException.class)
    public void testGetChangesUsingReflectionForunknownJob() throws IOException {
        Run run = mock(Run.class);
        List<ChangeLogSet<? extends Entry>> changesUsingReflection = RunScmChangeExtractor.getChangesUsingReflection(run);
    }

    /**
     * Test formatting of scm entry change time.
     *
     */
    @Test
    @WithoutJenkins
    public void testAppendChangeTimestampToDescription() {
        Updater updater = new Updater(null);
        StringBuilder description = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        JiraSite site = mock(JiraSite.class);
        when(site.getDateTimePattern()).thenReturn("yyyy-MM-dd HH:mm:ss");
        updater.appendChangeTimestampToDescription(description, site, calendar.getTimeInMillis());
        System.out.println(description.toString());
        Assert.assertThat(description.toString(), equalTo("2016-01-01 00:00:00"));
    }

    /**
     * Test formatting of scm entry change description.
     *
     */
    @Test
    public void testDateTimeInChangeDescription() {
        rule.getInstance();
        Updater updater = new Updater(null);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        JiraSite site = mock(JiraSite.class);
        when(site.isAppendChangeTimestamp()).thenReturn(true);
        when(site.getDateTimePattern()).thenReturn("yyyy-MM-dd HH:mm:ss");

        Run r = mock(Run.class);
        Job j = mock(Job.class);
        when(r.getParent()).thenReturn(j);
        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);
        when(j.getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);
        when(jiraProjectProperty.getSite()).thenReturn(site);

        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getTimestamp()).thenReturn(calendar.getTimeInMillis());
        when(entry.getCommitId()).thenReturn("dsgsvds2re3dsv");
        User mockAuthor = mock(User.class);
        when(mockAuthor.getId()).thenReturn("jenkins-user");
        when(entry.getAuthor()).thenReturn(mockAuthor);

        String description = updater.createScmChangeEntryDescription(r, entry, false, false);
        System.out.println(description);
        Assert.assertThat(description, containsString("2016-01-01 00:00:00"));
        Assert.assertThat(description, containsString("jenkins-user"));
        Assert.assertThat(description, containsString("dsgsvds2re3dsv"));
    }

    /**
     * Test formatting of scm entry change description 
     * when no format is provided (e.g. when null).
     *
     */
    @Test
    @WithoutJenkins
    public void testAppendChangeTimestampToDescriptionNullFormat() {
        //set default locale -> predictable test without explicit format
        Locale.setDefault(Locale.ENGLISH);
        
        Updater updater = new Updater(null);
        JiraSite site = mock(JiraSite.class);
        when(site.isAppendChangeTimestamp()).thenReturn(true);
        when(site.getDateTimePattern()).thenReturn(null);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        
        StringBuilder builder = new StringBuilder();
        updater.appendChangeTimestampToDescription(builder, site, calendar.getTimeInMillis());
        Assert.assertThat(builder.toString(), equalTo("1/1/16 12:00 AM"));        
    }
    
    /**
     * Test formatting of scm entry change description 
     * when no format is provided (e.g. when empty string).
     *
     */
    @Test
    @WithoutJenkins
    public void testAppendChangeTimestampToDescriptionNoFormat() {
        //set default locale -> predictable test without explicit format
        Locale.setDefault(Locale.ENGLISH);
        
        Updater updater = new Updater(null);
        JiraSite site = mock(JiraSite.class);
        when(site.isAppendChangeTimestamp()).thenReturn(true);
        when(site.getDateTimePattern()).thenReturn("");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        
        StringBuilder builder = new StringBuilder();
        updater.appendChangeTimestampToDescription(builder, site, calendar.getTimeInMillis());
        Assert.assertThat(builder.toString(), equalTo("1/1/16 12:00 AM"));        
    }

    /**
     * Test formatting of scm entry change description coverage primary wiki
     * style appendRevisionToDescription and appendAffectedFilesToDescription
     *
     */
    @Test
    public void tesDescriptionWithAffectedFiles() {
        rule.getInstance();
        Updater updater = new Updater(null);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        JiraSite site = mock(JiraSite.class);
        when(site.isAppendChangeTimestamp()).thenReturn(false);

        Run r = mock(Run.class);
        Job j = mock(Job.class);
        when(r.getParent()).thenReturn(j);
        JiraProjectProperty jiraProjectProperty = mock(JiraProjectProperty.class);
        when(j.getProperty(JiraProjectProperty.class)).thenReturn(jiraProjectProperty);
        when(jiraProjectProperty.getSite()).thenReturn(site);

        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getTimestamp()).thenReturn(calendar.getTimeInMillis());
        when(entry.getCommitId()).thenReturn("dsgsvds2re3dsv");
        User mockAuthor = mock(User.class);
        when(mockAuthor.getId()).thenReturn("jenkins-user");
        when(entry.getAuthor()).thenReturn(mockAuthor);
        
        Collection<MockAffectedFile> affectedFiles = Lists.newArrayList();
        MockAffectedFile affectedFile1 = mock(MockAffectedFile.class);
        when(affectedFile1.getEditType()).thenReturn(EditType.ADD);
        when(affectedFile1.getPath()).thenReturn("hudson/plugins/jira/File1");
        affectedFiles.add(affectedFile1);
        MockAffectedFile corruptedFile = mock(MockAffectedFile.class);
        when(corruptedFile.getEditType()).thenReturn(null);
        when(corruptedFile.getPath()).thenReturn(null);
        affectedFiles.add(corruptedFile);
        MockAffectedFile affectedFile2 = mock(MockAffectedFile.class);
        when(affectedFile2.getEditType()).thenReturn(EditType.DELETE);
        when(affectedFile2.getPath()).thenReturn("hudson/plugins/jira/File2");
        affectedFiles.add(affectedFile2);
        MockAffectedFile affectedFile3 = mock(MockAffectedFile.class);
        when(affectedFile3.getEditType()).thenReturn(EditType.EDIT);
        when(affectedFile3.getPath()).thenReturn("hudson/plugins/jira/File3");
        affectedFiles.add(affectedFile3);
        doReturn(affectedFiles).when(entry).getAffectedFiles();

        String description = updater.createScmChangeEntryDescription(r, entry, true, true);
        System.out.println(description);
        Assert.assertThat(description,
                equalTo(" (jenkins-user: rev dsgsvds2re3dsv)\n" + "* (add) hudson/plugins/jira/File1\n" + "* \n"
                        + "* (delete) hudson/plugins/jira/File2\n" + "* (edit) hudson/plugins/jira/File3\n"));
    }
    
}
