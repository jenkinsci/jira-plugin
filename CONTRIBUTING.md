### Contributing to the Plugin

New feature proposals and bug fix proposals should be submitted as [Pull Requests](https://help.github.com/articles/creating-a-pull-request).

Before submitting your change please note that:
* tests should be added for any changed code - the coverage is automatically checked after submitting the Pull Request
* the code formatting should follow the defined standard - see [Code Style](#code-style)
* you use findbugs to see if you haven't introduced any new warnings
* when adding new features please make sure that they support Jenkins Pipeline Plugin - see [COMPATIBILITY.md](https://github.com/jenkinsci/pipeline-plugin/blob/master/COMPATIBILITY.md) for more information

#### Testing your changes

There have been many developers involved in the development of this plugin and there are many downstream users who depend on it.
Tests help us assure that we're delivering a reliable plugin and that we've communicated our intent to other developers in a way that they can detect when they run tests.

Each change should be covered by appropriate unit tests.
In case it is not testable via a unit test, it should be tested against a real Jira instance - possibly both Jira Server and Jira Cloud.

There is a [Jira Cloud test instance](https://jenkins-jira-plugin.atlassian.net/) that we are using for testing the plugin releases - let us know in the Pull Request in case you need access for testing.

#### Code Style

We try to improve the code quality by conforming to
[Google Java styleguide](https://google.github.io/styleguide/javaguide.html), that is defined in
[google_checks.xml](https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/main/resources/google_checks.xml).
If you are using an IDE, like IntelliJ IDEA, please:

- install [Google Java Format plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format)

or

- install the [Checkstyle plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)
- go to **Preferences > Editor > Code Style > Java**, select **Scheme** to *Project*
- click the Cog icon and import Checkstyle configuration from [google_checks.xml](https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/main/resources/google_checks.xml)

Currently the formatting is not automatically checked during the build. However, in the effort to
improve the quality of the code,  maintainers might ask for proper formatting during the review
process, so it is better to have it in place sooner than later.
