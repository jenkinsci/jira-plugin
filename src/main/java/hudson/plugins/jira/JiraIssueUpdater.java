package hudson.plugins.jira;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.Lists;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

/**
 * Parses build changelog for JIRA issue IDs and then
 * updates JIRA issues accordingly.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraIssueUpdater extends Recorder implements MatrixAggregatable, SimpleBuildStep {

    private AbstractIssueSelector issueSelector;
    private SCM scm;
    private List<String> labels;

    @DataBoundConstructor
    public JiraIssueUpdater(AbstractIssueSelector issueSelector, SCM scm, List<String> labels) {
        super();
        this.issueSelector = issueSelector;
        this.scm = scm;
        if(labels != null)
            this.labels = labels;
        else
            this.labels = Lists.newArrayList();
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        // Don't do anything for individual matrix runs.
        if (run instanceof MatrixRun) {
            return;
        } else if(run instanceof AbstractBuild) {
            AbstractBuild<?,?> abstractBuild = (AbstractBuild<?,?>) run;
            Updater updater = new Updater(abstractBuild.getParent().getScm(), labels);
            updater.perform(run, listener, getIssueSelector());
        } else if(scm != null) {
            Updater updater = new Updater(scm, labels);
            updater.perform(run, listener, getIssueSelector());
        } else {
            throw new IllegalArgumentException("Unsupported run type "+run.getClass().getName());
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public AbstractIssueSelector getIssueSelector() {
        AbstractIssueSelector uis = this.issueSelector;
        if (uis == null) uis = new DefaultIssueSelector();
        return (this.issueSelector = uis);
    }

    public SCM getScm() {
        return scm;
    }

    public List<String> getLabels() {
        return labels;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                PrintStream logger = listener.getLogger();
                logger.println("End of Matrix Build. Updating JIRA.");
                Updater updater = new Updater(this.build.getParent().getScm(), labels);
                return updater.perform(this.build, this.listener, getIssueSelector());
            }
        };
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private DescriptorImpl() {
            super(JiraIssueUpdater.class);
        }

        @Override
        public String getDisplayName() {
            // Displayed in the publisher section
            return Messages.JiraIssueUpdater_DisplayName();

        }

        @Override
        public String getHelpFile() {
            return "/plugin/jira/help.html";
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public boolean hasIssueSelectors() {
            return Jenkins.getInstance().getDescriptorList(AbstractIssueSelector.class).size() > 1;
        }
    }

}
