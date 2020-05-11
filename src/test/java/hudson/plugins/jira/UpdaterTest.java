package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.jira.model.JiraIssue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.EditType;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test case for the Jira {@link Updater}.
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
    public void getScmCommentsFromPreviousBuilds() {
        final FreeStyleProject project = mock(FreeStyleProject.class);
        final FreeStyleBuild build1 = mock(FreeStyleBuild.class);
        final MockEntry entry1 = new MockEntry("FOOBAR-1: The first build");
        {
            ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
            when(build1.getChangeSet()).thenReturn(changeLogSet);
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<>();
            changeSets.add(changeLogSet);
            when(build1.getChangeSets()).thenReturn(changeSets);
            when(build1.getResult()).thenReturn(Result.FAILURE);
            doReturn(project).when(build1).getProject();

            doReturn(new JiraCarryOverAction(new HashSet(Arrays.asList( new JiraIssue( "FOOBAR-1", null)))))
                    .when(build1).getAction(JiraCarryOverAction.class);

            final Set<? extends Entry> entries = new HashSet(Arrays.asList(entry1));
            when(changeLogSet.iterator()).thenAnswer(invocation -> entries.iterator());
        }

        final FreeStyleBuild build2 = mock(FreeStyleBuild.class);
        final MockEntry entry2 = new MockEntry("FOOBAR-2: The next build");
        {
            ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
            when(build2.getChangeSet()).thenReturn(changeLogSet);
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<>();
            changeSets.add(changeLogSet);
            when(build2.getChangeSets()).thenReturn(changeSets);
            when(build2.getPreviousCompletedBuild()).thenReturn(build1);
            when(build2.getResult()).thenReturn(Result.SUCCESS);
            doReturn(project).when(build2).getProject();

            final Set<? extends Entry> entries = new HashSet(Arrays.asList(entry2));
            when(changeLogSet.iterator()).thenAnswer(invocation -> entries.iterator());
        }

        final List<Comment> comments = new ArrayList();
        final JiraSession session = mock(JiraSession.class);
        doAnswer((Answer<Object>) invocation -> {
            Comment rc = Comment.createWithGroupLevel((String) invocation.getArguments()[1], (String) invocation.getArguments()[2]);
            comments.add(rc);
            return null;
        }).when(session).addComment(anyString(), anyString(), anyString(), anyString());

        this.updater = new Updater(build2.getProject().getScm());        
        
        final Set<JiraIssue> ids = new HashSet(Arrays.asList(new JiraIssue("FOOBAR-1", null), new JiraIssue("FOOBAR-2", null)));
        updater.submitComments(build2, System.out, "http://jenkins", ids, session, false, false, "", "");

        Assert.assertEquals(2, comments.size());
        assertThat(comments.get(0).getBody(), Matchers.containsString(entry1.getMsg()));
        assertThat(comments.get(1).getBody(), Matchers.containsString(entry2.getMsg()));
    }

    /**
     * Tests that the generated comment matches the expectations -
     * especially that the Jira id is not stripped from the comment.
     */
    @Test
    @org.jvnet.hudson.test.Issue("4572")
    public void comment() {
        // mock Jira session:
        JiraSession session = mock(JiraSession.class);
        final Issue mockIssue = Mockito.mock( Issue.class);
        when(session.getIssue(Mockito.anyString())).thenReturn(mockIssue);

        final List<String> comments = new ArrayList<>();

        Answer answer = (Answer<Object>) invocation -> {
            comments.add((String) invocation.getArguments()[1]);
            return null;
        };
        doAnswer(answer).when(session).addComment(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // mock build:
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        FreeStyleProject project = mock(FreeStyleProject.class);
        when(build.getProject()).thenReturn(project);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getResult()).thenReturn(Result.SUCCESS);

        Set<? extends Entry> entries = new HashSet(Arrays.asList(new MockEntry("Fixed FOOBAR-4711")));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        // test:
        Set<JiraIssue> ids = new HashSet(Arrays.asList(new JiraIssue("FOOBAR-4711", "Title")));
        Updater updaterCurrent = new Updater(build.getParent().getScm());
        updaterCurrent.submitComments(build,
                System.out, "http://jenkins", ids, session, false, false, "", "");

        Assert.assertEquals(1, comments.size());
        String comment = comments.get(0);

        Assert.assertTrue(comment.contains("FOOBAR-4711"));

        // must also work case-insensitively (JENKINS-4132)
        comments.clear();
        entries = new HashSet(Arrays.asList(new MockEntry("Fixed Foobar-4711")));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());
        ids = new HashSet(Arrays.asList(new JiraIssue("FOOBAR-4711", "Title")));

        updaterCurrent.submitComments(build,
                System.out, "http://jenkins", ids, session, false, false, "", "");

        Assert.assertEquals(1, comments.size());
        comment = comments.get(0);

        Assert.assertTrue(comment.contains("Foobar-4711"));

    }

    /**
    /**
     * Checks if issues are correctly removed from the carry over list.
     */
    @Test
    @org.jvnet.hudson.test.Issue("17156")
    @WithoutJenkins
    public void issueIsRemovedFromCarryOverListAfterSubmission() throws RestClientException {
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
        Set<JiraIssue> issues = new HashSet(Arrays.asList(firstIssue, secondIssue, forbiddenIssue, thirdIssue));

        // mock Jira session:
        JiraSession session = mock(JiraSession.class);

        final List<Comment> comments = new ArrayList<>();

        Answer answer = (Answer<Object>) invocation -> {
            Comment c = Comment.createWithGroupLevel((String) invocation.getArguments()[0], (String) invocation.getArguments()[1]);
            comments.add(c);
            return null;
        };

        doAnswer(answer).when(session).addComment(eq(firstIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doAnswer(answer).when(session).addComment(eq(secondIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doAnswer(answer).when(session).addComment(eq(thirdIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // issue for the caught exception
        doThrow(new RestClientException(new Throwable(), 404)).when(session).addComment(eq(deletedIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        doThrow(new RestClientException(new Throwable(), 403)).when(session).addComment(eq(forbiddenIssue.getKey()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());


        final String groupVisibility = "";
        final String roleVisibility = "";

        Updater updaterCurrent = new Updater(build.getParent().getScm());

        updaterCurrent.submitComments(
                build, System.out, "http://jenkins", issues, session, false, false, groupVisibility, roleVisibility
        );

        // expected issue list
        final Set<JiraIssue> expectedIssuesToCarryOver = new LinkedHashSet();
        expectedIssuesToCarryOver.add(forbiddenIssue);
        assertThat(issues, is(expectedIssuesToCarryOver));
    }
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Test that workflow job - run instance of type WorkflowJob - can
     * return changeSets using java reflection api
     *
     */
    @Test
    @WithoutJenkins
    public void getChangesUsingReflectionForWorkflowJob() throws IOException {
        Jenkins jenkins = mock(Jenkins.class);
        
        when(jenkins.getRootDirFor(Mockito.anyObject())).thenReturn(folder.getRoot());
        WorkflowJob workflowJob = new WorkflowJob(jenkins, "job");
        WorkflowRun workflowRun = new WorkflowRun(workflowJob);
        
        ChangeLogSet.createEmpty(workflowRun);
        
        List<ChangeLogSet<? extends Entry>> changesUsingReflection = RunScmChangeExtractor.getChangesUsingReflection(workflowRun);
        Assert.assertNotNull(changesUsingReflection);
        Assert.assertTrue(changesUsingReflection.isEmpty());
    }
    
    @WithoutJenkins
    @Test(expected=IllegalArgumentException.class)
    public void getChangesUsingReflectionForunknownJob() {
        Run run = mock(Run.class);
        RunScmChangeExtractor.getChangesUsingReflection(run);
    }

    /**
     * Test formatting of scm entry change time.
     *
     */
    @Test
    @WithoutJenkins
    public void appendChangeTimestampToDescription() {
        Updater updater = new Updater(null);
        StringBuilder description = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        JiraSite site = mock(JiraSite.class);
        when(site.getDateTimePattern()).thenReturn("yyyy-MM-dd HH:mm:ss");
        updater.appendChangeTimestampToDescription(description, site, calendar.getTimeInMillis());
        System.out.println(description.toString());
        assertThat(description.toString(), equalTo("2016-01-01 00:00:00"));
    }

    /**
     * Test formatting of scm entry change description.
     *
     */
    @Test
    public void dateTimeInChangeDescription() {
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
        assertThat(description, containsString("2016-01-01 00:00:00"));
        assertThat(description, containsString("jenkins-user"));
        assertThat(description, containsString("dsgsvds2re3dsv"));
    }

    /**
     * Test formatting of scm entry change description 
     * when no format is provided (e.g. when null).
     *
     */
    @Test
    @WithoutJenkins
    public void appendChangeTimestampToDescriptionNullFormat() {
        //set default locale -> predictable test without explicit format
        Locale.setDefault(Locale.ENGLISH);
        
        Updater updater = new Updater(null);
        JiraSite site = mock(JiraSite.class);
        when(site.isAppendChangeTimestamp()).thenReturn(true);
        when(site.getDateTimePattern()).thenReturn(null);
        //when(site.getDateTimePattern()).thenReturn("d/M/yy hh:mm a");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        
        StringBuilder builder = new StringBuilder();
        updater.appendChangeTimestampToDescription(builder, site, calendar.getTimeInMillis());
        assertThat(builder.toString(), equalTo("1/1/16 12:00 AM"));        
    }
    
    /**
     * Test formatting of scm entry change description 
     * when no format is provided (e.g. when empty string).
     *
     */
    @Test
    @WithoutJenkins
    public void appendChangeTimestampToDescriptionNoFormat() {
        //set default locale -> predictable test without explicit format
        Locale.setDefault(Locale.ENGLISH);
        
        Updater updater = new Updater(null);
        JiraSite site = mock(JiraSite.class);
        when(site.isAppendChangeTimestamp()).thenReturn(true);
        when(site.getDateTimePattern()).thenReturn(null);
        //when(site.getDateTimePattern()).thenReturn("d/M/yy hh:mm a");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, 0, 1, 0, 0, 0);
        
        StringBuilder builder = new StringBuilder();
        updater.appendChangeTimestampToDescription(builder, site, calendar.getTimeInMillis());
        assertThat(builder.toString(), equalTo("1/1/16 12:00 AM"));        
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
        
        Collection<MockAffectedFile> affectedFiles = new ArrayList();
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
        assertThat(description,
                equalTo(" (jenkins-user: rev dsgsvds2re3dsv)\n" + "* (add) hudson/plugins/jira/File1\n" + "* \n"
                        + "* (delete) hudson/plugins/jira/File2\n" + "* (edit) hudson/plugins/jira/File3\n"));
    }
    
}
