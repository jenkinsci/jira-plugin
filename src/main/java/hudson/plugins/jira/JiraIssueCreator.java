package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintStream;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static hudson.plugins.jira.util.ListBoxModelUtil.emptyModel;
import static hudson.plugins.jira.util.ListBoxModelUtil.of;
import static hudson.plugins.jira.util.ListBoxModelUtil.convertJiraIssueType;
import static hudson.plugins.jira.util.ListBoxModelUtil.convertJiraPriority;
import static hudson.plugins.jira.util.ListBoxModelUtil.convertJiraComponent;

/**
 * Task which creates a new jira issue.
 * 
 * @author jdewinne
 * 
 */
public class JiraIssueCreator extends Notifier {

	private static final long serialVersionUID = 699563338312232811L;

	private String summary;
	private String project;
    private String issueType;
    private String component;
    private String priority;
    private String description;
    private String jqlQuery;


	@DataBoundConstructor
	public JiraIssueCreator(String summary, String project, String issueType, String component, String priority, String description, EnableCheckExistingBlock enableCheckExistingBlock) {
		this.summary = summary;
		this.project = project;
        this.issueType = issueType;
        this.component = component;
        this.priority = priority;
        this.description = description;
        if (enableCheckExistingBlock != null)
        {
            this.jqlQuery = enableCheckExistingBlock.jqlQuery;
        }
	}

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJqlQuery() {
        return jqlQuery;
    }

    public void setJqlQuery(String jqlQuery) {
        this.jqlQuery = jqlQuery;
    }

    public static class EnableCheckExistingBlock
    {
        private String jqlQuery;

        @DataBoundConstructor
        public EnableCheckExistingBlock(String jqlQuery)
        {
            this.jqlQuery = jqlQuery;
        }
    }

    @Override
	public BuildStepDescriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private void validate(String realSummary, String realProject, String realIssueType, String component, String realPriority, String realDescription) {
        if (realSummary == null || realSummary.isEmpty()) {
            throw new IllegalArgumentException("Summary is Empty");
        }
        if (realProject == null || realProject.isEmpty()) {
            throw new IllegalArgumentException("Project is Empty");
        }
        if (realIssueType == null || realIssueType.isEmpty()) {
            throw new IllegalArgumentException("Issue type is Empty");
        }
        if (component == null || component.isEmpty()) {
            throw new IllegalArgumentException("Components is Empty");
        }
        if (realPriority == null || realPriority.isEmpty()) {
            throw new IllegalArgumentException("Priority is Empty");
        }
        if (realDescription == null || realDescription.isEmpty()) {
            throw new IllegalArgumentException("Description is Empty");
        }

    }

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) {
		String realSummary = "";
        String realProject = "";
        String realIssueType = "";
        String realPriority = "";
        String realDescription = "";
        String realJqlQuery = "";


		try {
			realSummary = build.getEnvironment(listener).expand(summary);
            realProject = build.getEnvironment(listener).expand(project);
            realIssueType = build.getEnvironment(listener).expand(issueType);
            realPriority = build.getEnvironment(listener).expand(priority);
            realDescription = build.getEnvironment(listener).expand(description);
            realJqlQuery =  Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(jqlQuery));


            validate(realSummary, realProject, realIssueType, component, realPriority, realDescription);

            PrintStream logger = listener.getLogger();
            logger.printf("Going to create issue with values:  \nSummary: %s\nProject: %s\nIssueType: %s\nComponent: %s\nPriority: %s\nDescription: %s\nJqlQuery: %s\n", realSummary,
                    realProject, realIssueType, component, realPriority, realDescription, realJqlQuery);

            JiraSite site = JiraSite.get(build.getProject());
            if (StringUtils.isNotEmpty(realJqlQuery)) {
                logger.printf("Checking for existing with query: %s\n", realJqlQuery);
                Set<JiraIssue> issues = site.getIssuesFromJqlSearch(realJqlQuery);
                if(null != issues && issues.size() > 0) {
                    logger.printf("Existing issues found. No issue will be created\n");
                    return true;
                }
            }


			site.createIssue(realSummary, realProject, realIssueType, component, realPriority, realDescription);
		} catch (Exception e) {
			e.printStackTrace(listener.fatalError(
					"Unable to create jira issue %s %s %s %s %s %s: %s", realSummary,
					realProject, realIssueType, component, realPriority, realDescription, e));
			listener.finished(Result.FAILURE);
			return false;
		}
		return true;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
	
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(JiraIssueCreator.class);
		}

		@Override
		public JiraIssueCreator newInstance(StaplerRequest req,
				JSONObject formData) throws FormException {
			return req.bindJSON(JiraIssueCreator.class, formData);
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.JiraIssueCreator_DisplayName();
		}

        public JiraSite getSite() {
            // none is explicitly configured. try the default ---
            // if only one is configured, that must be it.
            JiraSite[] sites = JiraProjectProperty.DESCRIPTOR.getSites();
            if(sites.length==1) return sites[0];

            return null;
        }

		@Override
		public String getHelpFile() {
			return "/plugin/jira/help-create.html";
		}

        public ListBoxModel doFillProjectItems() {
            JiraSite site = getSite();
            Set<String> keys = site.getProjectKeys();
            return CollectionUtils.isEmpty(keys) ? emptyModel() : of(keys);
        }

        public ListBoxModel doFillPriorityItems() {
            JiraSite site = getSite();
            Set<JiraPriority> priorities = site.getPriorities();
            return CollectionUtils.isEmpty(priorities) ? emptyModel() : convertJiraPriority(priorities);
        }

        public ListBoxModel doFillIssueTypeItems(@QueryParameter String project) {
            JiraSite site = getSite();
            Set<JiraIssueType> issueTypes = site.getIssueTypes(project);
            return CollectionUtils.isEmpty(issueTypes) ? emptyModel() : convertJiraIssueType(issueTypes);
        }

        public ListBoxModel doFillComponentItems(@QueryParameter String project) {
            JiraSite site = getSite();
            Set<JiraComponent> components = site.getComponents(project);
            return CollectionUtils.isEmpty(components) ? emptyModel() : convertJiraComponent(components);
        }


	}
}
