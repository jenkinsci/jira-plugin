# Jenkins Jira plugin features

Once the plugin is [configured](configuration.md), here's what it can do.

## Link builds back to Jira issues

The plugin can update Jira issues with a back pointer to the Jenkins build page, so the reporter
and watchers can quickly find the build that picked up the fix.

![plugin-configuration](images/Plugin_Configuration.png)

Once a Jira site is configured, the plugin also automatically hyperlinks matching issue keys in the
build changelog. If you've additionally supplied Jira credentials, those links get tooltips with the
issue summary.

![example-annotated-changelog](images/example_annotated_changelog.png)

## Comment on issues from a build

To post comments back to Jira, supply a valid Jira user/API token — this is optional, since you
won't always have write access to every Jira project your build touches (e.g. a Jenkins build for
an upstream OSS dependency). If comments should only be visible to a specific Jira group (e.g.
*Software Development*), set the group name too.

![jira-comments](images/Jira_Comments.jpg)

This also works across projects: using Jenkins' [fingerprint](https://wiki.jenkins.io/display/JENKINS/Fingerprint)
feature, when a downstream build picks up a fix, that build number is recorded on the original Jira
issue too — useful when a bug is reported against one component but fixed in a dependency. See
[this thread](http://jenkins.361315.n4.nabble.com/How-can-does-Hudson-Jira-integration-works-td374680.html)
for how it works under the hood.

## Manage releases

Pull a Jira Release Version directly into your build with the Jira Release Version Parameter — handy
for generating release notes or triggering a parameterized build.

![version-parameters](images/version_parameters.png)

Generate release notes during the build and read them back from an environment variable. See the
[Maven Project Plugin](https://wiki.jenkins.io/display/JENKINS/Maven+Project+Plugin) docs for the
environment variables available from the POM.

![release-notes](images/release_notes.png)

After the build runs, the plugin can also:

- mark a release as resolved (typically one you specified as a Build Parameter)
  ![marking-as-resolved](images/mark_as_resolved.png)
- move issues matching a JQL query into a new release version
  ![moving-issues](images/move_issues.png)

Sample usage of generated release notes:
![release-notes-config](images/release_notes_config.png)

## Pipeline support

Most features above are also available as Pipeline steps — see [Usage Examples](usage-examples.md).
A few (mainly notifiers) aren't yet supported in Pipeline; use the `catchError` step and call the
Jira step manually as a workaround.

## Before you rely on any of this

The Jira user Jenkins connects as needs enough permissions for whatever it's asked to do — see
[Required Jira permissions](configuration.md#required-jira-permissions) in Configuration.
