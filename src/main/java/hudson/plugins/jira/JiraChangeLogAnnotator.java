package hudson.plugins.jira;

import hudson.MarkupText;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

/**
 * {@link ChangeLogAnnotator} that picks up JIRA issue IDs.
 * @author Kohsuke Kawaguchi
 */
public class JiraChangeLogAnnotator extends ChangeLogAnnotator {

    public void annotate(AbstractBuild<?,?> build, Entry change, MarkupText text) {
        JiraBuildAction a = build.getAction(JiraBuildAction.class);
        if(a==null)
            return; // not configured with JIRA

        for(SubText token : text.findTokens(JiraIssueUpdater.ISSUE_PATTERN)) {
            try {
                String id = token.group(0);
                URL url = a.getUrl(id);
                if(url==null)   continue;

                JiraIssue issue = a.getIssue(id);

                if(issue==null) {
                    token.surroundWith("<a href='"+url+"'>","</a>");
                } else {
                    token.surroundWith(
                        MessageFormat.format("<a href=''{0}'' id=''JIRA-{1}''>",url,issue.id),
                        MessageFormat.format("</a><script>makeTooltip(''JIRA-{1}'',''{0}'');</script>",
                            issue.title,issue.id));
                }
            } catch (MalformedURLException e) {
                // impossible
            }
        }
    }
}
