package hudson.plugins.jira.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraGlobalConfiguration;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class IssueSelectorStepTest {

    @Inject
    private IssueSelectorStep.DescriptorImpl descriptor;

    @Mock
    private AbstractIssueSelector issueSelector;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private TaskListener listener;

    @Mock
    private PrintStream logger;

    @Mock
    private Run run;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private StepContext stepContext;

    private IssueSelectorStep.IssueSelectorStepExecution stepExecution;
    private IssueSelectorStep subject;

    @BeforeEach
    void setUp(JenkinsRule jenkinsRule) throws Exception {
        jenkinsRule.getInstance().getInjector().injectMembers(this);

        when(listener.getLogger()).thenReturn(logger);
        when(stepContext.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(stepContext.get(Run.class)).thenReturn(run);
        when(stepContext.get(TaskListener.class)).thenReturn(listener);

        subject = (IssueSelectorStep) descriptor.newInstance(new HashMap<>());
        subject.setIssueSelector(issueSelector);
    }

    @Test
    void runWithNullSite() throws Exception {
        stepExecution = spy((IssueSelectorStep.IssueSelectorStepExecution) subject.start(stepContext));
        // doReturn(Optional.empty()).when(stepExecution).getOptionalJiraSite();

        doCallRealMethod().when(run).getParent();
        Set<String> ids = stepExecution.run();

        verify(run, times(1)).setResult(Result.FAILURE);
        assertThat(ids, hasSize(0));
    }

    @Test
    void run() throws Exception {
        JiraSite site = mock(JiraSite.class);
        JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
        jiraGlobalConfiguration.setSites(Collections.singletonList(site));
        stepExecution = spy((IssueSelectorStep.IssueSelectorStepExecution) subject.start(stepContext));
        doCallRealMethod().when(run).getParent();

        stepExecution.run();

        verify(run, times(0)).setResult(Result.FAILURE);
        verify(issueSelector).findIssueIds(run, site, listener);
    }
}
