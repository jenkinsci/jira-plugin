package hudson.plugins.jira;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import hudson.MarkupText;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mockito;

import com.google.common.collect.Sets;

/**
 * @author Kohsuke Kawaguchi
 */
public class JiraChangeLogAnnotatorTest  {
    private static final String TITLE = "title with $sign to confuse TextMarkup.replace";
    private JiraSite site;
    
    @Before
    public void before() throws IOException, ServiceException {
        JiraSession session = mock(JiraSession.class);
        when(session.getProjectKeys()).thenReturn(
                Sets.newHashSet("DUMMY", "HUDSON"));
        
        this.site = mock(JiraSite.class);
        when(site.createSession()).thenReturn(session);
        when(site.getUrl(Mockito.anyString())).thenReturn(new URL("http://dummy"));
        when(site.existsIssue(Mockito.anyString())).thenCallRealMethod();
        when(site.getProjectKeys()).thenCallRealMethod();
    }

    @Test
    public void testAnnotate() throws Exception {
        FreeStyleBuild b = mock(FreeStyleBuild.class);
        
        when(b.getAction(JiraBuildAction.class)).thenReturn(new JiraBuildAction(b, Collections.singleton(new JiraIssue("DUMMY-1", TITLE))));

        MarkupText text = new MarkupText("marking up DUMMY-1.");
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        
        annotator.annotate(b,null, text);

        // make sure '$' didn't confuse the JiraChangeLogAnnotator
        Assert.assertTrue(text.toString().contains(TITLE));
    }
    
    @Test
    @Bug(4132)
    public void testCaseInsensitiveAnnotate() throws IOException, ServiceException {
        
        Assert.assertTrue(site.existsIssue("HUDSON-123"));
        Assert.assertTrue(site.existsIssue("huDsOn-123"));
        Assert.assertTrue(site.existsIssue("dummy-4711"));
        
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        
        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(mock(FreeStyleBuild.class), null, text);
        
        Assert.assertEquals("fixed <a href='http://dummy'>DUMMY-42</a>", text.toString());
    }
    
    @Test
    @Bug(5252)
    public void testGetIssueDetailsFromUpstreamJob() {
        FreeStyleBuild b = mock(FreeStyleBuild.class);
        
        FreeStyleBuild upstream1 = mock(FreeStyleBuild.class);
        FreeStyleBuild upstream2 = mock(FreeStyleBuild.class);
        
        FreeStyleProject upstreamProj1 = mock(FreeStyleProject.class);
        when(upstreamProj1.getBuildByNumber(Mockito.anyInt())).thenReturn(upstream1);
        
        FreeStyleProject upstreamProj2 = mock(FreeStyleProject.class);
        when(upstreamProj2.getBuildByNumber(Mockito.anyInt())).thenReturn(upstream2);
        
        @SuppressWarnings("unchecked")
        Map<AbstractProject, Integer> upstreamBuilds = new HashMap<AbstractProject, Integer>();
        upstreamBuilds.put(upstreamProj1, Integer.valueOf(1));
        upstreamBuilds.put(upstreamProj2, Integer.valueOf(1));
        
        when(b.getTransitiveUpstreamBuilds()).thenReturn(upstreamBuilds);
        
        JiraIssue issue = new JiraIssue("DUMMY-42", TITLE);
        JiraBuildAction action = new JiraBuildAction(upstream1, Collections.singletonList(issue));
        when(upstream1.getAction(JiraBuildAction.class)).thenReturn(action);
        
        JiraChangeLogAnnotator annotator = spy(new JiraChangeLogAnnotator());
        doReturn(site).when(annotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        
        MarkupText text = new MarkupText("fixed DUMMY-42");
        annotator.annotate(b, null, text);
        Assert.assertTrue(text.toString().contains(TITLE));
    }
}
