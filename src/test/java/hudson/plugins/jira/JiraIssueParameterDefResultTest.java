package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterDefinition;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.Matchers.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class JiraIssueParameterDefResultTest {

    private Issue issueMock;

    @Before
    public void prepareMocks() {
        issueMock = mock(Issue.class);
        IssueField fieldMock1 = mock(IssueField.class);
        IssueField fieldMock2 = mock(IssueField.class);

        when(issueMock.getFieldByName("TestField1")).thenReturn(fieldMock1);
        when(issueMock.getFieldByName("TestField2")).thenReturn(fieldMock2);
        when(issueMock.getSummary()).thenReturn("Summary");
        when(fieldMock1.getValue()).thenReturn("Field1");
        when(fieldMock2.getValue()).thenReturn("Field2");

    }

    @Test
    public void testSummaryResult() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock, "");

        assertThat("Summary", equalTo(result.summary));
    }

    @Test
    public void testAltSummaryResult() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock,
                "TestField1,TestField2");

        assertThat("Field1 Field2 ", equalTo(result.summary));
    }
}
