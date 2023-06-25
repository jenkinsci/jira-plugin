package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import hudson.model.Job;
import hudson.model.Run;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Place needed resources in src/test/resources
 *
 */
public class JiraBuildActionTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    /**
     * Test if existing serialized JiraBuildAction information will be loaded after upgrading jira
     * plugin version to with new JiraBuildAction class version introduced in PR-72.
     */
    @Test
    @LocalData
    public void binaryCompatibility() {
        assertEquals("Jenkins JiraBuildActionTest config", r.jenkins.getSystemMessage());

        Job job = r.getInstance().getItemByFullName("/project", Job.class);
        Run run = job.getBuildByNumber(2);
        assertEquals("job/project/2/", run.getUrl());

        JiraBuildAction jba = run.getAction(JiraBuildAction.class);
        assertThat(jba.getOwner().getDisplayName(), is(run.getDisplayName()));
        assertThat(jba.getIssue("JIRA-123").getSummary(), is("Issue summary"));
    }
}
