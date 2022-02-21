// Builds a module using https://github.com/jenkins-infra/pipeline-library
buildPlugin(useAci: true, configurations: [
        [ platform: "linux", jdk: "8" ],
        [ platform: "windows", jdk: "8" ],
        [ platform: "linux", jdk: "11" ]
])

echo "${BRANCH_IS_PRIMARY}"
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

