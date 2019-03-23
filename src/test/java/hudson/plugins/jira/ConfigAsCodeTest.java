package hudson.plugins.jira;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.List;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConfigAsCodeTest {

    @Rule
    public JenkinsRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("multiple-sites.yml")
    public void shouldSupportConfigurationAsCode() throws Exception {
        List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
        assertThat(sites, hasSize(2));
        Assert.assertEquals("https://issues.jenkins-ci.org/", Objects
            .requireNonNull(sites.get(0).getUrl()).toExternalForm());
        Assert.assertEquals("https://jira.com/", Objects
            .requireNonNull(sites.get(1).getUrl()).toExternalForm());

    }

    @Test
    @ConfiguredWithCode("single-site.yml")
    public void shouldExportConfigurationAsCode() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(JiraGlobalConfiguration.class);
        final CNode node = c.describe(JiraGlobalConfiguration.get(), context);
        assertNotNull(node);
        final Mapping mapping = node.asMapping();
        Mapping sites = mapping.get("sites").asSequence().get(0).asMapping();

        assertThat(sites.getScalarValue("url"), is("https://jira.com/"));
    }

}
