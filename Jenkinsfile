// Builds a module using https://github.com/jenkins-infra/pipeline-library
buildPlugin(useAci: true, configurations: [
        [ platform: "linux", jdk: "8" ],
        [ platform: "windows", jdk: "8" ],
        [ platform: "linux", jdk: "11" ]
])

node("docker-highmem") {
    deleteDir()
    dir("localPlugins") {
        sh "git clone https://github.com/jenkinsci/jira-plugin.git jira"
        stash 'localPlugins'
    }
    runPCT(metadataFile: "jira/essentials.yml")
}

