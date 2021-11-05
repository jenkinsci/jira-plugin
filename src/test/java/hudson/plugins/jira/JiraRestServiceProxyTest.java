package hudson.plugins.jira;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JiraRestServiceProxyTest {

  private final ConnectionFactory connectionFactory = new HttpConnectionFactory();
  private final URI JIRA_URI = URI.create("http://example.com:8080/");
  private final String USERNAME = "user";
  private final String PASSWORD = "password";
  @Rule public JenkinsRule j = new JenkinsRule();
  private Server server;
  private ServerConnector connector;
  private ExtendedJiraRestClient client;

  @Before
  public void prepare() throws Exception {
    server = new Server();
    connector = new ServerConnector(server, connectionFactory);
    server.addConnector(connector);
    server.start();
  }

  @After
  public void dispose() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void withProxy() throws Exception {
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
  public void withoutProxy() throws Exception {
    assertNull(getProxyObjectFromRequest());
  }

  private Object getProxyObjectFromRequest()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          NoSuchFieldException {
    JiraRestService service =
        new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
    Method m = service.getClass().getDeclaredMethod("buildGetRequest", URI.class);
    m.setAccessible(true);

    Request buildGetRequestValue = (Request) m.invoke(service, URI.create(""));

    assertNotNull(buildGetRequestValue);

    Field proxy = buildGetRequestValue.getClass().getDeclaredField("proxy");
    proxy.setAccessible(true);
    return proxy.get(buildGetRequestValue);
  }
}
