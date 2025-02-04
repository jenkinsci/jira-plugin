package hudson.plugins.jira.versionparameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import hudson.cli.CLICommand;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import org.junit.jupiter.api.Test;

class JiraVersionParameterDefinitionTest {
    @Test
    void parameterValueMethodOverrides() throws Exception {
        ParameterDefinition definition =
                new JiraVersionParameterDefinition("pname", "pdesc", "JIRAKEY", null, "false", "false");
        CLICommand cliCommand = mock(CLICommand.class);

        ParameterValue value = definition.createValue(cliCommand, "Jira Version 1.2.3");
        assertEquals("pname", value.getName());
        assertEquals("Jira Version 1.2.3", value.getValue());
    }
}
