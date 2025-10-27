# Basic Pipeline Usage examples

This page provides common, real-world examples demonstrating how to use the Jenkins JIRA plugin steps within a Declarative Pipeline.

These examples assume you have already configured a JIRA site in the Jenkins System Configuration, which you reference using the site: 'YOUR_JIRA_SITE_ID' parameter.

---

## 1. Transitioning a JIRA Issue to 'Resolved' on Success

This is the most frequent use case: automatically updating the status of the associated JIRA issue once a build succeeds.

This pipeline assumes the JIRA issue key (e.g., PROJ-123) is extracted automatically from the commit message, which is the default behavior when the "Issue Pattern" is correctly configured in Jenkins.

### Declarative Pipeline example
```groovy
pipeline {
    agent any

    stages {
        stage('Build and Test') {
            steps {
                echo "Running build and unit tests..."
                // Application build and test commands go here.
            }
        }
    }

    post {
        success {
            echo "Build successful. Attempting to transition JIRA issue(s)."
            
            //Finds linked issues(e.g., from SCM commits) and transitions them.
            jiraIssueTransition transition: 'Resolved', site: 'YOUR_JIRA_SITE_ID'
            
            echo "JIRA issue status updated."
        }
        failure {
            echo "Build failed. JIRA issue status will remain unchanged."
        }
    }
}

pipeline {
    agent any

    post {
        // Run this block if the build was successful
        success {
            echo "Adding success comment to JIRA."
            jiraAddComment comment: "✅ SUCCESS: The build and all automated tests passed. See the results here: ${env.BUILD_URL}",
                           site: 'YOUR_JIRA_SITE_ID'
        }
        
        // Run this block if the build failed
        failure {
            echo "Adding failure comment to JIRA."
            jiraAddComment comment: "❌ FAILURE: The build failed! Please investigate the console output: ${env.BUILD_URL}",
                           site: 'YOUR_JIRA_SITE_ID'
        }
    }
}

node {
    stage('Health Check') {
        try {
            // Assume 'runHealthCheck()' is a function that throws an error if the service is down
            // runHealthCheck()
            echo "Service health check passed."

        } catch (error) {
            echo "Service health check failed. Creating a new JIRA issue."
            
            // Define the fields for the new issue
            def issueFields = [
                'project': ['key': 'BUGS'], // Key of the project to create the issue in
                'summary': "Production Health Check Failed - ${env.JOB_NAME} Build ${env.BUILD_NUMBER}",
                'issuetype': ['name': 'Bug'],
                'description': "The final deployment health check failed after build ${env.BUILD_URL}. \n\nError details: ${error}"
            ]

            // Create the issue and capture the response
            def newIssue = jiraNewIssue fields: issueFields, site: 'YOUR_JIRA_SITE_ID'

            echo "Created new JIRA issue: ${newIssue.key}"
            env.NEW_JIRA_ISSUE_KEY = newIssue.key

            throw error
        }
    }
}
```

# Advanced Pipeline Usage examples

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
}
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

Custom pipeline step (see [step-api](https://github.com/jenkinsci/workflow-plugin/blob/master/step-api/README.md)) that allow to 
search by jql query directly from workflow.

usage:

```groovy
node {
    List<String> issueKeys = jiraSearch(jql: "project = EX and labels = 'jenkins' and labels = '${version}'")	
}
```

## CommentStep

Interface for Pipeline job types that simply want to post a comment e.g.:

```groovy
node {
    jiraComment(issueKey: "EX-111", body: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) builded. Please go to ${env.BUILD_URL}.")
}
```

## JiraEnvironmentVariableBuilder

Not supported in Pipeline. You can get current jira url (if you are not using the Groovy sandbox):

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
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/basic-steps/CORE-STEPS.md) for more information how core 
jenkins steps integrate with workflow jobs.

Running a notifiers is trickier since normally a flow in progress has no status yet, unlike a freestyle project whose status is 
determined before the notifier is called (never supported).
So notifiers will never be implemented as you can use the catchError step and run jira action manually.
I'm going to create a special pipeline steps to replace this notifiers in future.