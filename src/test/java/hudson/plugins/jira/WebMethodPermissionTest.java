package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.net.URL;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * The descriptors' {@code doCheck*} / {@code doFill*} methods are routed independently of the
 * configuration form their field appears on, so each answers according to what the caller is
 * entitled to see. Every case below asserts that twice: what a caller who may configure the item
 * gets, and that a caller who may only read it gets an empty answer instead.
 */
@WithJenkins
class WebMethodPermissionTest {

    private static final String ADMIN = "admin";
    private static final String CONFIGURER = "dev";
    private static final String READER = "alice";

    private FreeStyleProject givenAProjectOnlyDevCanConfigure(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject();
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        strategy.grant(Jenkins.ADMINISTER).everywhere().to(ADMIN);
        strategy.grant(Jenkins.READ).everywhere().to(CONFIGURER, READER);
        strategy.grant(Item.CONFIGURE).onItems(project).to(CONFIGURER);
        strategy.grant(Item.READ).onItems(project).to(READER);
        r.jenkins.setAuthorizationStrategy(strategy);
        return project;
    }

    private ACLContext as(String username) {
        return ACL.as(User.getById(username, true));
    }

    @Test
    void jiraSiteUrlValidationIsWithheldFromNonConfigurers(JenkinsRule r) throws Exception {
        FreeStyleProject project = givenAProjectOnlyDevCanConfigure(r);
        JiraSite.DescriptorImpl descriptor = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class);

        try (ACLContext ignored = as(CONFIGURER)) {
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl(project, "not a url").kind);
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckAlternativeUrl(project, "not a url").kind);
        }

        try (ACLContext ignored = as(READER)) {
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrl(project, "not a url").kind);
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckAlternativeUrl(project, "not a url").kind);
        }
    }

    @Test
    void jiraSiteUrlValidationOutsideAnItemRequiresAdminister(JenkinsRule r) throws Exception {
        givenAProjectOnlyDevCanConfigure(r);
        JiraSite.DescriptorImpl descriptor = r.jenkins.getDescriptorByType(JiraSite.DescriptorImpl.class);

        // No item in the request path means global configuration, which only an administrator sees.
        try (ACLContext ignored = as(CONFIGURER)) {
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrl(null, "not a url").kind);
        }

        try (ACLContext ignored = as(ADMIN)) {
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl(null, "not a url").kind);
        }
    }

    @Test
    void workflowBuilderValidationIsWithheldFromNonConfigurers(JenkinsRule r) throws Exception {
        FreeStyleProject project = givenAProjectOnlyDevCanConfigure(r);
        JiraIssueUpdateBuilder.DescriptorImpl descriptor =
                r.jenkins.getDescriptorByType(JiraIssueUpdateBuilder.DescriptorImpl.class);

        try (ACLContext ignored = as(CONFIGURER)) {
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckJqlSearch(project, "").kind);
            assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckWorkflowActionName(project, "").kind);
        }

        try (ACLContext ignored = as(READER)) {
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckJqlSearch(project, "").kind);
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckWorkflowActionName(project, "").kind);
        }
    }

    @Test
    void projectKeyValidationIsWithheldFromNonConfigurers(JenkinsRule r) throws Exception {
        FreeStyleProject project = givenAProjectOnlyDevCanConfigure(r);

        try (ACLContext ignored = as(CONFIGURER)) {
            assertEquals(
                    FormValidation.Kind.ERROR, JiraCreateIssueNotifier.DESCRIPTOR.doCheckProjectKey(project, "").kind);
        }

        try (ACLContext ignored = as(READER)) {
            assertEquals(
                    FormValidation.Kind.OK, JiraCreateIssueNotifier.DESCRIPTOR.doCheckProjectKey(project, "").kind);
        }
    }

    @Test
    void siteNamesAreWithheldFromNonConfigurers(JenkinsRule r) throws Exception {
        FreeStyleProject project = givenAProjectOnlyDevCanConfigure(r);
        JiraSite site = mock(JiraSite.class);
        when(site.getName()).thenReturn("https://issues.example.org/");
        JiraGlobalConfiguration.get().setSites(Collections.singletonList(site));
        JiraProjectProperty.DescriptorImpl descriptor =
                r.jenkins.getDescriptorByType(JiraProjectProperty.DescriptorImpl.class);

        try (ACLContext ignored = as(CONFIGURER)) {
            ListBoxModel options = descriptor.doFillSiteNameItems(project, null);
            assertThat(options, hasSize(1));
            assertEquals("https://issues.example.org/", options.get(0).name);
        }

        try (ACLContext ignored = as(READER)) {
            assertThat(descriptor.doFillSiteNameItems(project, null), empty());
        }
    }

    @Test
    void jiraMetadataIsNotFetchedForNonConfigurers(JenkinsRule r) throws Exception {
        FreeStyleProject project = givenAProjectOnlyDevCanConfigure(r);
        JiraSite site = mock(JiraSite.class);
        when(site.getUrl()).thenReturn(new URL("https://issues.example.org/"));
        JiraGlobalConfiguration.get().setSites(Collections.singletonList(site));

        try (ACLContext ignored = as(READER)) {
            // Only the empty "no selection" option, and no session opened against Jira: these two
            // build their list by calling the configured site, which is work that should not happen
            // for a caller who could not have opened the form in the first place.
            assertThat(JiraCreateIssueNotifier.DESCRIPTOR.doFillPriorityIdItems(project), hasSize(1));
            assertThat(JiraCreateIssueNotifier.DESCRIPTOR.doFillTypeIdItems(project), hasSize(1));
        }

        verify(site, never()).getSession(any());
    }
}
