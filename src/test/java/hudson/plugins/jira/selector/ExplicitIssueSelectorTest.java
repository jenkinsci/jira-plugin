package hudson.plugins.jira.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.util.XStream2;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExplicitIssueSelectorTest {

    private static final String TEST_KEY = "EXAMPLE-1";

    @Test
    void returnsExplicitCollections() {
        ExplicitIssueSelector jqlUpdaterIssueSelector = new ExplicitIssueSelector(Collections.singletonList(TEST_KEY));
        Set<String> foundIssueIds = jqlUpdaterIssueSelector.findIssueIds(null, null, null);
        assertThat(foundIssueIds, hasSize(1));
        assertThat(foundIssueIds.iterator().next(), equalTo(TEST_KEY));
    }

    @Test
    void theListConstructorAlsoFillsTheFieldTheFormBindsTo() {
        ExplicitIssueSelector selector = new ExplicitIssueSelector(Arrays.asList("EXAMPLE-1", "EXAMPLE-2"));

        // getIssueKeys() used to be null here, so the configuration page rendered an empty text box
        // for a selector that had keys - and the next save wiped them.
        assertEquals("EXAMPLE-1,EXAMPLE-2", selector.getIssueKeys());
        assertThat(selector.findIssueIds(null, null, null), contains("EXAMPLE-1", "EXAMPLE-2"));
    }

    @Test
    void keysAreTrimmedAndBlanksDropped() {
        ExplicitIssueSelector selector = new ExplicitIssueSelector("EXAMPLE-1, EXAMPLE-2 ,,");

        // " EXAMPLE-2" with its leading space could never match a Jira issue.
        assertThat(selector.getJiraIssueKeys(), contains("EXAMPLE-1", "EXAMPLE-2"));
    }

    @Test
    void aNullListIsTreatedAsNoKeys() {
        ExplicitIssueSelector selector = new ExplicitIssueSelector((List<String>) null);

        assertEquals("", selector.getIssueKeys());
        assertThat(selector.findIssueIds(null, null, null), hasSize(0));
    }

    @Test
    void aConfigurationHoldingOnlyTheLegacyListStillWorks() {
        String legacyXml = """
                <hudson.plugins.jira.selector.ExplicitIssueSelector>
                  <jiraIssueKeys>
                    <string>EXAMPLE-1</string>
                    <string>EXAMPLE-2</string>
                  </jiraIssueKeys>
                </hudson.plugins.jira.selector.ExplicitIssueSelector>
                """;

        ExplicitIssueSelector selector = (ExplicitIssueSelector) new XStream2().fromXML(legacyXml);

        assertEquals("EXAMPLE-1,EXAMPLE-2", selector.getIssueKeys());
        assertThat(selector.findIssueIds(null, null, null), contains("EXAMPLE-1", "EXAMPLE-2"));
    }

    @Test
    void theLegacyListIsNotWrittenBackOut() {
        XStream2 xStream = new XStream2();
        ExplicitIssueSelector selector =
                (ExplicitIssueSelector) xStream.fromXML(xStream.toXML(new ExplicitIssueSelector("EXAMPLE-1")));

        assertThat(xStream.toXML(selector), equalTo(xStream.toXML(new ExplicitIssueSelector("EXAMPLE-1"))));
        assertEquals("EXAMPLE-1", selector.getIssueKeys());
    }

    @Test
    void aConfigurationHoldingNeitherFieldDeserialisesToNoKeys() {
        String emptyXml = "<hudson.plugins.jira.selector.ExplicitIssueSelector/>";

        ExplicitIssueSelector selector = (ExplicitIssueSelector) new XStream2().fromXML(emptyXml);

        assertEquals(null, selector.getIssueKeys());
        assertThat(selector.findIssueIds(null, null, null), hasSize(0));
    }

    @Test
    void anEmptySelectorFindsNothing() {
        assertThat(new ExplicitIssueSelector().findIssueIds(null, null, null), hasSize(0));
    }
}
