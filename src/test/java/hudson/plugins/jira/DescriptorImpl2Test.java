package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Permissions;
import hudson.plugins.jira.JiraSite.JiraSiteBuilder;
import hudson.util.FormValidation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for DescriptorImpl.
 */
public class DescriptorImpl2Test {

    JiraSite.DescriptorImpl descriptor = spy(new JiraSite.DescriptorImpl());

    JiraSiteBuilder builder = spy(new JiraSiteBuilder());

    JiraSite jiraSite = mock(JiraSite.class);

    JiraSession jiraSession = mock(JiraSession.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void prepareMocks() {
        when(descriptor.getJiraSiteBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(jiraSite);
        when(jiraSite.createSession()).thenReturn(jiraSession);
    }

    @Test
    public void testValidateConnectionError() throws Exception {
        when(jiraSession.getMyPermissions()).thenThrow(RestClientException.class);
        FormValidation validation = descriptor.doValidate("http://localhost:8080", null, null, null, false, null, JiraSite.DEFAULT_TIMEOUT);

        verify(descriptor).getJiraSiteBuilder();
        verify(builder).build();
        verify(jiraSite).createSession();
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }

    @Test
    public void testValidateConnectionOK() throws Exception {
        when(jiraSession.getMyPermissions()).thenReturn(mock(Permissions.class));
        FormValidation validation = descriptor.doValidate("http://localhost:8080", null, null, null, false, null, JiraSite.DEFAULT_TIMEOUT);

        verify(descriptor).getJiraSiteBuilder();
        verify(builder).build();
        verify(jiraSite).createSession();
        assertEquals(FormValidation.Kind.OK, validation.kind);
    }
}
