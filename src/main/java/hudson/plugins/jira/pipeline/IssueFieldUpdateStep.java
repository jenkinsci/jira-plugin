package hudson.plugins.jira.pipeline;

import com.atlassian.jira.rest.client.api.RestClientException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.EnvironmentExpander;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.Messages;
import hudson.plugins.jira.model.JiraIssueField;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Issue field updater
 *
 * @author Dmitry Frolov tekillaz.dev@gmail.com
 *
 */
public class IssueFieldUpdateStep extends Builder implements SimpleBuildStep {

    private AbstractIssueSelector issueSelector;

    public AbstractIssueSelector getIssueSelector() {
        return this.issueSelector;
    }

    @DataBoundSetter
    public void setIssueSelector(AbstractIssueSelector issueSelector) {
        this.issueSelector = issueSelector;
    }

    public String fieldId;

    public String getFieldId() {
        return this.fieldId;
    }

    @DataBoundSetter
    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String fieldValue;

    public String getFieldValue() {
        return this.fieldValue;
    }

    @DataBoundSetter
    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    @DataBoundConstructor
    public IssueFieldUpdateStep(AbstractIssueSelector issueSelector, String fieldId, String fieldValue) {
        this.issueSelector = issueSelector;
        this.fieldId = fieldId;
        this.fieldValue = fieldValue;
    }

    /**
     * Jira field ids that take a JSON array of plain strings rather than a scalar.
     *
     * <p>Deliberately just {@code labels}: the other multi-valued built-ins ({@code components},
     * {@code fixVersions}, {@code versions}) need arrays of objects, which is the structured-value
     * work tracked separately.
     */
    private static final Set<String> MULTI_VALUE_BUILT_IN_FIELDS = Set.of("labels");

    /**
     * Turns what the user typed into a Jira field id.
     *
     * <p>A bare number is the long-standing shorthand for a custom field, so it keeps getting the
     * {@code customfield_} prefix. Everything else is passed through: this used to prefix
     * unconditionally, which meant {@code labels} was sent as {@code customfield_labels} and every
     * built-in field was unreachable from the step.
     */
    public String prepareFieldId(String fieldId) {
        if (fieldId == null) {
            return null;
        }
        String prepared = fieldId.trim();
        if (prepared.matches("\\d+")) {
            prepared = "customfield_" + prepared;
        }
        return prepared;
    }

    /**
     * Turns what the user typed into a value Jira will accept for {@code fieldId}.
     */
    private Object prepareFieldValue(String fieldId, String expandedValue) {
        if (!MULTI_VALUE_BUILT_IN_FIELDS.contains(fieldId)) {
            return expandedValue;
        }
        return Arrays.stream(StringUtils.defaultString(expandedValue).split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public void perform(Run<?, ?> run, EnvVars env, TaskListener listener) throws IOException {

        PrintStream logger = listener.getLogger();

        AbstractIssueSelector selector = issueSelector;
        if (selector == null) {
            logger.println("[Jira][IssueFieldUpdateStep] No issue selector found!");
            throw new IOException("[Jira][IssueFieldUpdateStep] No issue selector found!");
        }

        JiraSite site = JiraSite.get(run.getParent());
        if (site == null) {
            logger.println(Messages.NoJiraSite());
            run.setResult(Result.FAILURE);
            return;
        }

        JiraSession session = site.getSession(run.getParent());
        if (session == null) {
            logger.println(Messages.NoRemoteAccess());
            run.setResult(Result.FAILURE);
            return;
        }

        Set<String> issues;
        try {
            issues = selector.findIssueIds(run, site, listener);
            if (issues.isEmpty()) {
                logger.println("[Jira][IssueFieldUpdateStep] Issue list is empty!");
                return;
            }
        } catch (RestClientException e) {
            logger.println(e.getMessage());
            return;
        }

        String preparedFieldId = prepareFieldId(getFieldId());
        List<JiraIssueField> fields = Collections.singletonList(new JiraIssueField(
                preparedFieldId,
                prepareFieldValue(preparedFieldId, EnvironmentExpander.expandVariable(getFieldValue(), env))));

        try {
            for (String issue : issues) {
                submitFields(session, issue, fields, logger);
            }
        } catch (RestClientException e) {
            logger.println(e.getMessage());
        }
    }

    @Override
    public boolean requiresWorkspace() {
        return false;
    }

    /**
     * @deprecated no reason for this to be exposed/public, use perform(...) instead
     */
    @Deprecated
    public void submitFields(JiraSession session, String issueId, List<JiraIssueField> fields, PrintStream logger) {
        try {
            session.addFields(issueId, fields);
        } catch (RestClientException e) {

            if (e.getStatusCode().or(0).equals(404)) {
                logger.println("[Jira] " + issueId + " - Jira issue not found");
            }

            if (e.getStatusCode().or(0).equals(403)) {
                logger.println("[Jira] " + issueId
                        + " - Jenkins Jira user does not have permissions to comment on this issue");
            }

            if (e.getStatusCode().or(0).equals(401)) {
                logger.println("[Jira] " + issueId + " - Jenkins Jira authentication problem");
            }

            logger.println(Messages.FailedToUpdateIssue(issueId));
            logger.println(e.getLocalizedMessage());
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("jiraUpdateIssueField")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        // Named for the field it validates: the config form binds field="fieldId", so Stapler looks for
        // doCheckFieldId and the old doCheckField_id was never called. Its digits-only rule would have
        // rejected every built-in field name had it been, so both are fixed together.
        public FormValidation doCheckFieldId(@QueryParameter String value) {
            String fieldId = Util.fixNull(value).trim();
            if (fieldId.isEmpty()) {
                return FormValidation.warning(Messages.JiraIssueFieldUpdater_NoIssueFieldID());
            }
            if (!fieldId.matches("\\w+")) {
                return FormValidation.error(Messages.JiraIssueFieldUpdater_NotAtIssueFieldID());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraIssueFieldUpdater_DisplayName();
        }
    }
}
