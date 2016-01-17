package hudson.plugins.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.util.FormValidation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by warden on 14.09.15.
 */
public class DescriptorImplTest {

//    @Rule
//    JenkinsRule rule = new JenkinsRule();

    JiraSite.DescriptorImpl descriptor = new JiraSite.DescriptorImpl();

    JiraSite jiraSite = mock(JiraSite.class);

    JiraSession jiraSession = mock(JiraSession.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testDoValidate() throws Exception {
        FormValidation validation = descriptor.doValidate(null, null, null, null, null, false, null, JiraSite.DEFAULT_TIMEOUT);
        assertEquals(validation.kind, FormValidation.Kind.ERROR);

        validation = descriptor.doValidate("user", "invalid", "pass", null, null, false, null, JiraSite.DEFAULT_TIMEOUT);
        assertEquals(validation.kind, FormValidation.Kind.ERROR);

        validation = descriptor.doValidate("user", "http://valid/", "pass", null, null, false, "invalid", JiraSite.DEFAULT_TIMEOUT);
        assertEquals(validation.kind, FormValidation.Kind.ERROR);
    }

    @Test
    public void testValidateConnectionError() throws Exception {
        when(jiraSession.getMyPermissions()).thenThrow(RestClientException.class);
        when(jiraSite.createSession()).thenReturn(jiraSession);
        FormValidation validation = descriptor.doValidate("user", "http://localhost:8080", "pass", null, null, false, " ", JiraSite.DEFAULT_TIMEOUT);
        assertEquals(validation.kind, FormValidation.Kind.ERROR);
    }

}