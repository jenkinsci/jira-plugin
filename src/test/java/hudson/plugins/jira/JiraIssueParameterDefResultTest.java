package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterDefinition;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JiraIssueParameterDefResultTest {

    private Issue issueMock;

    @Before
    public void prepareMocks() {
        issueMock = mock(Issue.class);
        IssueField fieldMock1 = mock(IssueField.class);
        IssueField fieldMock2 = mock(IssueField.class);
        IssueField fieldMock3 = mock(IssueField.class);

        when(issueMock.getFieldByName("TestField1")).thenReturn(fieldMock1);
        when(issueMock.getFieldByName("TestField2")).thenReturn(fieldMock2);
        when(issueMock.getFieldByName("TestField3")).thenReturn(fieldMock3);
        when(issueMock.getFieldByName("TestField4")).thenReturn(null);
        when(issueMock.getSummary()).thenReturn("Summary");
        when(fieldMock1.getValue()).thenReturn("Field1");
        when(fieldMock2.getValue()).thenReturn("Field2");
        when(fieldMock3.getValue()).thenReturn("");

    }

    @Test
    public void testSummaryResult() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock, "");

        assertThat("Summary", equalTo(result.summary));
    }

    @Test
    public void testAltSummaryResultCommaSep() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock,
                "TestField1,TestField2");

        assertThat("Field1 Field2", equalTo(result.summary));
    }

    @Test
    public void testAltSummaryResultCommaSpaceSep() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock,
                "TestField1, TestField2");

        assertThat("Field1 Field2", equalTo(result.summary));
    }

    @Test
    public void testAltSummaryResultMissingFieldIgnored() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock,
                "TestField1, TestField4");

        assertThat("Field1", equalTo(result.summary));
    }

    @Test
    public void testAltSummaryResultEmptyFieldIgnored() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock,
                "TestField1, TestField3");

        assertThat("Field1", equalTo(result.summary));
    }
}
