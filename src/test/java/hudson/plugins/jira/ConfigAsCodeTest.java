package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class ConfigAsCodeTest {

    @Test
    @ConfiguredWithCode("multiple-sites.yml")
    void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule r) throws Exception {
        List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
        assertThat(sites, hasSize(2));
        assertEquals(
                "https://issues.jenkins-ci.org/",
                Objects.requireNonNull(sites.get(0).getUrl()).toExternalForm());
        assertEquals(
                "https://jira.com/",
                Objects.requireNonNull(sites.get(1).getUrl()).toExternalForm());
    }

    @Test
    @ConfiguredWithCode("single-site.yml")
    void shouldExportConfigurationAsCode(JenkinsConfiguredWithCodeRule r) throws Exception {
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
