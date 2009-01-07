package hudson.plugins.jira;

import org.jvnet.hudson.test.HudsonTestCase;
import hudson.model.FreeStyleProject;
import hudson.model.FreeStyleBuild;
import hudson.MarkupText;

import java.util.Collections;
import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
public class JiraChangeLogAnnotatorTest extends HudsonTestCase {
    private static final String TITLE = "title with $sign to confuse TextMarkup.replace";

    public void testAnnotate() throws Exception {
        JiraProjectProperty.DESCRIPTOR.setSites(new MockJiraSite());

        // prepare a build for testing JiraChangeLogAnnotator
        FreeStyleProject p = createFreeStyleProject();
        p.addProperty(new JiraProjectProperty(null));
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        b.addAction(new JiraBuildAction(b, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        new JiraChangeLogAnnotator().annotate(b,null, text);
        System.out.println(text);

        // make sure '$' didn't confuse the JiraChangeLogAnnotator
        assertTrue(text.toString().contains(TITLE));
    }
}
