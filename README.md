Jenkins JIRA Plugin
===================

[![Join the chat at https://gitter.im/jenkinsci/jira-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jenkinsci/jira-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/jira-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/job/jira-plugin/)
[![Build Status](https://travis-ci.org/jenkinsci/jira-plugin.svg?branch=master)](https://travis-ci.org/jenkinsci/jira-plugin)
[![Coverage Status](https://coveralls.io/repos/jenkinsci/jira-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/jenkinsci/jira-plugin?branch=master)
[![Issue Stats](http://issuestats.com/github/jenkinsci/jira-plugin/badge/pr?style=flat)](http://issuestats.com/github/jenkinsci/jira-plugin)


See [Wiki page](https://wiki.jenkins-ci.org/display/JENKINS/JIRA+Plugin) for more information.

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



Contributing to the Plugin
==========================

Plugin source code is hosted on [GitHub](https://github.com/jenkinsci/jira-plugin).
New feature proposals and bug fix proposals should be submitted as
[GitHub pull requests](https://help.github.com/articles/creating-a-pull-request).
Fork the repository on GitHub, prepare your change on your forked
copy, and submit a pull request (see [here](https://github.com/jenkinsci/jira-plugin/pulls) for open pull requests). Your pull request will be evaluated
by the [Cloudbees Jenkins job](https://jenkins.ci.cloudbees.com/job/plugins/job/jira-plugin/)
and you should receive e-mail with the results of the evaluation.

Before submitting your change, please assure that you've added a test
which verifies your change.  There have been many developers involved
in the git plugin and there are many, many users who depend on the
git-plugin.  Tests help us assure that we're delivering a reliable
plugin, and that we've communicated our intent to other developers in
a way that they can detect when they run tests.

Code coverage reporting is available as a maven target and is actively
monitored.  Please try your best to improve code coverage with tests
when you submit.

Before submitting your change, please review the findbugs output to
assure that you haven't introduced new findbugs warnings.
