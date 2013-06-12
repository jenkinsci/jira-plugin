package hudson.plugins.jira;

import hudson.util.FormValidation;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class JiraSiteTest {

    @Test
    public void doValidateReturnsErrorWhenAlternativeUrlIsEmpty() throws IOException {
        final String url = "https://issues.jenkins-ci.org";
        final String userName = "username";
        final String password = "password";
        final String groupVisibility = "";
        final String roleVisibility = "";
        final boolean useHTTPAuth = false;
        
        final String alternativeUrl = "";
        
        JiraSite.DescriptorImpl descriptor = new JiraSite.DescriptorImpl();
        FormValidation validation =
                descriptor.doValidate(userName, url, password, groupVisibility, roleVisibility, useHTTPAuth, alternativeUrl);

        assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }
}
