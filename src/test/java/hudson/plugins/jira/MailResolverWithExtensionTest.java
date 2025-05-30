/*
 * The MIT License
 *
 * Copyright (c) 2015 schristou88
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.User;
import hudson.security.HudsonPrivateSecurityRealm;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jenkins.model.Jenkins;
import jenkins.security.SecurityListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class MailResolverWithExtensionTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    JiraSite site;

    JiraSession session;

    @Mock
    JiraRestService service;

    @BeforeEach
    void createMocks() throws Exception {
        session = new JiraSession(site, service);
        Mockito.when(site.getSession(any())).thenReturn(session);

        Map<String, URI> avatars = new HashMap<>();
        // pre check condition in Jira User constructor and do not ask me why!!
        avatars.put("48x48", new URI("https://foo.com"));
        com.atlassian.jira.rest.client.api.domain.User jiraUser = new com.atlassian.jira.rest.client.api.domain.User(
                null, "foo", "bar", "foo@beer.com", true, null, avatars, null);

        doReturn(session).when(site).getSession(any());
        doReturn(jiraUser).when(service).getUser("foo");
    }

    @Test
    void emailResolverWithSecurityExtension(JenkinsRule r) throws Exception {
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(true);
        realm.createAccount("foo", "pacific_ale");

        r.jenkins.setSecurityRealm(realm);

        JiraGlobalConfiguration.get().setSites(Collections.singletonList(site));

        r.createWebClient().login("foo", "pacific_ale");
    }

    @TestExtension
    public static class DummySecurityListener extends SecurityListener {
        @Override
        protected void authenticated2(@NonNull UserDetails details) {
            check(details.getUsername());
        }

        @Override
        protected void authenticated(@NonNull org.acegisecurity.userdetails.UserDetails details) {
            check(details.getUsername());
        }

        private void check(String userId) {

            User user = User.getById(userId, false);

            JiraMailAddressResolver jiraMailAddressResolver = Jenkins.get()
                    .getExtensionList(JiraMailAddressResolver.class)
                    .get(0);
            String email = jiraMailAddressResolver.findMailAddressFor(user);
            assertThat(email, is("foo@beer.com"));
        }
    }
}
