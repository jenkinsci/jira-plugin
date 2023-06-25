package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class JiraGlobalConfigurationSaveTest {

    @Rule
    public final RestartableJenkinsRule r = new RestartableJenkinsRule();

    @Test
    @Issue("JENKINS-57899")
    public void jiraSitesListSaved() throws Exception {
        String jiraUrl = "https://issues.jenkins.io/";
        r.then(r -> {
            JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
            jiraGlobalConfiguration.setSites(Collections.singletonList(new JiraSite(jiraUrl)));
            List<JiraSite> sites = jiraGlobalConfiguration.getSites();
            assertThat(sites, is(hasSize(1)));
            JiraSite jiraSite = sites.get(0);
            URL url = jiraSite.getUrl();
            assertThat(url, is(notNullValue()));
            assertThat(url.toString(), is(jiraUrl));
        });
        r.then(r -> {
            JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
            List<JiraSite> sites = jiraGlobalConfiguration.getSites();
            assertThat(sites, is(hasSize(1)));
            JiraSite jiraSite = sites.get(0);
            URL url = jiraSite.getUrl();
            assertThat(url, is(notNullValue()));
            assertThat(url.toString(), is(jiraUrl));
        });
    }
}
