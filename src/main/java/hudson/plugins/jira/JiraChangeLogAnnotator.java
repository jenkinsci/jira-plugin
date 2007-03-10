package hudson.plugins.jira;

import hudson.MarkupText;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link ChangeLogAnnotator} that picks up JIRA issue IDs.
 * @author Kohsuke Kawaguchi
 */
public class JiraChangeLogAnnotator extends ChangeLogAnnotator {

    public void annotate(AbstractBuild<?,?> build, Entry change, MarkupText text) {
        AbstractProject ap = build.getParent();
        if (ap instanceof Project) {
            Project p = (Project) ap;
            JiraIssueUpdater updater = (JiraIssueUpdater) p.getPublishers().get(JiraIssueUpdater.DESCRIPTOR);
            if(updater==null)
                return; // not configured with JIRA

            JiraSite site = updater.getSite();
            if(site==null)
                return; // not configured properly

            for(SubText token : text.findTokens(JiraIssueUpdater.ISSUE_PATTERN)) {
                try {
                    token.surroundWith("<a href='"+new URL(site.url,"browse/"+token.group(0))+"'>","</a>");
                } catch (MalformedURLException e) {
                    // impossible
                }
            }
        }

    }
}
