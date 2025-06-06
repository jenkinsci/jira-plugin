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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import hudson.model.Item;
import hudson.model.User;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import jenkins.model.Jenkins;
import jenkins.security.ApiTokenProperty;
import net.sf.json.JSONObject;
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

    private HttpServer server;
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
        // auto-bind to available port
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        servlet = new FakeJiraServlet(j);

        server.createContext("/", servlet);

        server.start();

        InetSocketAddress address = server.getAddress();
        serverUri = new URI(String.format("http://%s:%d/", address.getHostString(), address.getPort()));
        servlet.setServerUrl(serverUri);
    }

    @AfterEach
    void stopEmbeddedHttpServer() {
        server.stop(1);
    }

    private static class FakeJiraServlet implements HttpHandler {

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
        public void handle(HttpExchange he) throws IOException {
            String path = he.getRequestURI().getPath();

            String authBasicBase64 = he.getRequestHeaders().getFirst("Authorization");
            String authBase64 = authBasicBase64.substring("Basic ".length());
            String auth = new String(Base64.getDecoder().decode(authBase64), StandardCharsets.UTF_8);
            String[] authArray = auth.split(":");
            String user = authArray[0];
            String pwd = authArray[1];

            this.pwdCollected = pwd;

            try {
                if ("GET".equals(he.getRequestMethod()) && "/rest/api/latest/mypermissions".equals(path)) {
                    myPermissions(he);
                } else {
                    he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                }
            } finally {
                he.close();
            }
        }

        private void myPermissions(HttpExchange he) throws IOException {
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

            String response = JSONObject.fromObject(body).toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
