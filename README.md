# Jenkins Jira Plugin

[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.479.3-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/jira.svg?color=blue)](https://stats.jenkins.io/pluginversions/jira.html)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/jira-plugin.svg?label=Release)](https://github.com/jenkinsci/jira-plugin/releases/latest)
[![Jenkins CI](https://ci.jenkins.io/buildStatus/icon?job=Plugins/jira-plugin/master)](https://ci.jenkins.io/job/Plugins/job/jira-plugin/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/jira-plugin.svg)](https://github.com/jenkinsci/jira-plugin/graphs/contributors)

See user documentation at [https://jenkinsci.github.io/jira-plugin/](https://jenkinsci.github.io/jira-plugin/)

## i18n


[![da translation](https://img.shields.io/badge/dynamic/json?color=blue&label=da&style=flat&logo=crowdin&query=%24.progress.0.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)
[![de translation](https://img.shields.io/badge/dynamic/json?color=blue&label=de&style=flat&logo=crowdin&query=%24.progress.1.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)
[![en translation](https://img.shields.io/badge/dynamic/json?color=blue&label=en&style=flat&logo=crowdin&query=%24.progress.2.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)
[![es translation](https://img.shields.io/badge/dynamic/json?color=blue&label=es&style=flat&logo=crowdin&query=%24.progress.3.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)
[![fr translation](https://img.shields.io/badge/dynamic/json?color=blue&label=fr&style=flat&logo=crowdin&query=%24.progress.4.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)
[![it translation](https://img.shields.io/badge/dynamic/json?color=blue&label=it&style=flat&logo=crowdin&query=%24.progress.5.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)
[![ja translation](https://img.shields.io/badge/dynamic/json?color=blue&label=ja&style=flat&logo=crowdin&query=%24.progress.6.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)
[![pl translation](https://img.shields.io/badge/dynamic/json?color=blue&label=pl&style=flat&logo=crowdin&query=%24.progress.7.data.translationProgress&url=https%3A%2F%2Fbadges.awesome-crowdin.com%2Fstats-200016380-35.json)](https://jenkins.crowdin.com/jira-plugin)

This plugin uses [CrowdIn platform](https://jenkins.crowdin.com/jira-plugin) as the frontend to manage translations. If you would like to contribute translation of this plugin in your language,  you're most welcome! For details, see [jenkins.io CrowdIn introduction](https://www.jenkins.io/doc/developer/crowdin/translating-plugins/).

## Contributing

There have been many developers involved in the development of this plugin and there are many downstream users who depend on it. Tests help us assure that we're delivering a reliable plugin and that we've communicated our intent to other developers in a way that they can detect when they run tests.

- each change should be covered by appropriate unit tests
- in case it is not testable via a unit test, it should be tested against a real Jira instance - possibly both Jira Server and Jira Cloud. There is a [Jira Cloud test instance](https://jenkins-jira-plugin.atlassian.net/) that we are using for testing the plugin releases - let us know in the Pull Request in case you need access for testing
 
