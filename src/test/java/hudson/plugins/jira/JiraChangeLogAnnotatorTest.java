package hudson.plugins.jira;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Sets;

import hudson.MarkupText;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.Run;

/**
 * @author Kohsuke Kawaguchi
 */
public class JiraChangeLogAnnotatorTest {
    private static final String TITLE = "title with $sign to confuse TextMarkup.replace";
    private JiraSite site;

    @Before
    public void before() throws IOException {
        JiraSession session = mock(JiraSession.class);
        when(session.getProjectKeys()).thenReturn(
                Sets.newHashSet("DUMMY", "JENKINS"));

        this.site = mock(JiraSite.class);
        when(site.getSession()).thenReturn(session);
        when(site.getUrl(Mockito.anyString())).thenAnswer(
                new Answer<URL>() {
                    public URL answer(InvocationOnMock invocation)
                            throws Throwable {
                        String id = invocation.getArguments()[0].toString();
                        return new URL("http://dummy/" + id);
                    }
                });
        when(site.existsIssue(Mockito.anyString())).thenCallRealMethod();
        when(site.getProjectKeys()).thenCallRealMethod();
        when(site.getIssuePattern()).thenCallRealMethod();
        when(site.readResolve()).thenCallRealMethod();
        site.readResolve(); // create the lock object
    }

    @Test
    public void testAnnotate() throws Exception {
        FreeStyleBuild b = mock(FreeStyleBuild.class);

        when(b.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(b, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        annotator.annotate(b, null, text);

        // make sure '$' didn't confuse the JiraChangeLogAnnotator
        Assert.assertTrue(text.toString(false).contains(TITLE));
    }

    @Test
    public void testAnnotateWf() throws Exception {
        Run b = mock(Run.class);

        when(b.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(b, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        annotator.annotate(b, null, text);

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
    public void testWordBoundaryProblem() throws Exception {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        FreeStyleBuild b = mock(FreeStyleBuild.class);

        // old changelog annotator used MarkupText#findTokens
        // That broke because of the space after the issue id.
        MarkupText text = new MarkupText("DUMMY-4071 Text ");
        annotator.annotate(b, null, text);
        Assert.assertEquals("<a href='http://dummy/DUMMY-4071'>DUMMY-4071</a> Text ", text.toString(false));


        text = new MarkupText("DUMMY-1,comment");
        annotator.annotate(b, null, text);
        Assert.assertEquals("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>,comment", text.toString(false));

        text = new MarkupText("DUMMY-1.comment");
        annotator.annotate(b, null, text);
        Assert.assertEquals("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>.comment", text.toString(false));

        text = new MarkupText("DUMMY-1!comment");
        annotator.annotate(b, null, text);
        Assert.assertEquals("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>!comment", text.toString(false));

        text = new MarkupText("DUMMY-1\tcomment");
        annotator.annotate(b, null, text);
        Assert.assertEquals("<a href='http://dummy/DUMMY-1'>DUMMY-1</a>\tcomment", text.toString(false));
    }

    @Test
    public void testMatchMultipleIssueIds() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        FreeStyleBuild b = mock(FreeStyleBuild.class);

        MarkupText text = new MarkupText("DUMMY-1 Text DUMMY-2,DUMMY-3 DUMMY-4!");
        annotator.annotate(b, null, text);

        Assert.assertEquals("<a href='http://dummy/DUMMY-1'>DUMMY-1</a> Text " +
                "<a href='http://dummy/DUMMY-2'>DUMMY-2</a>," +
                "<a href='http://dummy/DUMMY-3'>DUMMY-3</a> " +
                "<a href='http://dummy/DUMMY-4'>DUMMY-4</a>!",
                text.toString(false));
    }

    @Test
    @Bug(4132)
    public void testCaseInsensitiveAnnotate() throws IOException {

        Assert.assertTrue(site.existsIssue("JENKINS-123"));
        Assert.assertTrue(site.existsIssue("jenKiNs-123"));
        Assert.assertTrue(site.existsIssue("dummy-4711"));

        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(mock(FreeStyleBuild.class), null, text);

        Assert.assertEquals("fixed <a href='http://dummy/DUMMY-42'>DUMMY-42</a>", text.toString(false));
    }

    /**
     * Tests that missing issues - i.e. issues not saved to build -
     * are fetched from remote.
     */
    @Test
    @Bug(5252)
    public void testGetIssueDetailsForMissingIssues() throws IOException {
        FreeStyleBuild b = mock(FreeStyleBuild.class);

        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        JiraIssue issue = new JiraIssue("DUMMY-42", TITLE);
        when(site.getIssue(Mockito.anyString())).thenReturn(issue);

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(b, null, text);
        Assert.assertTrue(text.toString(false).contains(TITLE));
    }

    /**
     * Tests that no exception is thrown if user issue pattern is invalid (contains no groups)
     */
    @Test
    public void testInvalidUserPattern() throws IOException {
        when(site.getIssuePattern()).thenReturn(Pattern.compile("[a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*"));

        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        FreeStyleBuild b = mock(FreeStyleBuild.class);

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(b, null, text);
        Assert.assertFalse(text.toString(false).contains(TITLE));
    }

    /**
     * Tests that only the 1st matching group is hyperlinked and not the whole pattern.
     * Previous implementation did so.
     */
    @Test
    public void testMatchOnlyMatchGroup1() throws IOException {

        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        when(site.getIssuePattern()).thenReturn(Pattern.compile("([a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*)abc"));

        MarkupText text = new MarkupText("fixed DUMMY-42abc");
        annotator.annotate(mock(FreeStyleBuild.class), null, text);

        Assert.assertEquals("fixed <a href='http://dummy/DUMMY-42'>DUMMY-42</a>abc", text.toString(false));

        // check again when issue != null:
        JiraIssue issue = new JiraIssue("DUMMY-42", TITLE);
        when(site.getIssue(Mockito.anyString())).thenReturn(issue);
        text = new MarkupText("fixed DUMMY-42abc");
        annotator.annotate(mock(FreeStyleBuild.class), null, text);
        Assert.assertEquals("fixed <a href='http://dummy/DUMMY-42' tooltip='title with $sign to confuse TextMarkup.replace'>DUMMY-42</a>abc", text.toString(false));
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
                new Answer<URL>() {
                    public URL answer(InvocationOnMock invocation)
                            throws Throwable {
                        String id = invocation.getArguments()[0].toString();
                        return new URL("http://altdummy/" + id);
                    }
                });

        FreeStyleBuild b = mock(FreeStyleBuild.class);

        when(b.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(b, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));
        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());

        annotator.annotate(b, null, text);

        Assert.assertTrue(text.toString(false).contains("<a href='http://altdummy/DUMMY-1'"));
    }

}
