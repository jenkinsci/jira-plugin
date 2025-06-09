package hudson.plugins.jira.versionparameter;

import com.atlassian.jira.rest.client.api.domain.Version;
import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.plugins.jira.JiraSession;
import hudson.plugins.jira.JiraSite;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

public class JiraVersionParameterDefinition extends ParameterDefinition {
    private static final long serialVersionUID = 4232979892748310160L;

    private String projectKey;
    private boolean showReleased = false;
    private boolean showArchived = false;
    private boolean showUnreleased = false;
    private Pattern pattern = null;

    @DataBoundConstructor
    public JiraVersionParameterDefinition(
            String name,
            String description,
            String jiraProjectKey,
            String jiraReleasePattern,
            String jiraShowReleased,
            String jiraShowArchived,
            String jiraShowUnreleased) {
        super(name, description);
        setJiraProjectKey(jiraProjectKey);
        setJiraReleasePattern(jiraReleasePattern);
        setJiraShowReleased(jiraShowReleased);
        setJiraShowArchived(jiraShowArchived);
        setShowUnreleased(jiraShowUnreleased);
    }

    @Override
    public ParameterValue createValue(StaplerRequest2 req) {
        String[] values = req.getParameterValues(getName());
        if (values == null || values.length != 1) {
            return null;
        }
        return new JiraVersionParameterValue(getName(), values[0]);
    }

    @Override
    public ParameterValue createValue(StaplerRequest2 req, JSONObject formData) {
        JiraVersionParameterValue value = req.bindJSON(JiraVersionParameterValue.class, formData);
        return value;
    }

    @Override
    public ParameterValue createValue(CLICommand command, String value) throws IOException, InterruptedException {
        return new JiraVersionParameterValue(getName(), value);
    }

    public List<JiraVersionParameterDefinition.Result> getVersions() throws IOException {
        Job<?, ?> contextJob = Stapler.getCurrentRequest2().findAncestorObject(Job.class);

        JiraSite site = JiraSite.get(contextJob);
        if (site == null) {
            throw new IllegalStateException(
                    "Jira site needs to be configured in the project " + contextJob.getFullDisplayName());
        }

        JiraSession session = site.getSession(contextJob);
        if (session == null) {
            throw new IllegalStateException("Remote access for Jira isn't configured in Jenkins");
        }

        return session.getVersions(projectKey).stream()
                .sorted(VersionComparator.INSTANCE)
                .filter(this::match)
                .map(Result::new)
                .collect(Collectors.toList());
    }

    private boolean match(Version version) {
        // Match regex if it exists
        if (pattern != null) {
            if (!pattern.matcher(version.getName()).matches()) {
                return false;
            }
        }

        boolean isReleased = version.isReleased();
        boolean isArchived = version.isArchived();
        boolean showAllVersions = !showReleased && !showUnreleased && !showArchived;

        if (showAllVersions) {
            return true;
        }

        if (showReleased && isReleased && !isArchived) {
            return true;
        }

        if (showArchived && isArchived) {
            return true;
        }

        if (showUnreleased && !isReleased && !isArchived) {
            return true;
        }

        return false;
    }

    public String getJiraReleasePattern() {
        if (pattern == null) {
            return "";
        }
        return pattern.pattern();
    }

    public void setJiraReleasePattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            this.pattern = null;
        } else {
            this.pattern = Pattern.compile(pattern);
        }
    }

    public String getJiraProjectKey() {
        return projectKey;
    }

    public void setJiraProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getJiraShowReleased() {
        return Boolean.toString(showReleased);
    }

    public void setJiraShowReleased(String showReleased) {
        this.showReleased = Boolean.parseBoolean(showReleased);
    }

    public String getJiraShowArchived() {
        return Boolean.toString(showArchived);
    }

    public void setJiraShowArchived(String showArchived) {
        this.showArchived = Boolean.parseBoolean(showArchived);
    }

    public String getJiraShowUnreleased() {
        return Boolean.toString(showUnreleased);
    }

    public void setShowUnreleased(String jiraShowUnreleased) {
        this.showUnreleased = Boolean.parseBoolean(jiraShowUnreleased);
    }

    @Extension
    @Symbol("jiraReleaseVersion")
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Jira Release Version Parameter";
        }
    }

    public static class Result {
        public final String name;
        public final Long id;

        public Result(final Version version) {
            this.name = version.getName();
            this.id = version.getId();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Result result = (Result) o;
            return Objects.equals(name, result.name) && Objects.equals(id, result.id);
        }
    }
}
