# Notes for maintainers

## Developing in Docker

### Start dockerized Jenkins (for testing)

The command below will start a local Jenkins using the version specified as first argument (or LTS):

```bash
./examples/start_docker.sh 2.249.2
```

### Build the plugin in Docker environment

The command below allows to build the plugin using maven docker image. This is useful to test building against different Maven/JDK versions.
See also [SDKMan](https://sdkman.io/) for a different approach.

```bash
docker run -it --rm -v "$PWD":/usr/src/mymaven -v "$HOME/.m2:/root/.m2" -w /usr/src/mymaven maven:3.3-jdk-8 mvn clean package
```

### Atlassian sources import

To resolve some binary compatibility issues [JENKINS-48357](https://issues.jenkins-ci.org/browse/JENKINS-48357),
the sources from the artifact [com.atlassian.httpclient:atlassian-httpclient-plugin:0.23](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/)
has been imported in the project to have control over http(s) protocol transport layer.
The downloaded sources didn't have any license headers but based on the [pom](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/atlassian-httpclient-plugin-0.23.0.pom)
sources are Apache License (see pom in src/main/resources/atlassian-httpclient-plugin-0.23.0.pom)

### Testing

There is a [Jira Cloud](https://jenkins-jira-plugin.atlassian.net/) test instance that the changes can be tested against, official maintainers are admins that can grant access for testing to PR submitters on a need-to-have basis.

### Releasing the plugin

Make sure you have your `~/.m2/settings.xml` configured accordingly - refer to [releasing Jenkins plugins](https://www.jenkins.io/doc/developer/publishing/releasing/).
