# Contribution guidelines

General rules:

- check the [general Jenkins development guide](https://www.jenkins.io/doc/developer/book/)
- make sure to provide tests
- mark the Pull Request as _draft_ initially, to make sure all the checks pass correctly, then convert it to non-draft.

## Setting up your environment

### Install pre-commit hooks

The [pre-commit](https://pre-commit.com/#install) hooks run various checks to make sure no unwanted files are committed and that the submitted change follows the code style and formatting rules:

```sh
brew install pre-commit && pre-commit install --install-hooks
```

## Notes for maintainers

### Local testing

Use [docker-compose](./docker-compose.yml) to run a local Jenkins instance with the plugin installed. The configuration includes local volumes for both: Jenkins and ssh-agent, so you can easily test the plugin in a clean environment.

```bash

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
