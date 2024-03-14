// Builds a module using https://github.com/jenkins-infra/pipeline-library
buildPlugin(useContainerAgent: true, configurations: [
    [platform: 'linux', jdk: 11], 
    [platform: 'windows', jdk: 11],
    [platform: 'linux', jdk: 17],
    [platform: 'linux', jdk: 21]
])

echo "BRANCH_IS_PRIMARY: ${BRANCH_IS_PRIMARY}"
if (${BRANCH_IS_PRIMARY}) {
    node("docker-highmem") {
        deleteDir()
        dir("localPlugins") {
            sh "git clone https://github.com/jenkinsci/jira-plugin.git -b ${env.CHANGE_BRANCH} jira"
            stash 'localPlugins'
            sh "ls -lrt jira/"
            stash name: 'essentials.yml', includes: 'jira/essentials.yml'
        }

        runPCT(metadataFile: "jira/essentials.yml")
    }
}

