package hudson.plugins.jira.versionparameter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import hudson.cli.CLICommand;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;

public class JiraVersionParameterDefinitionTest {
    @Test
    public void testParameterValueMethodOverrides() throws Exception {
        ParameterDefinition definition = new JiraVersionParameterDefinition("pname", "pdesc", "JIRAKEY", null, "false", "false");
        CLICommand cliCommand = mock(CLICommand.class);

        ParameterValue value = definition.createValue(cliCommand, "JIRA Version 1.2.3");
        assertEquals("pname", value.getName());
        assertEquals("JIRA Version 1.2.3", value.getValue());
    }
}
