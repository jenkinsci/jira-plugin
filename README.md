Jenkins JIRA Plugin
===================

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/jira-plugin/master)](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fjira-plugin/activity/)
[![Build Status](https://travis-ci.org/jenkinsci/jira-plugin.svg?branch=master)](https://travis-ci.org/jenkinsci/jira-plugin)
[![Coverage Status](https://coveralls.io/repos/jenkinsci/jira-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/jenkinsci/jira-plugin?branch=master)

Check documentation at [JIRA plugin page](https://plugins.jenkins.io/jira).

Reported Issues:
* Next Release:
[[ToDo]](https://issues.jenkins-ci.org/issues/?filter=14997)
[[Done]](https://issues.jenkins-ci.org/issues/?filter=14998)
* Bugs: [[All]](https://issues.jenkins-ci.org/issues/?filter=14761) [[Confirmed]](https://issues.jenkins-ci.org/issues/?filter=14996)
* Other: [[All Non-Bugs]](https://issues.jenkins-ci.org/issues/?filter=14762)
[[All Unresolved]](https://issues.jenkins-ci.org/issues/?filter=14956)
* Categorized:
[[by Votes]](https://issues.jenkins-ci.org/issues/?filter=15156)
[[by Priority]](https://issues.jenkins-ci.org/issues/?filter=15157)

### Common issues

#### Jenkins<>JIRA SSL connectivity problems

If you encounter stacktraces like this:
```
Caused by: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

make sure the JRE/JDK that Jenkins master is running (or the Jenkins slaves are running) contain the valid CA chain certificates that JIRA is running with.
You can test it using this [SSLPoke.java class](https://gist.github.com/warden/e4ef13ea60f24d458405613be4ddbc51)

like this:
```
$ wget -O SSLPoke.java https://gist.githubusercontent.com/warden/e4ef13ea60f24d458405613be4ddbc51/raw/7f258a30be4ddea7b67239b40ae305f6a2e98e0a/SSLPoke.java

$ /usr/java/jdk1.8.0_131/bin/javac SSLPoke.java

$ /usr/java/jdk1.8.0_131/jre/bin/java SSLPoke jira.domain.com 443
Successfully connected
```

References:
* [Jenkins fails with PKIX Path building error](https://stackoverflow.com/questions/52842214/jenkins-fails-with-pkix-path-building-error)
* [PKIX path building failed error message
](https://support.cloudbees.com/hc/en-us/articles/217078498-PKIX-path-building-failed-error-message)

### Contributing to the Plugin

See [examples](examples/) directory for some useful scripts like:

* [docker_build.sh](examples/docker_build.sh) for building using Docker maven image
* [docker-compose.yaml](examples/docker-compose.yaml) for running a complete development

New feature proposals and bug fix proposals should be submitted as [GitHub pull requests](https://help.github.com/articles/creating-a-pull-request).

There are two active branches:

* master - bugfixes and development of new features - major x.Y versions are released from this branch
* hotfix - bugfix branch - selected commits are cherry picked from master branch - patch x.y.Z are released from this branch

Fork the repository on GitHub, prepare your change on your forked copy, and submit a pull request (see [here](https://github.com/jenkinsci/jira-plugin/pulls) for open pull requests).

Your pull request will be evaluated by the [Travis CI Job](https://travis-ci.org/jenkinsci/jira-plugin)  and you should receive e-mail with the results of the evaluation.

If you are adding new features please make sure that they support Jenkins Pipeline Plugin.
See (here](https://github.com/jenkinsci/pipeline-plugin/blob/master/COMPATIBILITY.md) for some information.

Before submitting your change make sure that:
* you added tests - the coverage will be checked after submitting PRs
* the code formatting follows the plugin standard (i.e. how most of the source code is formatted)
* imports are organised - please do not use wildcard imports
* you use findbugs to see if you haven't introduced any new warnings.

There have been many developers involved in the git plugin and there are many, many users who depend on the git-plugin.  
Tests help us assure that we're delivering a reliable plugin, and that we've communicated our intent to other developers in a way that they can detect when they run tests.

#### Building plugin with Docker

Build the plugin locally using Docker and Maven image version 3.3 & newest JDK 8:

    docker run -it --rm -v "$PWD":/usr/src/mymaven -v "$HOME/.m2:/usr/src/mymaven/.m2" -w /usr/src/mymaven maven:3.3-jdk-8 mvn clean package

#### Atlassian sources import

To resolve some binary compatibility issues [JENKINS-48357](https://issues.jenkins-ci.org/browse/JENKINS-48357),
the sources from the artifact [com.atlassian.httpclient:atlassian-httpclient-plugin:0.23](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/)
has been imported in the project to have control over http(s) protocol transport layer.
The downloaded sources didn't have any license headers but based on the [pom](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/atlassian-httpclient-plugin-0.23.0.pom)
sources are Apache License (see pom in src/main/resources/atlassian-httpclient-plugin-0.23.0.pom)   
