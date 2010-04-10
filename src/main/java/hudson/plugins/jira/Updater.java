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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import org.apache.commons.lang.StringEscapeUtils;
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
            if (debug) {
                logger.println("site.userPattern " + site.userPattern );
            }
            Set<String> ids = findIssueIdsRecursive(build, site.userPattern);
    
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
    
            boolean doUpdate = build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
            
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
		    "Integrated in !%1$snocacheImages/16x16/%3$s! [%2$s|%4$s]\n     %5$s":
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
	    if (build.getProject().getScm() == null) {
	        return Collections.<String>emptyList();
	    }
        if (build.getProject().getScm().getEffectiveBrowser() == null) {
            return Collections.<String>emptyList();
        }	    
        List<String> scmChanges = new ArrayList<String>();
	    RepositoryBrowser repoBrowser = build.getProject().getScm().getEffectiveBrowser();
	    for (Entry change : build.getChangeSet()) {
	        if (jiraIssue != null  && !StringUtils.contains( change.getMsg(), jiraIssue.id )) {
	            continue;
	        }
	        try {
    	        String uid = change.getAuthor().getId();
    	        URL url = repoBrowser.getChangeSetLink( change );
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
	        Method method = clazz.getMethod( "getRevision", null );
	        if (method==null){
	            return null;
	        }
	        Object revObj = method.invoke( entry, null );
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
    private static Set<String> findIssueIdsRecursive(AbstractBuild<?,?> build, String pattern) {
        Set<String> ids = new HashSet<String>();

        // first, issues that were carried forward.
        Run<?, ?> prev = build.getPreviousBuild();
        if(prev!=null) {
            JiraCarryOverAction a = prev.getAction(JiraCarryOverAction.class);
            if(a!=null)
                ids.addAll(a.getIDs());
        }

        // then issues in this build
        findIssues(build,ids, pattern );

        // check for issues fixed in dependencies
        for( DependencyChange depc : build.getDependencyChanges(build.getPreviousBuild()).values())
            for(AbstractBuild<?, ?> b : depc.getBuilds())
                findIssues(b,ids, pattern );

        return ids;
    }

    /**
     * @param build
     * @param ids
     * @param pattern if pattern is <code>null</code> the default one is used {@value #ISSUE_PATTERN}
     */
    static void findIssues(AbstractBuild<?,?> build, Set<String> ids, String userPattern) {
        Pattern pattern = userPattern == null ? ISSUE_PATTERN : Pattern.compile( userPattern );
        for (Entry change : build.getChangeSet()) {
            LOGGER.fine("Looking for JIRA ID in "+change.getMsg());
            Matcher m = pattern.matcher(change.getMsg());
            
            while (m.find()) {
                String content = StringUtils.upperCase( m.group(1));
                ids.add(content);
            }
            
        }
    }

    /**
     * Regexp pattern that identifies JIRA issue token.
     *
     * <p>
     * First char must be a letter, then at least one letter, digit or underscore.
     * See issue HUDSON-729, HUDSON-4092
     */
    public static final Pattern ISSUE_PATTERN = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]+-[1-9][0-9]*)");

    private static final Logger LOGGER = Logger.getLogger(Updater.class.getName());

    /**
     * Debug flag.
     */
    public static boolean debug = false;
}
