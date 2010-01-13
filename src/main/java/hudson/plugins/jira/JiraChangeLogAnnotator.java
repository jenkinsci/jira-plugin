package hudson.plugins.jira;

import hudson.Extension;
import hudson.MarkupText;
import hudson.Util;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ChangeLogAnnotator} that picks up JIRA issue IDs.
 * @author Kohsuke Kawaguchi
 */
@Extension
public class JiraChangeLogAnnotator extends ChangeLogAnnotator {

    @Override
	public void annotate(AbstractBuild<?,?> build, Entry change, MarkupText text) {
        JiraSite site = getSiteForProject(build.getProject());
        if(site==null)      return;    // not configured with JIRA

        // if there's any recorded detail information, try to use that, too.
        Map<String, JiraIssue> associatedIssues = fetchAllAssociatatedIssues(build);

        for(SubText token : text.findTokens(Updater.ISSUE_PATTERN)) {
            try {
                String id = token.group(0).toUpperCase();
                if(!site.existsIssue(id))
                    continue;
                URL url = site.getUrl(id);

                JiraIssue issue = associatedIssues.get(id);

                if(issue==null) {
                    token.surroundWith("<a href='"+url+"'>","</a>");
                } else {
                    token.surroundWith(
                        String.format("<a href='%s' tooltip='%s'>",url, Util.escape(issue.title)),
                        "</a>");
                }
            } catch (MalformedURLException e) {
            	throw new AssertionError(e); // impossible
            }
        }
    }
    
    /**
     * Fetches all {@link JiraIssue}s associated with the build and all
     * its upstream builds.
     */
    @SuppressWarnings("unchecked")
    private Map<String, JiraIssue> fetchAllAssociatatedIssues(AbstractBuild<?,?> build) {
        Map<String, JiraIssue> issues = new HashMap<String, JiraIssue>();
        
        JiraBuildAction a = build.getAction(JiraBuildAction.class);
        if (a != null) {
            for (JiraIssue i : a.issues) {
                issues.put(i.id, i);
            }
        }
        
        Map<AbstractProject, Integer> transitiveUpstreamBuilds = build.getTransitiveUpstreamBuilds();
        
        for (Map.Entry<AbstractProject, Integer> entry : transitiveUpstreamBuilds.entrySet()) {
            AbstractProject project = entry.getKey();
            Run run = project.getBuildByNumber(entry.getValue().intValue());
            if (run != null) {
                a = run.getAction(JiraBuildAction.class);
                if (a != null) {
                    for (JiraIssue i : a.issues) {
                        issues.put(i.id, i);
                    }
                }
            }
        }
        
        return issues;
    }

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }
}
