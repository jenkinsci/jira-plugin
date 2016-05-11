package hudson.plugins.jira.selector;

import com.google.common.collect.Sets;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.User;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.listissuesparameter.JiraIssueParameterValue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatternIssueSelectorTest {

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
    public void testDefaultPatternMatch() {
        JiraSite site = mock(JiraSite.class);
        when(site.getIssuePattern()).thenReturn(JiraSite.DEFAULT_ISSUE_PATTERN);
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed toto [FOOBAR-4711]"),
                new MockEntry("[TEST-9] with [dede]"),
                new MockEntry("toto [maven-release-plugin] prepare release foo-2.2.3"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        PatternIssueSelector pis = new PatternIssueSelector();

        Set<String> ids = pis.findIssueIds(build, site, mock(BuildListener.class));
        Assert.assertEquals(2, ids.size());
        Assert.assertTrue(ids.contains("TEST-9"));
        Assert.assertTrue(ids.contains("FOOBAR-4711"));
    }


    @Test
    public void testUserPatternMatch() {
        JiraSite site = mock(JiraSite.class);
        when(site.getIssuePattern()).thenReturn(JiraSite.DEFAULT_ISSUE_PATTERN);
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed toto [FOOBAR-4711]"),
                new MockEntry("[TEST-9] with [dede]"),
                new MockEntry("toto [maven-release-plugin] prepare release foo-2.2.3"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        List<ChangeLogSet<? extends Entry>> changeSets = new ArrayList<ChangeLogSet<? extends Entry>>();
        changeSets.add(changeLogSet);
        when(build.getChangeSets()).thenReturn(changeSets);

        PatternIssueSelector pis = new PatternIssueSelector();
        pis.setIssuePattern("([a-zA-Z][TEST_]+-[1-9][0-9]*)([^.]|\\.[^0-9]|\\.$|$)");

        Set<String> ids = pis.findIssueIds(build, site, mock(BuildListener.class));
        Assert.assertEquals(1, ids.size());
        Assert.assertTrue(ids.contains("TEST-9"));
        Assert.assertFalse(ids.contains("FOOBAR-4711"));
    }

    /**
     * Tests that the default pattern doesn't match strings like 'project-1.1'.
     * These patterns are used e.g. by the maven release plugin.
     */
    @Test
    public void testDefaultPattertNotToMatchMavenRelease() {
        JiraSite site = mock(JiraSite.class);
        when(site.getIssuePattern()).thenReturn(JiraSite.DEFAULT_ISSUE_PATTERN);
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        // commit messages like the one from the Maven release plugin must not
        // match
        Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("prepare release project-4.7.1"));
        when(changeLogSet.iterator()).thenReturn(entries.iterator());

        PatternIssueSelector pis = new PatternIssueSelector();

        Set<String> ids = pis.findIssueIds(build, site, mock(BuildListener.class));

        Assert.assertEquals(0, ids.size());
    }

}
