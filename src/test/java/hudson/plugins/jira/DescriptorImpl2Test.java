package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Permissions;
import hudson.util.FormValidation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Test for DescriptorImpl that requires powermock.
 */
// TODO Coverage with JaCoCo, see https://github.com/powermock/powermock/wiki/Code-coverage-with-JaCoCo#on-the-fly-instrumentation
@RunWith(PowerMockRunner.class)
@PrepareForTest(JiraSite.DescriptorImpl.class)
public class DescriptorImpl2Test {

    JiraSite.DescriptorImpl descriptor = new JiraSite.DescriptorImpl();

    JiraSite jiraSite = mock(JiraSite.class);

    JiraSession jiraSession = mock(JiraSession.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testValidateConnectionError() throws Exception {
        whenNew(JiraSite.class).withAnyArguments().thenReturn(jiraSite);
        when(jiraSession.getMyPermissions()).thenThrow(RestClientException.class);
        when(jiraSite.createSession()).thenReturn(jiraSession);
        FormValidation validation = descriptor.doValidate("http://localhost:8080", null, null, null, false, null, JiraSite.DEFAULT_TIMEOUT);
        verifyNew(JiraSite.class);
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }

    @Test
    public void testValidateConnectionOK() throws Exception {
        whenNew(JiraSite.class).withAnyArguments().thenReturn(jiraSite);
        when(jiraSession.getMyPermissions()).thenReturn(mock(Permissions.class));
        when(jiraSite.createSession()).thenReturn(jiraSession);
        FormValidation validation = descriptor.doValidate("http://localhost:8080", null, null, null, false, null, JiraSite.DEFAULT_TIMEOUT);
        verifyNew(JiraSite.class);
        assertEquals(FormValidation.Kind.OK, validation.kind);
    }
}
