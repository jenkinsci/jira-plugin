package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import hudson.MarkupText;
import hudson.model.Run;
import hudson.plugins.jira.model.JiraIssue;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * @author Kohsuke Kawaguchi
 */
@ExtendWith(MockitoExtension.class)
class JiraChangeLogAnnotatorTest {
    private static final String TITLE = "title with $sign to confuse TextMarkup.replace";

    @Mock(strictness = Mock.Strictness.LENIENT)
    private JiraSite site;

    @Mock
    private Run run;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private JiraSession session;

    @BeforeEach
    void before() throws Exception {
        when(session.getProjectKeys()).thenReturn(new HashSet(Arrays.asList("DUMMY", "JENKINS")));
        when(site.getSession(any())).thenReturn(session);
        when(site.getProjectUpdateLock()).thenReturn(new ReentrantLock());

        when(site.getUrl(Mockito.anyString())).thenAnswer((Answer<URL>) invocation -> {
            String id = invocation.getArguments()[0].toString();
            return new URL("http://dummy/" + id);
        });
        when(site.getProjectKeys(run.getParent())).thenCallRealMethod();
        when(site.getIssuePattern()).thenCallRealMethod();
    }

    @Test
    void annotate() {
        when(run.getAction(JiraBuildAction.class))
                .thenReturn(new JiraBuildAction(Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        annotator.annotate(run, null, text);

        // make sure '$' didn't confuse the JiraChangeLogAnnotator
        assertThat(text.toString(false), containsString(TITLE));
        assertThat(text.toString(false), containsString("href"));
    }

    @Test
    void annotateDisabledOnSiteLevel() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());

        doReturn(site).when(annotator).getSiteForProject(Mockito.any());
        doReturn(true).when(site).getDisableChangelogAnnotations();

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        annotator.annotate(run, null, text);

        assertThat(text.toString(false), not(containsString("href")));
    }

    @Test
    void annotateWf() {
        when(run.getAction(JiraBuildAction.class))
                .thenReturn(new JiraBuildAction(Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

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
    void wordBoundaryProblem() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

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
    void matchMultipleIssueIds() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        MarkupText text = new MarkupText("DUMMY-1 Text DUMMY-2,DUMMY-3 DUMMY-4!");
        annotator.annotate(run, null, text);

        assertThat(
                text.toString(false),
                is("<a href='http://dummy/DUMMY-1'>DUMMY-1</a> Text " + "<a href='http://dummy/DUMMY-2'>DUMMY-2</a>,"
                        + "<a href='http://dummy/DUMMY-3'>DUMMY-3</a> "
                        + "<a href='http://dummy/DUMMY-4'>DUMMY-4</a>!"));
    }

    @Test
    void hasProjectForIssueIsCaseInsensitive() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(mock(Run.class), null, text);

        assertThat(annotator.hasProjectForIssue("JENKINS-123", site, run), is(true));
        assertThat(annotator.hasProjectForIssue("jenKiNs-123", site, run), is(true));
        assertThat(annotator.hasProjectForIssue("dummy-4711", site, run), is(true));
        assertThat(annotator.hasProjectForIssue("OThEr-4711", site, run), is(false));
    }

    @Test
    @Issue("4132")
    void caseInsensitiveAnnotate() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(mock(Run.class), null, text);

        assertThat(text.toString(false), is("fixed <a href='http://dummy/DUMMY-42'>DUMMY-42</a>"));
    }

    /**
     * Tests that missing issues - i.e. issues not saved to build -
     * are fetched from remote.
     */
    @Test
    @Issue("5252")
    void getIssueDetailsForMissingIssues() throws IOException {
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
     * Tests that no exception is thrown if user issue pattern is invalid (contains
     * no groups)
     */
    @Test
    void invalidUserPattern() {
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());

        when(site.getIssuePattern()).thenReturn(Pattern.compile("[a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*"));
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(mock(Run.class), null, text);

        assertThat(text.toString(false), not(containsString(TITLE)));
    }

    /**
     * Tests that only the 1st matching group is hyperlinked and not the whole
     * pattern.
     * Previous implementation did so.
     */
    @Test
    void matchOnlyMatchGroup1() throws IOException {
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
                is(
                        "fixed <a href='http://dummy/DUMMY-42' tooltip='title with $sign to confuse TextMarkup.replace'>DUMMY-42</a>abc"));
    }

    /**
     * Tests that setting the "alternative Url" property actually
     * changes the link also.
     *
     * @throws Exception
     */
    @Test
    void alternativeURLAnnotate() throws Exception {
        when(site.getAlternativeUrl(Mockito.anyString())).thenAnswer((Answer<URL>) invocation -> {
            String id = invocation.getArguments()[0].toString();
            return new URL("http://altdummy/" + id);
        });

        Run run = mock(Run.class);

        when(run.getAction(JiraBuildAction.class))
                .thenReturn(new JiraBuildAction(Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));
        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject(Mockito.any());

        annotator.annotate(run, null, text);

        assertThat(text.toString(false), containsString("<a href='http://altdummy/DUMMY-1'"));
    }
}
