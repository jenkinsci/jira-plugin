Jenkins JIRA Plugin
===================

[![Documentation](https://img.shields.io/jenkins/plugin/v/jira.svg?label=Documentation)](https://plugins.jenkins.io/jira)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/jira-plugin.svg?label=Release)](https://github.com/jenkinsci/jira-plugin/releases/latest)
[![Jenkins CI](https://ci.jenkins.io/buildStatus/icon?job=Plugins/jira-plugin/master)](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fjira-plugin/activity/)
[![Travis CI](https://travis-ci.org/jenkinsci/jira-plugin.svg?branch=master)](https://travis-ci.org/jenkinsci/jira-plugin)

[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/jira.svg?color=blue)](https://stats.jenkins.io/pluginversions/jira.html)
[![Coverage](https://coveralls.io/repos/jenkinsci/jira-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/jenkinsci/jira-plugin?branch=master)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/jira-plugin.svg)](https://github.com/jenkinsci/jira-plugin/graphs/contributors)


#### About the plugin

This plugin integrates with Jenkins the [Atlassian JIRA Software](http://www.atlassian.com/software/jira/) (both Cloud and Server versions).

#### Using JIRA REST API

This plugin has an optional feature to update JIRA issues with a back
pointer to Jenkins build pages. This allows the submitter and watchers
to quickly find out which build they need to pick up to get the fix.

![](docs/images/Plugin_Configuration.jpg)

  

#### JIRA Issue links in build Changelog

When you configure your JIRA site in Jenkins, the plugin will
automatically hyperlink all matching issue names to JIRA.

If you have additionally provided username/password to JIRA, the
hyperlinks will also contain tooltips with the issue summary.

![](docs/images/example_annotated_changelog.png)

#### Updating JIRA issues with back pointers

If you also want to use this feature, you need to supply a valid user
id/password. If you need the comment only to be visible to a certain
JIRA group, e.g. *Software Development*, enter the groupname. 

Now you also need to configure jobs. I figured you might not always have
write access to the JIRA (say you have a Jenkins build for one of the
Apache commons project that you depend on), so that's why this is
optional.  

And the following screen shows how JIRA issue is updated:

![](docs/images/JIRA_Comments.jpg)

By taking advantages of Jenkins'
[fingerprint](https://wiki.jenkins.io/display/JENKINS/Fingerprint)
feature, when your other projects that depend on this project pick up a
build with a fix, those build numbers can also be recorded in JIRA.

This is quite handy when a bug is fixed in one of the libraries, yet the
submitter wants a fix in a different project. This happens often in my
work, where a bug is reported against JAX-WS but the fix is in JAXB. 

For curious mind, see [this thread for how this works behind the scene](http://jenkins.361315.n4.nabble.com/How-can-does-Hudson-Jira-integration-works-td374680.html).

#### Referencing JIRA Release version 

To reference JIRA Release versions in your build, you can pull these
releases directly from JIRA by adding the JIRA Release Version
Parameter. 

This can be useful for generating release notes, trigerring
parameterized build, etc.  
![](docs/images/version_parameters.png)

#### Generating Release Notes

You can also generate release notes to be used during your build. These
notes can be retrieved from an environment variable. See the [Maven Project Plugin](https://wiki.jenkins.io/display/JENKINS/Maven+Project+Plugin) for
the environment variables found within the POM.  
![](docs/images/release_notes.png)

After your build has run, you can also have the plugin mark a release as
resolved. This typically will be a release you specified in your Build
Parameters.  
![](docs/images/mark_as_resolved.png)

The plugin can also move certain issues matching a JQL query to a new
release version.  
![](docs/images/move_issues.png)

Sample usage of generated Release Notes:

![](docs/images/release_notes_config.png)

#### JIRA Authentication & Permissions required

**Note:** As a rule of thumb, **you should be always using a service
account** (instead of a personal account) to integrate Jenkins with
JIRA.

Make sure that the JIRA user used by Jenkins has enough permissions to
execute its actions. You can do that via JIRA Permission Helper tool.

-   For creating JIRA issues, the user has to be able to Create Issues
    in the specified project
-   If you additionally enter assignee or component field values, make
    sure that:
    -   both of the fields are assigned to the corresponding JIRA Screen
    -   the JIRA user is Assignable in the project
    -   the Jenkins JIRA user can Assign issues

##### JIRA Cloud

In Atlassian JIRA Cloud, it's not possible to create a user without an
email, so you need to create API token.

Then create a global Jenkins credential, where you put *Atlassian ID
email* as username and *API token* as password.

You can check if your API token works correctly by getting a correct
JSON issue response with this command (where TEST-1 is an example issue
in your project):

``` syntaxhighlighter-pre
$ curl -X GET -u <email>:<API token> -H "Content-Type: application/json"  https://<YourCloudInstanceName>.atlassian.net/rest/api/latest/issue/TEST-1
```

Also make sure that CAPTCHA is not triggered for your user as this will
prevent the API token to work - see [CAPTCHA section in Atlassian REST API documentation.](https://developer.atlassian.com/cloud/jira/platform/jira-rest-api-basic-authentication/)

  

#### System properties

| Property Name                                                   | Functionality Change                                                                                  |
|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| **-Dhudson.plugins.jira.JiraMailAddressResolver.disabled=true** | Use to disable resolving user email from JIRA usernames. Currently there is no option for this in UI. |

#### Related Resources

-   Check also the Marvelution [Jira Hudson Integration](http://www.marvelution.com/atlassian/jira-hudson-integration/)
    which provides a two-way solution Hudson-\>JIRA and JIRA-\>Hudson
-   [Hudson integration for JIRA](https://plugins.atlassian.com/plugin/details/11858) adds Hudson information to JIRA.
-   The [Subversion JIRA plugin](https://studio.plugins.atlassian.com/wiki/display/SVN/Subversion+JIRA+plugin) also allows recording of scm changes to JIRA issues (for other SCMs
    there are similar plugins)
-   For JIRA Workflow (Pipeline) plugin compatibility
    see [COMPATIBILITY.md](https://github.com/jenkinsci/jira-plugin/blob/master/COMPATIBILITY.md)

#### Releases

See [CHANGELOG.md](https://github.com/jenkinsci/jira-plugin/blob/master/CHANGELOG.md)

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
See [here](https://github.com/jenkinsci/pipeline-plugin/blob/master/COMPATIBILITY.md) for some information.

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
