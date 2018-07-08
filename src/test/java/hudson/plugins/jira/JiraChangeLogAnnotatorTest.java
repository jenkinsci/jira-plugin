package hudson.plugins.jira;

import com.google.common.collect.Sets;
import hudson.MarkupText;
import hudson.model.FreeStyleBuild;
import hudson.model.Run;
import hudson.plugins.jira.model.JiraIssue;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Kohsuke Kawaguchi
 */
public class JiraChangeLogAnnotatorTest {
    private static final String TITLE = "title with $sign to confuse TextMarkup.replace";
    private JiraSite site;

    @Before
    public void before() throws Exception {
        JiraSession session = mock(JiraSession.class);
        when(session.getProjectKeys()).thenReturn(
                Sets.newHashSet("DUMMY", "JENKINS"));

        this.site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);
        when(site.getUrl(Mockito.anyString())).thenAnswer(
                (Answer<URL>) invocation -> {
                    String id = invocation.getArguments()[0].toString();
                    return new URL("http://dummy/" + id);
                });
        when(site.getProjectKeys()).thenCallRealMethod();
        when(site.getIssuePattern()).thenCallRealMethod();

        // create inner objects
        Whitebox.setInternalState(site, "projectUpdateLock", new ReentrantLock());
        Whitebox.setInternalState(site, "issueCache", (Object)Whitebox.invokeMethod(JiraSite.class, "makeIssueCache"));
    }

    @Test
    public void testAnnotate() {
        Run run = mock(Run.class);

        when(run.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(run, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        annotator.annotate(run, null, text);

        // make sure '$' didn't confuse the JiraChangeLogAnnotator
        assertThat(text.toString(false), containsString(TITLE));
        assertThat(text.toString(false), containsString("href"));
    }

    @Test
    public void testAnnotateDisabledOnSiteLevel() {
        Run run = mock(Run.class);

        when(run.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(run, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));
        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());
        doReturn(true).when(site).getDisableChangelogAnnotations();

        assertThat(text.toString(false), not(containsString("href")));
    }

    @Test
    public void testAnnotateWf() {
        Run run = mock(Run.class);

        when(run.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(run, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        annotator.annotate(run, null, text);

        // make sure '$' didn't confuse the JiraChangeLogAnnotator
        assertThat(text.toString(false), containsString(TITLE));
    }

    /**
     * Jenkins' MarkupText#findTokens() doesn't work in our case if
     * the whole pattern matches the following word boundary character
     * (but not matching group 1).
     * Regression test for this.
     */
    @Test
    public void testWordBoundaryProblem() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        Run run = mock(Run.class);

        // old changelog annotator used MarkupText#findTokens
        // That broke because of the space after the issue id.
        MarkupText text = new MarkupText("DUMMY-4071 Text ");
        annotator.annotate(run, null, text);
        assertThat(text.toString(false), is("<a href='http://dummy/DUMMY-4071'>DUMMY-4071</a> Text "));

        text = new MarkupText("DUMMY-1,comment");
        annotator.annotate(run, null, text);
        assertThat(text.toString(false), is("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>,comment"));

        text = new MarkupText("DUMMY-1.comment");
        annotator.annotate(run, null, text);
        assertThat(text.toString(false), is("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>.comment"));

        text = new MarkupText("DUMMY-1!comment");
        annotator.annotate(run, null, text);
        assertThat(text.toString(false), is("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>!comment"));

        text = new MarkupText("DUMMY-1\tcomment");
        annotator.annotate(run, null, text);
        assertThat(text.toString(false), is("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>\tcomment"));
    }

    @Test
    public void testMatchMultipleIssueIds() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        Run run = mock(Run.class);

        MarkupText text = new MarkupText("DUMMY-1 Text DUMMY-2,DUMMY-3 DUMMY-4!");
        annotator.annotate(run, null, text);

        assertThat(text.toString(false), is(
                "<a href='http://dummy/DUMMY-1'>DUMMY-1</a> Text " +
                "<a href='http://dummy/DUMMY-2'>DUMMY-2</a>," +
                "<a href='http://dummy/DUMMY-3'>DUMMY-3</a> " +
                "<a href='http://dummy/DUMMY-4'>DUMMY-4</a>!")
        );
    }

    @Test
    public void hasProjectForIssueIsCaseInsensitive() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        assertThat(annotator.hasProjectForIssue("JENKINS-123", site), is(true));
        assertThat(annotator.hasProjectForIssue("jenKiNs-123", site), is(true));
        assertThat(annotator.hasProjectForIssue("dummy-4711", site), is(true));
    }

    @Test
    @Bug(4132)
    public void testCaseInsensitiveAnnotate() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(mock(FreeStyleBuild.class), null, text);

        assertThat(text.toString(false), is("fixed <a href='http://dummy/DUMMY-42'>DUMMY-42</a>"));
    }

    /**
     * Tests that missing issues - i.e. issues not saved to build -
     * are fetched from remote.
     */
    @Test
    @Bug(5252)
    public void testGetIssueDetailsForMissingIssues() throws IOException {
        Run run = mock(Run.class);

        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        JiraIssue issue = new JiraIssue("DUMMY-42", TITLE);
        when(site.getIssue(Mockito.anyString())).thenReturn(issue);

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(run, null, text);
        assertThat(text.toString(false), containsString(TITLE));
    }

    /**
     * Tests that no exception is thrown if user issue pattern is invalid (contains no groups)
     */
    @Test
    public void testInvalidUserPattern() {
        when(site.getIssuePattern()).thenReturn(Pattern.compile("[a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*"));

        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        MarkupText text = new MarkupText("fixed DUMMY-42");
        assertThat(text.toString(false), not(containsString(TITLE)));
    }

    /**
     * Tests that only the 1st matching group is hyperlinked and not the whole pattern.
     * Previous implementation did so.
     */
    @Test
    public void testMatchOnlyMatchGroup1() throws IOException {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());
        when(site.getIssuePattern()).thenReturn(Pattern.compile("([a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*)abc"));

        MarkupText text = new MarkupText("fixed DUMMY-42abc");
        annotator.annotate(mock(Run.class), null, text);

        assertThat(text.toString(false), is("fixed <a href='http://dummy/DUMMY-42'>DUMMY-42</a>abc"));

        // check again when issue != null:
        JiraIssue issue = new JiraIssue("DUMMY-42", TITLE);
        when(site.getIssue(Mockito.anyString())).thenReturn(issue);
        text = new MarkupText("fixed DUMMY-42abc");
        annotator.annotate(mock(Run.class), null, text);
        assertThat(
                text.toString(false),
                is("fixed <a href='http://dummy/DUMMY-42' tooltip='title with $sign to confuse TextMarkup.replace'>DUMMY-42</a>abc")
        );
    }

    /**
     * Tests that setting the "alternative Url" property actually
     * changes the link also.
     *
     * @throws Exception
     */
    @Test
    public void testAlternativeURLAnnotate() throws Exception {
        when(site.getAlternativeUrl(Mockito.anyString())).thenAnswer(
                (Answer<URL>) invocation -> {
                    String id = invocation.getArguments()[0].toString();
                    return new URL("http://altdummy/" + id);
                });

        Run run = mock(Run.class);

        when(run.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(run, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));
        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        annotator.annotate(run, null, text);

        assertThat(text.toString(false), containsString("<a href='http://altdummy/DUMMY-1'"));
    }

}
