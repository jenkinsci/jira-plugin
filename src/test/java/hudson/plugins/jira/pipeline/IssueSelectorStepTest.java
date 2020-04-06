package hudson.plugins.jira.pipeline;

import com.google.inject.Inject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueSelectorStepTest {

    @Inject
    private IssueSelectorStep.DescriptorImpl descriptor;

    @Mock
    private AbstractIssueSelector issueSelector;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;
    @Mock
    private Run run;
    @Mock
    private StepContext stepContext;

    private IssueSelectorStep.IssueSelectorStepExecution stepExecution;
    private IssueSelectorStep subject;

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();


    @Before
    public void setUp() throws Exception {
        jenkinsRule.getInstance().getInjector().injectMembers(this);

        when(listener.getLogger()).thenReturn(logger);
        when(stepContext.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(stepContext.get(Run.class)).thenReturn(run);
        when(stepContext.get(TaskListener.class)).thenReturn(listener);

        subject = (IssueSelectorStep) descriptor.newInstance(new HashMap<>());
        subject.setIssueSelector(issueSelector);
    }

    @Test
    public void runWithNullSite() throws Exception {
        stepExecution = spy((IssueSelectorStep.IssueSelectorStepExecution) subject.start(stepContext));
        doReturn(Optional.empty()).when(stepExecution).getOptionalJiraSite();

        Set<String> ids = stepExecution.run();

        verify(run, times(1)).setResult(Result.FAILURE);
        assertThat(ids, hasSize(0));
    }

    @Test
    public void run() throws Exception {
        stepExecution = spy((IssueSelectorStep.IssueSelectorStepExecution) subject.start(stepContext));
        JiraSite site = mock(JiraSite.class);

        //when(stepExecution.getOptionalJiraSite()).thenReturn( Optional.of( site ) );
        doReturn(Optional.of(site)).when(stepExecution).getOptionalJiraSite();

        stepExecution.run();

        verify(run, times(0)).setResult(Result.FAILURE);
        verify(issueSelector).findIssueIds(run, site, listener);
    }
}
