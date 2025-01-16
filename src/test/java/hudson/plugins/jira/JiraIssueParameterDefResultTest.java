package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JiraIssueParameterDefResultTest {

    private Issue issueMock;

    @BeforeEach
    void prepareMocks() {
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
    void testSummaryResult() {
        JiraIssueParameterDefinition.Result result = new JiraIssueParameterDefinition.Result(issueMock, "");

        assertThat("Summary", equalTo(result.summary));
    }

    @Test
    void testAltSummaryResultCommaSep() {
        JiraIssueParameterDefinition.Result result =
                new JiraIssueParameterDefinition.Result(issueMock, "TestField1,TestField2");

        assertThat("Field1 Field2", equalTo(result.summary));
    }

    @Test
    void testAltSummaryResultCommaSpaceSep() {
        JiraIssueParameterDefinition.Result result =
                new JiraIssueParameterDefinition.Result(issueMock, "TestField1, TestField2");

        assertThat("Field1 Field2", equalTo(result.summary));
    }

    @Test
    void testAltSummaryResultMissingFieldIgnored() {
        JiraIssueParameterDefinition.Result result =
                new JiraIssueParameterDefinition.Result(issueMock, "TestField1, TestField4");

        assertThat("Field1", equalTo(result.summary));
    }

    @Test
    void testAltSummaryResultEmptyFieldIgnored() {
        JiraIssueParameterDefinition.Result result =
                new JiraIssueParameterDefinition.Result(issueMock, "TestField1, TestField3");

        assertThat("Field1", equalTo(result.summary));
    }
}
