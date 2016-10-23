package hudson.plugins.jira;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.jira.model.JiraIssueField;

import hudson.plugins.jira.pipeline.IssueFieldUpdateStep;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



/**
 * @author Dmitry Frolov tekillaz.dev@gmail.com
 */
public class IssueFieldUpdateStepTest {

    @Test
    public void checkPrepareFieldId() {
    	
    	List<String> field_test= Arrays.asList(
    			"10100", 
    			"customfield_10100", 
    			"field_10100");
    	
    	List<String> field_after = Arrays.asList(
    			"customfield_10100", 
    			"customfield_10100", 
    			"customfield_field_10100");
    	
    	IssueFieldUpdateStep jifu = new IssueFieldUpdateStep(null, null, "");
    	for( int i=0; i<field_test.size(); i++ )
	    	assertEquals("Check field id conversion #" + Integer.toString(i),
	    			jifu.prepareFieldId(field_test.get(i)),
	    			field_after.get(i) );
    }
    
	@Test(expected = IOException.class)
	public void checkSelectorIsNull() throws InterruptedException, IOException {
		AbstractBuild build = mock(AbstractBuild.class);
		Launcher launcher = mock(Launcher.class);
		BuildListener listener = mock(BuildListener.class);
		EnvVars env = mock(EnvVars.class);
		AbstractProject project = mock(AbstractProject.class);
        PrintStream logger = mock(PrintStream.class);

        when(build.getParent()).thenReturn(project);
        when(build.getProject()).thenReturn(project);
        when(build.getEnvironment(listener)).thenReturn(env);
        when(listener.getLogger()).thenReturn(logger);
        
		IssueFieldUpdateStep jifu = spy(new IssueFieldUpdateStep( null, "", "") );
		jifu.perform(build, null, launcher, listener);
		assertTrue("Check selector is null", build.getResult() == Result.FAILURE);
	}
		
	@Test
	public void checkSubmit() throws InterruptedException, IOException {
		PrintStream logger = mock(PrintStream.class);
		
		final List<String> issues_after = Lists.newArrayList();
		final List<JiraIssueField> fields_after = Lists.newArrayList();
		JiraSession session = mock(JiraSession.class);
        doAnswer(new Answer<Object>() {

            public Object answer(final InvocationOnMock invocation) throws Throwable {
            	issues_after.add( (String) invocation.getArguments()[0] );
            	List<JiraIssueField> fields_tmp = (List<JiraIssueField>) invocation.getArguments()[1]; 
            	for( JiraIssueField field : fields_tmp )
            		fields_after.add( field );
                return null;
            }

        }).when(session).addFields(Matchers.anyString(), Matchers.anyListOf(JiraIssueField.class));
        
        String issue_test = "issue-10100";
        List<JiraIssueField> fields_test = Lists.newArrayList();
        for( int i=0; i<100; i++ )         	
			fields_test.add(new JiraIssueField(issue_test, "value-"+Integer.toString(10100+i)));
        
		IssueFieldUpdateStep jifu = spy(new IssueFieldUpdateStep(null, "", "") );
		jifu.submitFields(session, issue_test, fields_test, logger);
		
		assertEquals("Check issues list size", issues_after.size(), 1);
		assertEquals("Check issue value", issues_after.get(0), issue_test);
		assertEquals("Check fields list size", fields_after.size(), fields_test.size());
		for( int i=0; i<fields_test.size(); i++ ) {
			assertEquals("Check #" + Integer.toString(i) + " field id", fields_after.get(i).getId(), fields_test.get(i).getId());
			assertEquals("Check #" + Integer.toString(i) + " field value", fields_after.get(i).getValue(), fields_test.get(i).getValue());
		}
	}

}
