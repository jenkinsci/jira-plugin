# JIRA Plugin Usage Examples

This page provides common, real-world examples demonstrating how to use the Jenkins JIRA plugin steps within a Declarative Pipeline.

These examples assume you have already configured a JIRA site in the Jenkins System Configuration, which you reference using the site: 'YOUR_JIRA_SITE_ID' parameter.

---

## 1. Transitioning a JIRA Issue to 'Resolved' on Success

This is the most frequent use case: automatically updating the status of the associated JIRA issue once a build succeeds.

This pipeline assumes the JIRA issue key (e.g., PROJ-123) is extracted automatically from the commit message, which is the default behavior when the "Issue Pattern" is correctly configured in Jenkins.

### Declarative Pipeline Example
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