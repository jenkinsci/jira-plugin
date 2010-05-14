package hudson.plugins.jira;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.plugins.jira.soap.RemotePermissionException;
import hudson.scm.RepositoryBrowser;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import org.apache.commons.lang.StringUtils;

/**
 * Actual JIRA update logic.
 *
 * 
 * @author Kohsuke Kawaguchi
 */
class Updater {
    static boolean perform(AbstractBuild<?, ?> build, BuildListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        List<JiraIssue> issues = null;
        
        try {
            JiraSite site = JiraSite.get(build.getProject());
            if(site==null) {
                logger.println(Messages.Updater_NoJiraSite());
                build.setResult(Result.FAILURE);
                return true;
            }
    
            String rootUrl = Hudson.getInstance().getRootUrl();
            if(rootUrl==null) {
                logger.println(Messages.Updater_NoHudsonUrl());
                build.setResult(Result.FAILURE);
                return true;
            }
            
            Set<String> ids = findIssueIdsRecursive(build, site.getIssuePattern(), listener);
    
            if(ids.isEmpty()) {
                if(debug)
                    logger.println("No JIRA issues found.");
                return true;    // nothing found here.
            }
            
            JiraSession session = null;
    		try {
    			session = site.createSession();
    		} catch (ServiceException e) {
    			listener.getLogger().println(Messages.Updater_FailedToConnect());
                e.printStackTrace(listener.getLogger());
    		}
            if(session==null) {
                logger.println(Messages.Updater_NoRemoteAccess());
                build.setResult(Result.FAILURE);
                return true;
            }
    
            boolean doUpdate = false;
            if (site.updateJiraIssueForAllStatus){
                doUpdate = true;
            } else {
              doUpdate = build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
            }
            boolean useWikiStyleComments = site.supportsWikiStyleComment;
            
            issues = getJiraIssues(ids, session, logger);
            build.getActions().add(new JiraBuildAction(build,issues));
            
            if (doUpdate) {
                submitComments(build, logger, rootUrl, issues,
                        session, useWikiStyleComments, site.recordScmChanges);
            } else {
                // this build didn't work, so carry forward the issues to the next build
                build.addAction(new JiraCarryOverAction(issues));
            }
        } catch (Exception e) {
            logger.println("Error updating JIRA issues. Saving issues for next build.\n" + e);
            if (issues != null && !issues.isEmpty()) {
                // updating issues failed, so carry forward issues to the next build
                build.addAction(new JiraCarryOverAction(issues));
            }
        }

        return true;
    }

    /**
     * Submits comments for the given issues.
     * Removes from <code>issues</code> the ones which appear to be invalid.
     */
	static void submitComments(
	            AbstractBuild<?, ?> build, PrintStream logger, String hudsonRootUrl,
	            List<JiraIssue> issues, JiraSession session,
	            boolean useWikiStyleComments, boolean recordScmChanges) throws RemoteException {
	    // copy to prevent ConcurrentModificationException
	    List<JiraIssue> copy = new ArrayList<JiraIssue>(issues);
        for (JiraIssue issue : copy) {
            try {
                logger.println(Messages.Updater_Updating(issue.id));
                StringBuilder aggregateComment = new StringBuilder();
                for(Entry e :build.getChangeSet()){
                    if(e.getMsg().toUpperCase().contains(issue.id)){
                        aggregateComment.append(e.getMsg()).append("\n");
                        // kutzi: don't know why the issue id was removed in previous versions:
                        //aggregateComment = aggregateComment.replaceAll(id, "");

                    }
                }

                session.addComment(issue.id,
                    createComment(build, useWikiStyleComments,
                            hudsonRootUrl, aggregateComment.toString(), recordScmChanges, issue));
            } catch (RemotePermissionException e) {
                // Seems like RemotePermissionException can mean 'no permission' as well as
                // 'issue doesn't exist'.
                // To prevent carrying forward invalid issues forever, we have to drop them
                // even if the cause of the exception was different.
                logger.println("Looks like " + issue.id + " is no valid JIRA issue. Issue will not be updated.\n" + e);
                issues.remove(issue);
            }
        }
    }
	
	private static List<JiraIssue> getJiraIssues( 
            Set<String> ids, JiraSession session, PrintStream logger) throws RemoteException {
        List<JiraIssue> issues = new ArrayList<JiraIssue>(ids.size());
        for (String id : ids) {
            if(!session.existsIssue(id)) {
                if(debug)
                    logger.println(id+" looked like a JIRA issue but it wasn't");
                continue;   // token looked like a JIRA issue but it's actually not.
            }

            issues.add(new JiraIssue(session.getIssue(id)));
        }
        return issues;
    }
	

