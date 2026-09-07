package hudson.plugins.jira.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class JiraIssueFieldTest {

    @Test
    void comparingTwoFieldsTerminates() {
        JiraIssueField labels = new JiraIssueField("labels", Arrays.asList("a", "b"));
        JiraIssueField duedate = new JiraIssueField("duedate", "2026-12-24");

        // compareTo used to call itself, so any comparison threw StackOverflowError.
        assertTrue(labels.compareTo(duedate) > 0);
        assertTrue(duedate.compareTo(labels) < 0);
        assertEquals(0, labels.compareTo(new JiraIssueField("labels", "something else")));
    }

    @Test
    void fieldsSortByTheirId() {
        List<JiraIssueField> fields = new ArrayList<>(Arrays.asList(
                new JiraIssueField("summary", "s"),
                new JiraIssueField("customfield_10100", "c"),
                new JiraIssueField("labels", "l")));

        Collections.sort(fields);

        assertThat(
                fields.stream().map(JiraIssueField::getId).toList(),
                contains("customfield_10100", "labels", "summary"));
    }
}
