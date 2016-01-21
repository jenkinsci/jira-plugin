# Plugin Compatibility with [Pipeline](https://github.com/jenkinsci/workflow-plugin)
(formerly known as workflow plugin)

This document captures the status of features to be compatible or incompatible.

##JiraIssueUpdater usage example

You need keep reference to used scm.
As an example, you can write a flow:

```groovy
node {
    def gitScm = git url: 'git@github.com:jenkinsci/jira-plugin.git', branch: 'master'
    sh 'make something'
    step([$class: 'hudson.plugins.jira.JiraIssueUpdater', 
            issueSelector: [$class: 'hudson.plugins.jira.DefaultUpdaterIssueSelector'], 
            scm: gitScm])            
    gitScm = null
}
```

Note that a pointer to scm class should be better cleared to not serialize scm object between steps.

You can add some labels to issue in jira:
```groovy
    step([$class: 'hudson.plugins.jira.JiraIssueUpdater', 
            issueSelector: [$class: 'hudson.plugins.jira.DefaultUpdaterIssueSelector'], 
            scm: gitScm,
            labels: [ "$version", "jenkins" ]])            
```

##SearchIssuesStep

Custom pipeline step (see [step-api](https://github.com/jenkinsci/workflow-plugin/blob/master/step-api/README.md)) that allow to search by jql query directly from workflow.

usage:
```groovy
node {
    List<String> issueKeys = jiraSearch(jql: "project = EX and labels = 'jenkins' and labels = '${version}'")	
}
```

##CommentStep

Interface for Pipeline job types that simply want to post a comment e.g.
```groovy
node {
    jiraComment(issueKey: "EX-111", body: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) builded. Please go to ${env.BUILD_URL}.")
}
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
- [X] `Jira Version Parameter` supported
- [X] `JiraChangeLogAnnotator` supported
- [X] `JiraIssueUpdater` supported
- [ ] `JiraIssueUpdateBuilder` not supported yet
- [ ] `JiraCreateReleaseNotes` not supported yet
- [ ] `JiraCreateIssueNotifier` never supported
- [ ] `JiraIssueMigrator` never supported
- [ ] `JiraReleaseVersionUpdater` never supported
- [ ] `JiraReleaseVersionUpdaterBuilder` not supported yet
- [ ] `JiraVersionCreator` never supported