    /**
     * Creates a comment to be used in JIRA for the build.
     */
	private static String createComment(AbstractBuild<?, ?> build,
			boolean wikiStyle, String hudsonRootUrl, String scmComments, boolean recordScmChanges, JiraIssue jiraIssue) {
		String comment = String.format(
		    wikiStyle ?
		    "Integrated in !%1$simages/16x16/%3$s! [%2$s|%4$s]\n     %5$s":
		    "Integrated in %2$s (See [%4$s])\n    %5$s",
		    hudsonRootUrl,
		    build,
		    build.getResult().color.getImage(),
		    Util.encode(hudsonRootUrl+build.getUrl()),
		    scmComments);
		if (recordScmChanges) {
		    List<String> scmChanges = getScmComments(wikiStyle, build, jiraIssue );
		    StringBuilder sb = new StringBuilder(comment);
		    for (String scmChange : scmChanges)
		    {
		        sb.append( "\n" ).append( scmChange );
		    }
		    return sb.toString();
		}
		return comment;
	}
	
	private static List<String> getScmComments(boolean wikiStyle, AbstractBuild<?, ?> build, JiraIssue jiraIssue)
	{
	    RepositoryBrowser repoBrowser = null;
	    if (build.getProject().getScm() != null) {
	        repoBrowser = build.getProject().getScm().getEffectiveBrowser();
	    }
        List<String> scmChanges = new ArrayList<String>();
	    for (Entry change : build.getChangeSet()) {
	        if (jiraIssue != null  && !StringUtils.contains( change.getMsg(), jiraIssue.id )) {
	            continue;
	        }
	        try {
    	        String uid = change.getAuthor().getId();
    	        URL url = repoBrowser == null ? null : repoBrowser.getChangeSetLink( change );
    	        StringBuilder scmChange = new StringBuilder();
    	        if (StringUtils.isNotBlank( uid )) {
    	            scmChange.append( uid ).append( " : " );
    	        }
    	        if (url != null  && StringUtils.isNotBlank( url.toExternalForm() )) {
    	            if (wikiStyle) {
    	                String revision = getRevision( change );
    	                if (revision != null)
    	                {
    	                    scmChange.append( "[" ).append( revision );
    	                    scmChange.append( "|" );
    	                    scmChange.append( url.toExternalForm() ).append( "]" );
    	                }
    	                else
    	                {
    	                    scmChange.append( "[" ).append( url.toExternalForm() ).append( "]" );
    	                }
    	            } else {
    	                scmChange.append( url.toExternalForm() );
    	            }
    	        }
    	        scmChange.append( "\nFiles : " ).append( "\n" );
    	        for (AffectedFile affectedFile : change.getAffectedFiles()) {
    	            scmChange.append( "* " ).append( affectedFile.getPath() ).append( "\n" );
    	        }
    	        if (scmChange.length()>0) {
    	            scmChanges.add( scmChange.toString() );
    	        }
	        } catch (IOException e) {
	            LOGGER.warning( "skip failed to calculate scm repo browser link " + e.getMessage() );
	        }
	    }
	    return scmChanges;
	}
	
	private static String getRevision(Entry entry) {
	    // svn at least can get the revision
	    try {
	        Class<?> clazz = entry.getClass();
	        Method method = clazz.getMethod( "getRevision", (Class[])null );
	        if (method==null){
	            return null;
	        }
	        Object revObj = method.invoke( entry, (Object[])null );
	        return (revObj != null) ? revObj.toString() : null;
	    } catch (Exception e) {
	        return null;
	    }
	}
	

    /**
     * Finds the strings that match JIRA issue ID patterns.
     *
     * This method returns all likely candidates and doesn't check
     * if such ID actually exists or not. We don't want to use
     * {@link JiraSite#existsIssue(String)} here so that new projects
     * in JIRA can be detected.
     */
    private static Set<String> findIssueIdsRecursive(AbstractBuild<?,?> build, Pattern pattern,
    		BuildListener listener) {
        Set<String> ids = new HashSet<String>();

        // first, issues that were carried forward.
        Run<?, ?> prev = build.getPreviousBuild();
        if(prev!=null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if(a!=null)
                ids.addAll(a.getIDs());
        }

        // then issues in this build
        findIssues(build,ids, pattern, listener);

        // check for issues fixed in dependencies
        for( DependencyChange depc : build.getDependencyChanges(build.getPreviousBuild()).values())
            for(AbstractBuild<?, ?> b : depc.getBuilds())
                findIssues(b,ids, pattern, listener);

        return ids;
    }

    /**
     * @param pattern pattern to use to match issue ids
     */
    static void findIssues(AbstractBuild<?,?> build, Set<String> ids, Pattern pattern,
    		BuildListener listener) {
        for (Entry change : build.getChangeSet()) {
            LOGGER.fine("Looking for JIRA ID in "+change.getMsg());
            Matcher m = pattern.matcher(change.getMsg());
            
            while (m.find()) {
            	if (m.groupCount() >= 1) {
	                String content = StringUtils.upperCase( m.group(1));
	                ids.add(content);
            	} else {
            		listener.getLogger().println("Warning: The JIRA pattern " + pattern + " doesn't define a capturing group!");
            	}
            }
            
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Updater.class.getName());

    /**
     * Debug flag.
     */
    public static boolean debug = false;
}
