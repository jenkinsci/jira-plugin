package com.atlassian.httpclient.apache.httpcomponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.atlassian.httpclient.api.Response;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.ProxyConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class ApacheAsyncHttpClientTest {

    private final ConnectionFactory connectionFactory = new HttpConnectionFactory();

    private Server server;

    private ServerConnector connector;

    static final String CONTENT_RESPONSE = "Sounds Good";

    public void prepare(Handler handler) throws Exception {
        server = new Server();
        connector = new ServerConnector(server, connectionFactory);
        server.addConnector(connector);
        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    void dispose() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void simple_get(JenkinsRule r) throws Exception {
        TestHandler testHandler = new TestHandler();
        prepare(testHandler);

        ApacheAsyncHttpClient httpClient = new ApacheAsyncHttpClient(
                null, buildApplicationProperties(), new NoOpThreadLocalContextManager(), new HttpClientOptions());

        Response response = httpClient
                .newRequest("http://localhost:" + connector.getLocalPort() + "/foo")
                .get()
                .get(10, TimeUnit.SECONDS);
        assertEquals(200, response.getStatusCode());
        assertEquals(CONTENT_RESPONSE, IOUtils.toString(response.getEntityStream()));
    }

    @Test
    void simple_post(JenkinsRule r) throws Exception {
        TestHandler testHandler = new TestHandler();
        prepare(testHandler);

        ApacheAsyncHttpClient httpClient = new ApacheAsyncHttpClient(
                null, buildApplicationProperties(), new NoOpThreadLocalContextManager(), new HttpClientOptions());

        Response response = httpClient
                .newRequest("http://localhost:" + connector.getLocalPort() + "/foo")
                .setEntity("FOO")
                .setContentType("text")
                .post()
                .get(10, TimeUnit.SECONDS);
        assertEquals(200, response.getStatusCode());
        assertEquals(CONTENT_RESPONSE, IOUtils.toString(response.getEntityStream()));
        assertEquals("FOO", testHandler.postReceived);
    }

    @Test
    void simple_get_with_non_proxy_host(JenkinsRule r) throws Exception {
        ProxyTestHandler testHandler = new ProxyTestHandler();
        prepare(testHandler);

        Jenkins.get().proxy =
                new ProxyConfiguration("localhost", connector.getLocalPort(), "foo", "bar", "www.apache.org");

        ApacheAsyncHttpClient httpClient = new ApacheAsyncHttpClient(
                null, buildApplicationProperties(), new NoOpThreadLocalContextManager(), new HttpClientOptions());

        Response response = httpClient.newRequest("http://www.apache.org").get().get(30, TimeUnit.SECONDS);
        assertEquals(200, response.getStatusCode());
        // assertEquals(CONTENT_RESPONSE, IOUtils.toString(response.getEntityStream()));
    }

    @Test
    void simple_get_with_proxy(JenkinsRule r) throws Exception {
        ProxyTestHandler testHandler = new ProxyTestHandler();
        prepare(testHandler);

        Jenkins.get().proxy = new ProxyConfiguration("localhost", connector.getLocalPort(), "foo", "bar");

        ApacheAsyncHttpClient httpClient = new ApacheAsyncHttpClient(
                null, buildApplicationProperties(), new NoOpThreadLocalContextManager(), new HttpClientOptions());

        Response response = httpClient.newRequest("http://jenkins.io").get().get(30, TimeUnit.SECONDS);
        assertEquals(200, response.getStatusCode());
        assertEquals(CONTENT_RESPONSE, IOUtils.toString(response.getEntityStream()));
    }

    @Test
    void simple_post_with_proxy(JenkinsRule r) throws Exception {
        ProxyTestHandler testHandler = new ProxyTestHandler();
        prepare(testHandler);

        Jenkins.get().proxy = new ProxyConfiguration("localhost", connector.getLocalPort(), "foo", "bar");

        ApacheAsyncHttpClient httpClient = new ApacheAsyncHttpClient(
                null, buildApplicationProperties(), new NoOpThreadLocalContextManager(), new HttpClientOptions());

        Response response = httpClient
                .newRequest("http://jenkins.io")
                .setEntity("FOO")
                .setContentType("text")
                .post()
                .get(30, TimeUnit.SECONDS);
        // we are sure to hit the proxy first :-)
        assertEquals(200, response.getStatusCode());
        assertEquals(CONTENT_RESPONSE, IOUtils.toString(response.getEntityStream()));
        assertEquals("FOO", testHandler.postReceived);
    }

    public static class ProxyTestHandler extends Handler.Abstract {

        String postReceived;

        final String user = "foo";

        final String password = "bar";

        final String serverHost = "server";

        final String realm = "test_realm";

        @Override
        public boolean handle(
                org.eclipse.jetty.server.Request request, org.eclipse.jetty.server.Response response, Callback callback)
                throws IOException {

            final String credentials = Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));

            String authorization = request.getHeaders().get(HttpHeader.PROXY_AUTHORIZATION.asString());
            if (authorization == null) {
                response.setStatus(HttpStatus.PROXY_AUTHENTICATION_REQUIRED_407);
                response.getHeaders().add(HttpHeader.PROXY_AUTHENTICATE.asString(), "Basic realm=\"" + realm + "\"");
                callback.succeeded();
                return true;
            } else {
                String prefix = "Basic ";
                if (authorization.startsWith(prefix)) {
                    String attempt = authorization.substring(prefix.length());
                    if (!credentials.equals(attempt)) {
                        callback.succeeded();
                        return true;
                    }
                }
            }

            if (StringUtils.equalsIgnoreCase("post", request.getMethod())) {
                postReceived = Content.Source.asString(request, StandardCharsets.UTF_8);
            }
            Content.Sink.write(response, true, CONTENT_RESPONSE, callback);
            return true;
        }
    }

    public static class TestHandler extends Handler.Abstract {

        String postReceived;

        @Override
        public boolean handle(
                org.eclipse.jetty.server.Request request, org.eclipse.jetty.server.Response response, Callback callback)
                throws IOException {
            if (StringUtils.equalsIgnoreCase("post", request.getMethod())) {
                postReceived = Content.Source.asString(request, StandardCharsets.UTF_8);
            }
            Content.Sink.write(response, true, CONTENT_RESPONSE, callback);
            return true;
        }
    }

    private ApplicationProperties buildApplicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties() {
            @Override
            public String getBaseUrl() {
                return null;
            }

            @NonNull
            @Override
            public String getBaseUrl(UrlMode urlMode) {
                return null;
            }

            @NonNull
            @Override
            public String getDisplayName() {
                return "Foo";
            }

            @NonNull
            @Override
            public String getPlatformId() {
                return null;
            }

            @NonNull
            @Override
            public String getVersion() {
                return "1";
            }

            @NonNull
            @Override
            public Date getBuildDate() {
                return null;
            }

            @NonNull
            @Override
            public String getBuildNumber() {
                return "1";
            }

            @Nullable
            @Override
            public File getHomeDirectory() {
                return null;
            }

            @Override
            public String getPropertyValue(String s) {
                return null;
            }

            @NonNull
            @Override
            public String getApplicationFileEncoding() {
                return System.getProperty("file.encoding");
            }

            @NonNull
            @Override
            public Optional<Path> getLocalHomeDirectory() {
                return Optional.empty();
            }

            @NonNull
            @Override
            public Optional<Path> getSharedHomeDirectory() {
                return Optional.empty();
            }
        };
        return applicationProperties;
    }

    private static final class NoOpThreadLocalContextManager<C> implements ThreadLocalContextManager<C> {
        @Override
        public C getThreadLocalContext() {
            return null;
        }

        @Override
        public void setThreadLocalContext(C context) {}

        @Override
        public void clearThreadLocalContext() {}
    }
}
