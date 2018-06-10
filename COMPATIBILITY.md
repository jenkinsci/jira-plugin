# Plugin Compatibility with [Pipeline](https://github.com/jenkinsci/pipeline-plugin)
(formerly known as Pipeline plugin)

This document captures the status of features to be compatible or incompatible.

## JiraIssueUpdater usage example

You need keep reference to used scm.
As an example, you can write a flow:

```groovy
node {
    def gitScm = git url: 'git@github.com:jenkinsci/jira-plugin.git', branch: 'master'
    sh 'make something'
    step([$class: 'hudson.plugins.jira.JiraIssueUpdater', 
            issueSelector: [$class: 'hudson.plugins.jira.selector.DefaultIssueSelector'], 
            scm: gitScm])            
    gitScm = null
}
```

Note that a pointer to scm class should be better cleared to not serialize scm object between steps.

You can add some labels to issue in jira:
```groovy
    step([$class: 'hudson.plugins.jira.JiraIssueUpdater', 
            issueSelector: [$class: 'hudson.plugins.jira.selector.DefaultIssueSelector'], 
            scm: gitScm,
            labels: [ "$version", "jenkins" ]])            
```

## JiraIssueUpdateBuilder  usage example

```groovy
node {
    step([$class: 'hudson.plugins.jira.JiraIssueUpdateBuilder', 
            jqlSearch: "project = EX and labels = 'jenkins' and labels = '${version}'",
            workflowActionName: 'Resolve Issue',
            comment: 'comment'])
```

## JiraCreateReleaseNotes usage example

```groovy
node {
    wrap([$class: 'hudson.plugins.jira.JiraCreateReleaseNotes', jiraProjectKey: 'TST', 
	    jiraRelease: '1.1.1', jiraEnvironmentVariable: 'notes', jiraFilter: 'status in (Resolved, Closed)']) 
	{
        //do some useful here
		//release notes can be found in environment variable jiraEnvironmentVariable
		print env.notes
    }

```

## JiraReleaseVersionUpdaterBuilder usage example

```groovy
node {
    step([$class: 'hudson.plugins.jira.JiraReleaseVersionUpdaterBuilder', 
            jiraProjectKey: 'TEST', 
            jiraRelease: '1.1.1'])            
}
```

## SearchIssuesStep

Custom pipeline step (see [step-api](https://github.com/jenkinsci/workflow-plugin/blob/master/step-api/README.md)) that allow to search by jql query directly from workflow.

usage:
```groovy
node {
    List<String> issueKeys = jiraSearch(jql: "project = EX and labels = 'jenkins' and labels = '${version}'")	
}
```

## CommentStep

Interface for Pipeline job types that simply want to post a comment e.g.
```groovy
node {
    jiraComment(issueKey: "EX-111", body: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) builded. Please go to ${env.BUILD_URL}.")
}
```

##JiraEnvironmentVariableBuilder
Is not supported in Pipeline. You can get current jira url (if you are not using the Groovy sandbox):

```groovy
import hudson.plugins.jira.JiraSite;

node {
    String jiraUrl = JiraSite.get(currentBuild.rawBuild).name    	
    env.JIRA_URL = jiraUrl
}
```

To replace JIRA_ISSUES env variable, you can use pipeline step jiraIssueSelector:
```groovy
    List<String> issueKeys = jiraIssueSelector()
```

or if you use custom issue selector:
```groovy
    List<String> issueKeys = jiraIssueSelector(new CustomIssueSelector())
```

## Other features

Some features are currently not supported in pipeline.
If you are adding new features please make sure that they support Jenkins pipeline Plugin.
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/COMPATIBILITY.md) for some information.
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/basic-steps/CORE-STEPS.md) for more information how core jenkins steps integrate with workflow jobs.

Running a notifiers is trickier since normally a flow in progress has no status yet, unlike a freestyle project whose status is determined before the notifier is called (never supported).
So notifiers will never be implemented as you can use the catchError step and run jira action manually.
I'm going to create a special pipeline steps to replace this notifiers in future.

Other builders will be supported in future (not supported yet).

##Current status:

- [X] `JIRA Issue Parameter` supported
- [X] `JIRA Version Parameter` supported
- [X] `JiraChangeLogAnnotator` supported
- [X] `JiraIssueUpdater` supported
- [X] `JiraIssueUpdateBuilder` supported
- [X] `JiraCreateReleaseNotes` supported
- [ ] `JiraCreateIssueNotifier` never supported
- [ ] `JiraIssueMigrator` never supported
- [ ] `JiraReleaseVersionUpdater` never supported
- [X] `JiraReleaseVersionUpdaterBuilder` supported
- [ ] `JiraVersionCreator` never supported
- [ ] `JiraEnvironmentVariableBuilder` not supported (workaround exists)