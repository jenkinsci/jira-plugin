package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.*;

import hudson.ProxyConfiguration;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import jenkins.model.Jenkins;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Request;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JiraRestServiceProxyTest {

    private final ConnectionFactory connectionFactory = new HttpConnectionFactory();
    private final URI JIRA_URI = URI.create("http://example.com:8080/");
    private final String USERNAME = "user";
    private final String PASSWORD = "password";

    private Server server;
    private ServerConnector connector;
    private ExtendedJiraRestClient client;

    @BeforeEach
    void prepare() throws Exception {
        server = new Server();
        connector = new ServerConnector(server, connectionFactory);
        server.addConnector(connector);
        server.start();
    }

    @AfterEach
    void dispose() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void withProxy(JenkinsRule r) throws Exception {
        int localPort = connector.getLocalPort();
        Jenkins.get().proxy = new ProxyConfiguration("localhost", localPort);

        Object objectProxy = getProxyObjectFromRequest();

        assertNotNull(objectProxy);
        assertEquals(HttpHost.class, objectProxy.getClass());
        HttpHost proxyHost = (HttpHost) objectProxy;
        assertEquals("localhost", proxyHost.getHostName());
        assertEquals(localPort, proxyHost.getPort());
    }

    @Test
    void withProxyAndNoProxyHosts(JenkinsRule r) throws Exception {
        int localPort = connector.getLocalPort();
        Jenkins.get().proxy = new ProxyConfiguration("localhost", localPort);
        Jenkins.get().proxy.setNoProxyHost("example.com|google.com");

        assertNull(getProxyObjectFromRequest());
    }

    @Test
    void withoutProxy(JenkinsRule r) throws Exception {
        assertNull(getProxyObjectFromRequest());
    }

    private Object getProxyObjectFromRequest()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        JiraRestService service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        Method m = service.getClass().getDeclaredMethod("buildGetRequest", URI.class);
        m.setAccessible(true);

        Request buildGetRequestValue = (Request) m.invoke(service, JIRA_URI);

        assertNotNull(buildGetRequestValue);

        Field proxy = buildGetRequestValue.getClass().getDeclaredField("proxy");
        proxy.setAccessible(true);
        return proxy.get(buildGetRequestValue);
    }
}
