package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintStream;

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


	@DataBoundConstructor
	public JiraIssueCreator(String summary, String project, String issueType, String component, String priority, String description) {
		this.summary = summary;
		this.project = project;
        this.issueType = issueType;
        this.component = component;
        this.priority = priority;
        this.description = description;
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

    @Override
	public BuildStepDescriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}
	
	@Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private void validate(String realSummary, String realProject, String realIssueType, String realComponent, String realPriority, String realDescription) {
        if (realSummary == null || realSummary.isEmpty()) {
            throw new IllegalArgumentException("Summary is Empty");
        }
        if (realProject == null || realProject.isEmpty()) {
            throw new IllegalArgumentException("Project is Empty");
        }
        if (realIssueType == null || realIssueType.isEmpty()) {
            throw new IllegalArgumentException("Issue type is Empty");
        }
        if (realComponent == null || realComponent.isEmpty()) {
            throw new IllegalArgumentException("Component is Empty");
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
        String realComponent = "";
        String realPriority = "";
        String realDescription = "";


		try {
			realSummary = build.getEnvironment(listener).expand(summary);
            realProject = build.getEnvironment(listener).expand(project);
            realIssueType = build.getEnvironment(listener).expand(issueType);
            realComponent = build.getEnvironment(listener).expand(component);
            realPriority = build.getEnvironment(listener).expand(priority);
            realDescription = build.getEnvironment(listener).expand(description);

            validate(realSummary, realProject, realIssueType, realComponent, realPriority, realDescription);

            PrintStream logger = listener.getLogger();
            logger.printf("Going to create issue with values:  \nSummary: %s\nProject: %s\nIssueType: %s\nComponent: %s\nPriority: %s\nDescription: %s\n", realSummary,
                    realProject, realIssueType, realComponent, realPriority, realDescription);

			JiraSite site = JiraSite.get(build.getProject());

			site.createIssue( realSummary, realProject, realIssueType, realComponent, realPriority, realDescription);
		} catch (Exception e) {
			e.printStackTrace(listener.fatalError(
					"Unable to create jira issue %s %s %s %s %s %s: %s", realSummary,
					realProject, realIssueType, realComponent, realPriority, realDescription, e));
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

		@Override
		public String getHelpFile() {
			return "/plugin/jira/help-create.html";
		}
	}
}
