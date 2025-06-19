package hudson.plugins.jira.versionparameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import hudson.cli.CLICommand;
import hudson.model.ParameterValue;
import org.junit.jupiter.api.Test;

class JiraVersionParameterDefinitionTest {

    @Test
    void parameterValueMethodOverrides() throws Exception {
        JiraVersionParameterDefinition definition =
                new JiraVersionParameterDefinition("pname", "pdesc", "JIRAKEY", null, "true", "true");

        assertEquals("JIRAKEY", definition.getJiraProjectKey());
        assertEquals("true", definition.getJiraShowReleased());
        assertEquals("true", definition.getJiraShowArchived());
        assertEquals("pdesc", definition.getDescription());

        CLICommand cliCommand = mock(CLICommand.class);

        ParameterValue value = definition.createValue(cliCommand, "Jira Version 1.2.3");
        assertEquals(definition.getName(), value.getName());
        assertEquals("Jira Version 1.2.3", value.getValue());
    }
}
