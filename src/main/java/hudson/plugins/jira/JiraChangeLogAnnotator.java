package hudson.plugins.jira;

import hudson.Extension;
import hudson.MarkupText;
import hudson.Util;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ChangeLogAnnotator} that picks up JIRA issue IDs.
 * @author Kohsuke Kawaguchi
 */
@Extension
public class JiraChangeLogAnnotator extends ChangeLogAnnotator {
    private static final Logger LOGGER = Logger.getLogger(JiraChangeLogAnnotator.class.getName());
    @Override
	public void annotate(AbstractBuild<?,?> build, Entry change, MarkupText text) {
        JiraSite site = getSiteForProject(build.getProject());
        if(site==null)      return;    // not configured with JIRA

        // if there's any recorded detail information, try to use that, too.
        JiraBuildAction a = build.getAction(JiraBuildAction.class);
        
        Set<JiraIssue> issuesToBeSaved = new HashSet<JiraIssue>();
        
        for(SubText token : text.findTokens(site.getIssuePattern())) {
            try {
            	String id;
            	try {
            		id = token.group(1).toUpperCase();
            	} catch (ArrayIndexOutOfBoundsException e) {
            		// ugly hack to detect that there is no group 1
            		// currently (1.355) SubText doesn't provide a groupCount() o.s.l.t. 
            		LOGGER.log(Level.WARNING,
            				"Issue pattern " + site.getIssuePattern() + " doesn't seem to have a capturing group. ",
            				e);
            		continue;
            	}
                if(!site.existsIssue(id))
                    continue;
                URL url = site.getUrl(id);

                JiraIssue issue = null;
                if (a != null) {
                    issue = a.getIssue(id);
                }

                if (issue == null) {
                    try {
                        issue = site.getIssue(id);
                        issuesToBeSaved.add(issue);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error getting remote issue", e);
                    }
                }

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
        
        if (!issuesToBeSaved.isEmpty()) {
            saveIssues(build, a, issuesToBeSaved);
        }
    }
    
    private void saveIssues(AbstractBuild<?, ?> build, JiraBuildAction a,
            Set<JiraIssue> issuesToBeSaved) {
        if (a != null) {
            a.addIssues(issuesToBeSaved);
        } else {
            JiraBuildAction action = new JiraBuildAction(build, issuesToBeSaved);
            build.addAction(action);
        }
        
        try {
            build.save();
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Error saving updated build", e);
        }
    }

    JiraSite getSiteForProject(AbstractProject<?, ?> project) {
        return JiraSite.get(project);
    }
}
