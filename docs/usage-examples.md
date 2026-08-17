# Usage examples

Once the plugin is [configured](configuration.md), here are Pipeline snippets for each step it
provides. See the [jira plugin steps reference](https://www.jenkins.io/doc/pipeline/steps/jira/)
for the full parameter list of each step.

## jiraCommentIssues

Keep a reference to the SCM you used:

```groovy
node {
    def gitScm = git url: 'git@github.com:jenkinsci/jira-plugin.git', branch: 'master'
    sh 'make something'
    jiraCommentIssues(
            issueSelector: DefaultSelector(),
            scm: gitScm)
    gitScm = null
}
```

Clear the `scm` reference once you're done with it, so it isn't serialized between steps.

You can also add labels to the Jira issue:

```groovy
    jiraCommentIssues(
            issueSelector: DefaultSelector(),
            scm: gitScm,
            labels: [ "$version", "jenkins" ])
```

## jiraExecuteWorkflow

```groovy
node {
    jiraExecuteWorkflow(
            jqlSearch: "project = EX and labels = 'jenkins' and labels = '${version}'",
            workflowActionName: 'Resolve Issue',
            comment: 'comment')
}
```

## jiraCreateReleaseNotes

```groovy
node {
    jiraCreateReleaseNotes(jiraProjectKey: 'TST',
            jiraRelease: '1.1.1', jiraEnvironmentVariable: 'notes', jiraFilter: 'status in (Resolved, Closed)')
            {
                // do something useful here —
                // release notes are available in the environment variable named by jiraEnvironmentVariable
                print env.notes
            }
}
```

## jiraMarkVersionReleased

```groovy
node {
    jiraMarkVersionReleased(
            jiraProjectKey: 'TEST',
            jiraRelease: '1.1.1')
}
```

## jiraUpdateIssueField

```groovy
node {
    jiraUpdateIssueField(
            issueSelector: ExplicitSelector("JIRA-123"),
            fieldId: "10001",
            fieldValue: "value"
    )
}
```

## jiraSearch (SearchIssuesStep)

Custom Pipeline step (see [step-api](https://github.com/jenkinsci/workflow-plugin/blob/master/step-api/README.md))
that searches Jira by JQL query directly from a workflow:

```groovy
node {
    List<String> issueKeys = jiraSearch(jql: "project = EX and labels = 'jenkins' and labels = '${version}'")
}
```

## jiraComment (CommentStep)

For jobs that just want to post a single comment:

```groovy
node {
    jiraComment(issueKey: "EX-111", body: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) built. Please go to ${env.BUILD_URL}.")
}
```

## JiraEnvironmentVariableBuilder

Not supported in Pipeline. Outside the Groovy sandbox, you can read the current Jira URL directly:

```groovy
import hudson.plugins.jira.JiraSite;

node {
    String jiraUrl = JiraSite.get(currentBuild.rawBuild).name
    env.JIRA_URL = jiraUrl
}
```

To populate the `JIRA_ISSUES` environment variable, use the `jiraIssueSelector` step:

```groovy
    List<String> issueKeys = jiraIssueSelector()
```

or, with a custom issue selector:

```groovy
    List<String> issueKeys = jiraIssueSelector(new CustomIssueSelector())
```

## What isn't supported in Pipeline yet

Notifiers aren't implemented for Pipeline: a running flow has no build status yet (unlike a
freestyle project, whose status is known before its notifier runs), so there's nothing meaningful
for a notifier to react to. Use the `catchError` step and call the relevant Jira step manually
instead.

If you're adding a new feature, make sure it also works as a Pipeline step — see the workflow
plugin's [compatibility notes](https://github.com/jenkinsci/workflow-plugin/blob/master/COMPATIBILITY.md)
and [core steps reference](https://github.com/jenkinsci/workflow-plugin/blob/master/basic-steps/CORE-STEPS.md)
for how Jenkins core steps integrate with Pipeline jobs.
