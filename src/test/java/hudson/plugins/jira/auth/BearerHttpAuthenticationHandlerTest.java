package hudson.plugins.jira.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.atlassian.httpclient.api.Request;
import org.junit.jupiter.api.Test;

class BearerHttpAuthenticationHandlerTest {

    @Test
    void testConfigure() {
        String token = "token";
        BearerHttpAuthenticationHandler handler = new BearerHttpAuthenticationHandler(token);
        Request.Builder builder = mock(Request.Builder.class);

        handler.configure(builder);

        verify(builder).setHeader("Authorization", "Bearer " + token);
    }
}
