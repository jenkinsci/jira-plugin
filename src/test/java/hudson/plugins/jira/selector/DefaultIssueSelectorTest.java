package hudson.plugins.jira.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.User;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

class DefaultIssueSelectorTest {

    private static class MockEntry extends Entry {

        private final String msg;

        public MockEntry(String msg) {
            this.msg = msg;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return null;
        }

        @Override
        public User getAuthor() {
            return null;
        }

        @Override
        public String getMsg() {
            return this.msg;
        }
    }

    @Test
    @Issue("4132")
    void projectNamesAllowed() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);
        JiraSite site = mock(JiraSite.class);

        when(site.getIssuePattern()).thenReturn(JiraSite.DEFAULT_ISSUE_PATTERN);

        Set<? extends Entry> entries = new HashSet(Arrays.asList(
                new MockEntry("Fixed JI123-4711"),
                new MockEntry("Fixed foo_bar-4710"),
                new MockEntry("Fixed FoO_bAr-4711"),
                new MockEntry("Fixed something.\nJFoO_bAr_MULTI-4718"),
                new MockEntry("TR-123: foo"),
                new MockEntry("[ABC-42] hallo"),
                new MockEntry("#123: this one must not match"),
                new MockEntry("ABC-: this one must also not match"),
                new MockEntry("ABC-: \n\nABC-127:\nthis one should match"),
                new MockEntry("ABC-: \n\nABC-128:\nthis one should match"),
                new MockEntry("ABC-: \n\nXYZ-10:\nXYZ-20 this one too"),
                new MockEntry("Fixed DOT-4."),
                new MockEntry("Fixed DOT-5. Did it right this time")));

        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        Set<String> expected = new HashSet(Arrays.asList(
                "JI123-4711",
                "FOO_BAR-4710",
                "FOO_BAR-4711",
                "JFOO_BAR_MULTI-4718",
                "TR-123",
                "ABC-42",
                "ABC-127",
                "ABC-128",
                "XYZ-10",
                "XYZ-20",
                "DOT-4",
                "DOT-5"));

        Set<String> result = new DefaultIssueSelector().findIssueIds(build, site, listener);

        assertEquals(expected.size(), result.size());
        assertEquals(expected, result);
    }

    /**
     * Tests that the JiraIssueParameters are identified as updateable Jira
     * issues.
     */
    @Test
    @Issue("12312")
    void findIssuesWithJiraParameters() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);
        JiraSite site = mock(JiraSite.class);

        when(site.getIssuePattern()).thenReturn(JiraSite.DEFAULT_ISSUE_PATTERN);

        JiraIssueParameterValue parameter = mock(JiraIssueParameterValue.class);
        JiraIssueParameterValue parameterTwo = mock(JiraIssueParameterValue.class);
        ParametersAction action = mock(ParametersAction.class);
        List<ParameterValue> parameters = new ArrayList<>();

        when(changeLogSet.iterator()).thenReturn(Collections.EMPTY_LIST.iterator());
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getAction(ParametersAction.class)).thenReturn(action);
        when(action.getParameters()).thenReturn(parameters);
        when(parameter.getValue()).thenReturn("JIRA-123");
        when(parameterTwo.getValue()).thenReturn("JIRA-321");

        // Initial state contains zero parameters
        Set<String> ids = new DefaultIssueSelector().findIssueIds(build, site, listener);
        assertTrue(ids.isEmpty());

        parameters.add(parameter);
        ids = new DefaultIssueSelector().findIssueIds(build, site, listener);
        assertEquals(1, ids.size());
        assertEquals("JIRA-123", ids.iterator().next());

        parameters.add(parameterTwo);
        ids = new DefaultIssueSelector().findIssueIds(build, site, listener);
        assertEquals(2, ids.size());
        Set<String> expected = new TreeSet(Arrays.asList("JIRA-123", "JIRA-321"));
        assertEquals(expected, ids);
    }

    @Test
    @Issue("6043")
    void userPatternNotMatch() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = new HashSet(Arrays.asList(new MockEntry("Fixed FOO_BAR-4711")));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        Set<String> ids = new LinkedHashSet<>();
        DefaultIssueSelector.findIssues(build, ids, Pattern.compile("[(w)]"), mock(BuildListener.class));

        assertEquals(0, ids.size());
    }

    @Test
    @Issue("6043")
    void userPatternMatchTwoIssuesInOneComment() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = new HashSet(Arrays.asList(
                new MockEntry("Fixed toto [FOOBAR-4711]  [FOOBAR-21] "),
                new MockEntry("[TEST-9] with [dede]"),
                new MockEntry("toto [maven-release-plugin] prepare release foo-2.2.3")));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        Set<String> ids = new LinkedHashSet<>();
        Pattern pat = Pattern.compile("\\[(\\w+-\\d+)\\]");
        DefaultIssueSelector.findIssues(build, ids, pat, mock(BuildListener.class));
        assertEquals(3, ids.size());
        assertTrue(ids.contains("TEST-9"));
        assertTrue(ids.contains("FOOBAR-4711"));
        assertTrue(ids.contains("FOOBAR-21"));
    }

    @Test
    @Issue("6043")
    void userPatternMatch() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = new HashSet(Arrays.asList(
                new MockEntry("Fixed toto [FOOBAR-4711]"),
                new MockEntry("[TEST-9] with [dede]"),
                new MockEntry("toto [maven-release-plugin] prepare release foo-2.2.3")));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = new ArrayList<>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        Set<String> ids = new LinkedHashSet<>();
        Pattern pat = Pattern.compile("\\[(\\w+-\\d+)\\]");
        DefaultIssueSelector.findIssues(build, ids, pat, mock(BuildListener.class));
        assertEquals(2, ids.size());
        assertTrue(ids.contains("TEST-9"));
        assertTrue(ids.contains("FOOBAR-4711"));
    }

    /**
     * Tests that the default pattern doesn't match strings like 'project-1.1'.
     * These patterns are used e.g. by the maven release plugin.
     */
    @Test
    void defaultPatternNotToMatchMavenRelease() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        // commit messages like the one from the Maven release plugin must not
        // match
        Set<? extends Entry> entries = new HashSet(Arrays.asList(new MockEntry("prepare release project-4.7.1")));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        Set<String> ids = new LinkedHashSet<>();
        DefaultIssueSelector.findIssues(build, ids, JiraSite.DEFAULT_ISSUE_PATTERN, null);
        assertEquals(0, ids.size());
    }
}
