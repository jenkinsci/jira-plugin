Changelog
===

### Newer versions

See [GitHub releases](https://github.com/jenkinsci/jira-plugin/releases)

### Unreleased

Release date:  _March 2, 2020_

* Changed all references of JIRA to Jira per [Atlassian branding updates](https://community.atlassian.com/t5/Feedback-Forum-articles/A-new-look-for-Atlassian/ba-p/638077)
* JiraCreateIssueNotifier: regard all statuses in "done" category as finished

### 3.0.11

Release date:  _Nov 21, 2019_

* Fix security issue

### 3.0.10

Release date:  _Sep 26, 2019_

* dependencies cleanup (remove dependency on org.codehaus.jackson:*)

### 3.0.9

Release date:  _Aug 21, 2019_

* JENKINS-59001 IssueSelectorStep should handle NullPointerException caused by having no configured sites
* JENKINS-57899 Jira site configuration lost after a restart

### 3.0.8

Release date:  _Jun 28, 2019_

* JENKINS-58244 JIRA Site at folder level doesn't show credentials for non-admin users
* JENKINS-57664 On Release of a Version recieve a Null Pointer exception

### 3.0.7

Release date:  _May 1, 2019_

* JENKINS-56951 JIRA plugin config incorrectly mixed with properties section
* JENKINS-52906 jira-plugin: FAILED TO EXPORT hudson.plugins.jira.JiraProjectProperty
* JENKINS-33222 Concurrent builds are blocking with jira-plugin
* JENKINS-19195 Add description field to Release JIRA version action

### 3.0.6

Release date:  _March 30, 2019_

* JENKINS-56810 Add current date in startDate when creating new version 
* JENKINS-56697 Simplify constructor, main URL is the only mandatory field 
* JENKINS-50643 Jira Plugin v2.4.2 leaks selectors resulting in Too Many Open Files

## 3.0.5

Release date:  _Jul 7, 2018_

* JENKINS-54469 Unable to set Thread Executor Size when configuring Jira Plugin 
* JENKINS-54131 "Failed to parse changelog" in JIRA plugin 3.0.3 
* JENKINS-54116 Jenkins Jira Plugin - Unable to add version
 
### 3.0.4

Release date:  _Oct 26, 2018_

* JENKINS-54144 Job fails as JiraSCMListsener/JiraSite potentially creating Executor with 0 threads

### 3.0.3

Release date:  _Oct 16, 2018_

* JENKINS-54042 Fix some misconfiguration between connect time and read timeout
* JENKINS-53808 Binary compatibility broken between JIRA plugin 3.0.2 and Artifactory plugin 2.16.2
* JENKINS-53642 Configuration for "Add timestamp to JIRA comments" not observed/remembered

### 3.0.2

Release date:  _Sep 25, 2018_

* SECURITY-1029 - CSRF vulnerability and missing permission checks in Jira Plugin allowed capturing credentials

### 3.0.1

Release date:  _Aug 22, 2018_

* JENKINS-54093 Jenkins Jira Plugin sets connection timeout to default = 10 after Jenkins restart
* JENKINS-53150 Remove Perforce Plugin dependency
* JENKINS-51164 JIRA plugin doesn't honor proxy excludes
* JENKINS-45789 Use Credentials Plugin for JIRA Global Configuration User

### 3.0.0

Release date:  _May 20, 2018_

* JENKINS-51312 Jira plugin core 2.60.1
* JENKINS-51310 Update JIRA plugin to use jackson2-api-plugin

### 2.5.2

Release date:  _May 4, 2018_

* JENKINS-49975 BasicHttpCache error with JIRA Plugin 2.5.1
* JENKINS-49231 jira-plugin 2.5.1 throws exception fails build
* JENKINS-48357 Binary Compatibility between JIRA Plugin and Apache HttpComponents Client 4.x API
* JENKINS-25829 Proxy configuration does not work

### 2.4.2

Release date:  _Aug 8, 2017_

* JENKINS-45992 Cannot validate JIRA site settings

### Version 2.4

Release date:  _Aug 3, 2017_

* JENKINS-44524 Support adding JIRA sites on folder

### 2.3.1

Release date:  _May 23, 2017_

* JENKINS-40571 JiraVersionCreatorBuilder fields missing in post-build form

### 2.3

Release date:  _Dec 19, 2016_

* JENKINS-39192 Support for updating custom fields 
* JENKINS-39091 Jira plugin fails to log in to Jira site after dependency updates 
* JENKINS-38142 Possibility to specify JIRA site in jenkins pipeline project 
* JENKINS-36726 Plugin comments on subsequent successful builds 
* JENKINS-35998 No issue update if at least one issue does not exists 
* JENKINS-34661 Bump LTS to 1.642.3 
* JENKINS-33996 Issue selector step improvement 
* JENKINS-33859 NPE when well-formed Jira Issue doesn't exist 
* JENKINS-32602 jira-plugin missing License 
* JENKINS-32492 Rename all dropdown labels to include JIRA: prefix 
* JENKINS-32491 Rename Workflow Plugin to Pipeline 
* JENKINS-31164 Issue Type must be selectable from fetched dropdown 
* JENKINS-24207 Use Perforce Jobs attached to changelist to track JIRA issues 
* JENKINS-19286 Support multiple fix versions

### 2.2.1

Release date:  _Mar 26, 2016_

* JENKINS-33293 (Jira) Updater throws NullPointerException for labels
* JENKINS-33211 NullPointerException in JiraVersionParameterValue.java

### 2.2

Release date:  _Feb 20, 2016_

* Split each SCM changes in paragraphs
* support release candidates (RCs) via Maven ComparableVersion
* Console logging improvements in various places
* JiraEnvironmentVariableBuilder support
* Support adding labels to updated issues
* (optionally) add scm entry change date and time to description in JIRA tickets
* JENKINS-32504 Make JiraEnvironmentVariableBuilder compatible with pipeline
* JENKINS-32276 JIRA Release Version Parameter is truncating list of Jira versions
* JENKINS-32170 Support CLI parameter submission for JiraIssueParameterDefinition
* JENKINS-32106 Issue type "UNKNOWN" in Release Notes
* JENKINS-31268 @Exported returns double XML value for getter
* JENKINS-31113 Configurable HTTP timeout parameter for JiraRestService

### 2.1

Release date:  _Now 18, 2015_

* Bumped Jenkins Core to LTS v. 1.609.3
* Added dependencies: mailer-plugin, matrix-plugin
* Removed dependencies: maven-plugin
* JENKINS-32949 Issue with JIRA plugins in JENKINS 
* JENKINS-31626 Expand JIRA Project Key variable in other build tasks 
* JENKINS-31349 Jira configuration - Validate Settings doesn't validate username/password 
* JENKINS-30829 JIRA Generate Release Notes needs default Environment Variable 
* JENKINS-30305 Allow CLI parameter submission for JiraVersionParameterDefinition 
* JENKINS-26701 Jira Plugin: I need sorting option in "JIRA Release Version Parameter". Is it possible? 
* JENKINS-25828 JIRA version name/value not picked up by remote API 
* JENKINS-17156 If Updater fails to update due to missing permission, it crashes and never flushes the comment queue 
* JENKINS-13436 Message logged by JIRA plugin should mention that the message relates to a Jenkins job. 
* JENKINS-12578 Jira issue parameter value is not exposed via remote API 
* JENKINS-3709 Jira login information should be scrambled in the configuration file

### 2.0.3

Release date:  _Oct 26, 2015_

* JENKINS-30682 Ticket creation fails when no components in JIRA project exist / JIRA rejects empty component list
* JENKINS-30408 JIRA REST API requests lead to 404 (not found) 
* JENKINS-30333 Thread Leak due to use of deprecated JiraSize.createSession

### 2.0.1

Release date:  _Sep 10, 2015_

* JENKINS-30242 Update to 1.13.2 breaks global configuration submission when jira-plugin installed

### 2.0

Release date:  _Sep 2, 2015_

* switch from JIRA RPC SOAP to JIRA REST API communication - the former has been deprecated and dropped since JIRA v.7.0.
* JENKINS-23257 Non well-formed response from JIRA error is hard to diagnose 
* JENKINS-18227 Add support for Atlassian OnDemand JIRA 
* JENKINS-18166 Add support for JIRA REST API - JIRA SOAP API will be removed in JIRA 7 
* JENKINS-10223 This is a valid URL but it doesn't look like JIRA

### 1.41

Release date:  _Jun 10, 2015_

* JENKINS-22628 Change comments/description to use String.format() instead. 
* JENKINS-21776 Jenkins jira comment text typo 
* JENKINS-20528 Unable to link to Jira 
* JENKINS-9549 JIRA plugin does not create links to JIRA repository 
* JENKINS-1904 Can't get issue links generated with out user/password

### Version 1.39

Release date:  _Oct 6, 2013_

* Ability only to comment issue without processing of workflow (pull #38)

### 1.38

Release date:  _Aug 23, 2013_

* Post build step to create new JIRA version (pull #30)

### 1.37

Release date:  _Jun 21, 2013_

* Error with empty alternative url issue #18229

### 1.35

Release date:  _Jul 29, 2012_

* Prevents multiple comments on one issue for matrix builds. (PR #13)

### 1.34

Release date:  _Jun 11, 2012_

* Fix NPE when Jenkins user does not have access to perform any workflow actions JENKINS-13998

### 1.33

Release date:  _Jun 1, 2012_

* Support workflow steps as build actions and/or post-build notifiers JENKINS-13652

### 1.32

Release date:  _May 15, 2012_

* Option to show archived versions.

### 1.31

Release date:  _May 1, 2012_

* Add JiraIssueMigrator - a post build action that will move issues to a new fixVersion based on a JQL query.
* Add Additional filtering of issues to be included in the release notes. Defaults to 'status in (Resolved, Closed)'

### 1.30

Release date:  _April 25, 2012_

* Add build parameter that providers a drop-down with JIRA release versions
* Add a build wrapper that will assemble release notes based on issues in the release version and store it in an environment variable
* JENKINS-123 Issue summary
* JENKINS-124 Another Issue summary
* JENKINS-321 Yet another issue summary
* Add a post-build action that will mark a version as released in JIRA

### 1.29

Release date:  _August 25, 2011_

* JENKINS-10817 Jira-plugin should add the overall build result to the issue's comment
* Include revisions also for non-subversion plugins; include revisions also if we don't have a repository browser
* Defined a new parameter type for parameterized builds that allow you to select a JIRA ticket (from the result of a JQL query)

### 1.28

Release date:  _Jun 15, 2011_

* Improve the form validation error check JENKINS-9625
* Supported security level of the comment JENKINS-1489

### 1.27

Release date:  _Feb 27, 2011_

* Updates for Jenkins

### 1.26

Release date:  _Jan 14, 2011_

* JENKINS-2508 : JIRA plugin not updating JIRA when perforce plugin used.

### 1.25

* JENKINS-6758: Failed to save system settings with JIRA Plugin.

### 1.24

* JENKINS-6462: Version 1.355 of Hudson and Jira Plugin 1.21: Images in Jira comments are not showing up.

### Version 1.23

* JENKINS-6264, JENKINS-6282: IndexOutOfBoundsException when no issue pattern is configured (default pattern wasn't used)
* JENKINS-6381: configured patterned wasn't used for changelog annotation. Default pattern was always used for that.
* improved default pattern to not match commit messages with dots in the number part (like 'projectname-1.2.3'). These messages are e.g. used by the Maven release plugin

### Version 1.22

* JENKINS-6043: Issue pattern can be configurable
* JENKINS-6225: option to update jira issue whatever the build result is (even if failed)

### 1.21

* JENKINS-5989: option to record scm changes in jira.
* JENKINS-6007: Added French localization.

### 1.20

* Added Japanese localization (JENKINS-5788)

### 1.19

* Fix: Prevent carrying forward invalid issue ids forever

### 1.18

* Case insensitive matching of JIRA ids also in the 'recent changes' view (JENKINS-4132)
* fetch missing details for JIRA issues - i.e. completes issue title tooltip in 'recent changes' view (JENKINS-5252)
* prevent build FAILURE if JIRA site is not available (JENKINS-3046)

### 1.17

* Fixed an ArrayIndexOutOfBoundsException when JIRA issues contain '$' in the name.
* Support underscore in project names (JENKINS-4092)
* Support digits in project names (JENKINS-729)
* Case insensitive matching of JIRA ids (JENKINS-4132)
* Don't strip JIRA id from posted comment
* German translation

### 1.15

Release date:  _Aug 22, 2008_

* Update JIRA if the build is UNSTABLE or better.  Previously only updated if the build was stable.
* Include relevant SCM comment in the JIRA comment which should make JIRA ticket history more meaningful.

### 1.13

Release date:  _Auh 5, 2008_

* Fixed a performance issue in a large enterprise deployment of JIRA (JENKINS-1703)

### Version 1.12

* A typo in the commit message shouldn't break builds (JENKINS-1593)
* Postpone JIRA updates until a successful build is obtained (JENKINS-506)

### Version 1.11

* Added more logging and debug flag to examine issues that people are reporting (report)

### Version 1.10

* Wiki-style notation option wasn't persisted (JENKINS-977)
* Fixed a packaging problem (JENKINS-1127)

### 1.9

* Fixed NPE when failed to talk to JIRA (JENKINS-1097)

### 1.8

* Be more graceful in dealing with URLs (JENKINS-896)
* URLs need to be escaped (JENKINS-943)

### 1.7

* Fixed NPE when username/password is not set (JENKINS-828)

### 1.6

* Relaxed the JIRA project key regexp a little bit to allow numbers (JENKINS-729)

### 1.5

* Issue hyperlinking is now smart enough not to be confused by strings that look like JIRA issue that actually aren't.

### 1.4

* Fixed a bug that prevented tooltips for JIRA issues from being displayed JENKINS-694
