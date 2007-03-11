package hudson.plugins.jira;

import hudson.Plugin;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Jobs;
import hudson.tasks.BuildStep;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.addRecorder(JiraIssueUpdater.DESCRIPTOR);
        Jobs.PROPERTIES.add(JiraProjectProperty.DESCRIPTOR);
        new JiraChangeLogAnnotator().register();
    }
}
