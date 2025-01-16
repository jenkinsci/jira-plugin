package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JiraGlobalConfigurationSaveTest {

    @Test
    @Issue("JENKINS-57899")
    void jiraSitesListSaved(JenkinsRule r) throws Throwable {
        String jiraUrl = "https://issues.jenkins.io/";
        JiraGlobalConfiguration jiraGlobalConfiguration = JiraGlobalConfiguration.get();
        jiraGlobalConfiguration.setSites(Collections.singletonList(new JiraSite(jiraUrl)));
        List<JiraSite> sites = jiraGlobalConfiguration.getSites();
        assertThat(sites, is(hasSize(1)));
        JiraSite jiraSite = sites.get(0);
        URL url = jiraSite.getUrl();
        assertThat(url, is(notNullValue()));
        assertThat(url.toString(), is(jiraUrl));

        r.restart();

        jiraGlobalConfiguration = JiraGlobalConfiguration.get();
        sites = jiraGlobalConfiguration.getSites();
        assertThat(sites, is(hasSize(1)));
        jiraSite = sites.get(0);
        url = jiraSite.getUrl();
        assertThat(url, is(notNullValue()));
        assertThat(url.toString(), is(jiraUrl));
    }
}
