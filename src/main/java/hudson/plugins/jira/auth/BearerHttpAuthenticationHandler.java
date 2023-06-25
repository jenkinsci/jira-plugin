package hudson.plugins.jira.auth;

import com.atlassian.httpclient.api.Request.Builder;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;

/**
 * Authentication handler for bearer authentication
 *
 * @author Elia Bracci
 */
public class BearerHttpAuthenticationHandler implements AuthenticationHandler {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final String token;

    /**
     * Bearer http authentication handler constructor
     * @param token pat or api token to use for bearer authentication
     */
    public BearerHttpAuthenticationHandler(final String token) {
        this.token = token;
    }

    @Override
    public void configure(Builder builder) {
        builder.setHeader(AUTHORIZATION_HEADER, "Bearer " + token);
    }
}
