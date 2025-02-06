package hudson.plugins.jira;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Item;
import hudson.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import jenkins.model.Jenkins;
import jenkins.security.ApiTokenProperty;
import net.sf.json.JSONObject;
import org.eclipse.jetty.ee9.servlet.DefaultServlet;
import org.eclipse.jetty.ee9.servlet.ServletContextHandler;
import org.eclipse.jetty.ee9.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.util.NameValuePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class JiraSiteSecurity1029Test {

    private Server server;
    private URI serverUri;
    private FakeJiraServlet servlet;

    @Test
    @Issue("SECURITY-1029")
    void cannotLeakCredentials(JenkinsRule j) throws Exception {
        setupServer(j);

        final String ADMIN = "admin";
        final String USER = "user";
        final String USER_FOLDER_CONFIGURE = "folder_configure";

        j.jenkins.setCrumbIssuer(null);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to(ADMIN)
                .grant(Jenkins.READ, Item.READ)
                .everywhere()
                .to(USER)
                .grant(Jenkins.READ, Item.READ, Item.CONFIGURE)
                .everywhere()
                .to(USER_FOLDER_CONFIGURE));

        String credId_1 = "cred-1-id";
        String credId_2 = "cred-2-id";
        String credId_3 = "cred-3-id";

        String pwd1 = "pwd1";
        String pwd2 = "pwd2";
        String pwd3 = "pwd3";

        UsernamePasswordCredentialsImpl cred1 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_1, null, "user1", pwd1);
        UsernamePasswordCredentialsImpl cred2 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_2, null, "user2", pwd2);
        UsernamePasswordCredentialsImpl cred3 =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId_3, null, "user3", pwd3);

        SystemCredentialsProvider systemProvider = SystemCredentialsProvider.getInstance();
        systemProvider.getCredentials().add(cred1);
        systemProvider.getCredentials().add(cred2);
        systemProvider.save();

        User admin = User.getById(ADMIN, true);
        admin.addProperty(new ApiTokenProperty());
        admin.getProperty(ApiTokenProperty.class).changeApiToken();
        User user = User.getById(USER, true);
        user.addProperty(new ApiTokenProperty());
        user.getProperty(ApiTokenProperty.class).changeApiToken();

        User userFolderConfigure = User.getById(USER_FOLDER_CONFIGURE, true);
        userFolderConfigure.addProperty(new ApiTokenProperty());
        userFolderConfigure.getProperty(ApiTokenProperty.class).changeApiToken();

        { // as an admin I should be able to validate my url / credentials
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc = wc.withBasicApiToken(admin);

            String jiraSiteValidateUrl = j.getURL() + "descriptorByName/" + JiraSite.class.getName() + "/validate";
            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                    new NameValuePair("threadExecutorNumber", "1"),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", credId_1),
                    new NameValuePair("useHTTPAuth", "true")));

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
                    new NameValuePair("threadExecutorNumber", "1"),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", credId_2),
                    new NameValuePair("useHTTPAuth", "true")));

            Page page = wc.getPage(request);
            // to avoid trouble, we always validate when the user has not the good permission
            assertThat(page.getWebResponse().getStatusCode(), equalTo(403));
            assertThat(servlet.getPasswordAndReset(), nullValue());
        }

        { // as an user with just read access, I may not be able to leak any credentials in folder
            Folder folder = j.jenkins.createProject(
                    Folder.class, "folder" + j.jenkins.getItems().size());

            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(user);

            String jiraSiteValidateUrl = j.jenkins.getRootUrl() + folder.getUrl() + "descriptorByName/"
                    + JiraSite.class.getName() + "/validate";

            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                    new NameValuePair("threadExecutorNumber", "1"),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", credId_2),
                    new NameValuePair("useHTTPAuth", "true")));

            Page page = wc.getPage(request);
            // to avoid trouble, we always validate when the user has not the good permission
            assertThat(page.getWebResponse().getStatusCode(), equalTo(403));
            assertThat(servlet.getPasswordAndReset(), nullValue());
        }

        { // as an user with just read access, I may not be able to leak any credentials with fake id in a folder
            Folder folder = j.jenkins.createProject(
                    Folder.class, "folder" + j.jenkins.getItems().size());

            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(userFolderConfigure);

            String jiraSiteValidateUrl = j.jenkins.getRootUrl() + folder.getUrl() + "descriptorByName/"
                    + JiraSite.class.getName() + "/validate";
            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                    new NameValuePair("threadExecutorNumber", "1"),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", "aussie-beer-is-the-best"), // use a non existing id on purpose
                    new NameValuePair("useHTTPAuth", "true")));

            Page page = wc.getPage(request);
            WebResponse webResponse = page.getWebResponse();
            assertThat(webResponse.getStatusCode(), equalTo(200));
            assertThat(webResponse.getContentAsString(), containsString("Cannot validate configuration"));
            assertThat(servlet.getPasswordAndReset(), nullValue());
        }

        { // as an user with configure access, I can access
            Folder folder = j.jenkins.createProject(
                    Folder.class, "folder" + j.jenkins.getItems().size());

            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(userFolderConfigure);

            String jiraSiteValidateUrl = j.jenkins.getRootUrl() + folder.getUrl() + "descriptorByName/"
                    + JiraSite.class.getName() + "/validate";

            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                    new NameValuePair("threadExecutorNumber", "1"),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", credId_2),
                    new NameValuePair("useHTTPAuth", "true")));

            Page page = wc.getPage(request);
            // to avoid trouble, we always validate when the user has the good permission
            assertThat(page.getWebResponse().getStatusCode(), equalTo(200));
            assertThat(servlet.getPasswordAndReset(), equalTo(pwd2));
        }

        { // as an user with folder access, I can access
            Folder folder = j.jenkins.createProject(
                    Folder.class, "folder" + j.jenkins.getItems().size());

            CredentialsStore folderStore = JiraFolderPropertyTest.getFolderStore(folder);
            folderStore.addCredentials(Domain.global(), cred3);

            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.withBasicApiToken(userFolderConfigure);

            String jiraSiteValidateUrl = j.jenkins.getRootUrl() + folder.getUrl() + "descriptorByName/"
                    + JiraSite.class.getName() + "/validate";

            WebRequest request = new WebRequest(new URL(jiraSiteValidateUrl), HttpMethod.POST);
            request.setRequestParameters(Arrays.asList(
                    new NameValuePair("threadExecutorNumber", "1"),
                    new NameValuePair("url", serverUri.toString()),
                    new NameValuePair("credentialsId", credId_3),
                    new NameValuePair("useHTTPAuth", "true")));

            Page page = wc.getPage(request);
            // to avoid trouble, we always validate when the user has the good permission
            assertThat(page.getWebResponse().getStatusCode(), equalTo(200));
            assertThat(servlet.getPasswordAndReset(), equalTo(pwd3));
        }
    }

    public void setupServer(JenkinsRule j) throws Exception {
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

    @AfterEach
    void stopEmbeddedJettyServer() {
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
            Object body = new HashMap<String, Object>() {
                {
                    put("permissions", new HashMap<String, Object>() {
                        {
                            put("perm_1", new HashMap<String, Object>() {
                                {
                                    put("id", 1);
                                    put("key", "perm_key");
                                    put("name", "perm_name");
                                    put("description", null);
                                    put("havePermission", "true");
                                }
                            });
                        }
                    });
                }
            };

            resp.getWriter().write(JSONObject.fromObject(body).toString());
        }
    }
}
