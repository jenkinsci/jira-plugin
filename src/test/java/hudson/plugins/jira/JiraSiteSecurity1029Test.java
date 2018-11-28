package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.User;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class JiraSiteSecurity1029Test {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    {j.timeout = 0;}
    
    private Server server;
    private URI serverUri;
    private FakeJiraServlet servlet;
    
    @Test
    public void cannotLeakCredentials() throws Exception {
        setupServer();
        
        final String ADMIN = "admin";
        final String USER = "user";
        final String USER_FOLDER_CONFIGURE = "folder_configure";
        
        j.jenkins.setCrumbIssuer(null);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER).everywhere().to(ADMIN)
                .grant(Jenkins.READ, Item.READ).everywhere().to(USER)
                .grant(Jenkins.READ, Item.READ, Item.CONFIGURE).everywhere().to(USER_FOLDER_CONFIGURE)
        );
        
        String credId_1 = "cred-1-id";
        String credId_2 = "cred-2-id";
        
        String pwd1 = "pwd1";
        String pwd2 = "pwd2";
        
        UsernamePasswordCredentialsImpl cred1 = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_1, null, "user1", pwd1);
        UsernamePasswordCredentialsImpl cred2 = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_2, null, "user2", pwd2);
        
        SystemCredentialsProvider systemProvider = SystemCredentialsProvider.getInstance();
        systemProvider.getCredentials().add(cred1);
        systemProvider.getCredentials().add(cred2);
        systemProvider.save();
        
        User admin = User.getById(ADMIN, true);
        User user = User.getById(USER, true);
        User userFolderConfigure = User.getById(USER_FOLDER_CONFIGURE, true);
        
        { // as an admin I should be able to validate my url / credentials
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(admin);

            String jiraSiteValidateUrl = j.getURL() + "descriptorByName/" + JiraSite.class.getName() + "/validate";
            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                    new NameValuePair( "threadExecutorNumber", "1" ),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", credId_1),
                    new NameValuePair("useHTTPAuth", "true")
            ));

            Page page = wc.getPage(request);
            assertThat(page.getWebResponse().getStatusCode(), equalTo(200));
            assertThat(servlet.getPasswordAndReset(), equalTo(pwd1));
        }
        { // as an user with just read access, I may not be able to leak any credentials
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(user);

            String jiraSiteValidateUrl = j.getURL() + "descriptorByName/" + JiraSite.class.getName() + "/validate";
            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                    new NameValuePair( "threadExecutorNumber", "1" ),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", credId_2),
                    new NameValuePair("useHTTPAuth", "true")
            ));

            Page page = wc.getPage(request);
            // to avoid trouble, we always validate when the user has not the good permission
            assertThat(page.getWebResponse().getStatusCode(), equalTo(403));
            assertThat(servlet.getPasswordAndReset(), nullValue());

        }

        { // as an user with just read access, I may not be able to leak any credentials in folder
            Folder folder = j.jenkins.createProject(Folder.class, "folder" + j.jenkins.getItems().size());

            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(user);

            String jiraSiteValidateUrl = j.jenkins.getRootUrl() + folder.getUrl()
                + "descriptorByName/" + JiraSite.class.getName() + "/validate";

            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                new NameValuePair( "threadExecutorNumber", "1" ),
                new NameValuePair("url", serverUri.toString()),
                new NameValuePair("credentialsId", credId_2),
                new NameValuePair("useHTTPAuth", "true")
            ));

            Page page = wc.getPage(request);
            // to avoid trouble, we always validate when the user has not the good permission
            assertThat(page.getWebResponse().getStatusCode(), equalTo(403));
            assertThat(servlet.getPasswordAndReset(), nullValue());
        }

        { // as an user with configure access, I can access
            Folder folder = j.jenkins.createProject(Folder.class, "folder" + j.jenkins.getItems().size());

            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(userFolderConfigure);

            String jiraSiteValidateUrl = j.jenkins.getRootUrl() + folder.getUrl()
                + "descriptorByName/" + JiraSite.class.getName() + "/validate";

            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                new NameValuePair( "threadExecutorNumber", "1" ),
                new NameValuePair("url", serverUri.toString()),
                new NameValuePair("credentialsId", credId_2),
                new NameValuePair("useHTTPAuth", "true")
            ));

            Page page = wc.getPage(request);
            // to avoid trouble, we always validate when the user has the good permission
            assertThat(page.getWebResponse().getStatusCode(), equalTo(200));
            assertThat(servlet.getPasswordAndReset(), equalTo(pwd2));
        }
    }
    
    public void setupServer() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        // auto-bind to available port
        connector.setPort(0);
        server.addConnector(connector);
        
        servlet = new FakeJiraServlet(j);
        
        ServletContextHandler context = new ServletContextHandler();
        ServletHolder servletHolder = new ServletHolder("default", servlet);
        context.addServlet(servletHolder, "/*");
        server.setHandler(context);
        
        server.start();
        
        String host = connector.getHost();
        if (host == null) {
            host = "localhost";
        }
        
        int port = connector.getLocalPort();
        serverUri = new URI(String.format("http://%s:%d/", host, port));
        servlet.setServerUrl(serverUri);
    }
    
    @After
    public void stopEmbeddedJettyServer() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static class FakeJiraServlet extends DefaultServlet {
        
        private JenkinsRule jenkinsRule;
        private URI serverUri;
        
        private String pwdCollected;
        
        public FakeJiraServlet(JenkinsRule jenkinsRule) {
            this.jenkinsRule = jenkinsRule;
        }
        
        public void setServerUrl(URI serverUri) {
            this.serverUri = serverUri;
        }
        
        public String getPasswordAndReset() {
            String result = pwdCollected;
            this.pwdCollected = null;
            return result;
        }
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String path = req.getRequestURL().toString();
            String relativePath = path.substring(this.serverUri.toString().length());
            
            String authBasicBase64 = req.getHeader("Authorization");
            String authBase64 = authBasicBase64.substring("Basic ".length());
            String auth = new String(Base64.getDecoder().decode(authBase64), StandardCharsets.UTF_8);
            String[] authArray = auth.split(":");
            String user = authArray[0];
            String pwd = authArray[1];
            
            this.pwdCollected = pwd;
            
            switch (relativePath) {
                case "rest/api/latest/mypermissions":
                    myPermissions(req, resp);
                    break;
            }
        }
        
        private void myPermissions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Object body = new HashMap<String, Object>() {{
                put("permissions", new HashMap<String, Object>() {{
                            put("perm_1", new HashMap<String, Object>() {{
                                put("id", 1);
                                put("key", "perm_key");
                                put("name", "perm_name");
                                put("description", null);
                                put("havePermission", "true");
                            }});
                        }}
                );
            }};
            
            resp.getWriter().write(JSONObject.fromObject(body).toString());
        }
    }

}
