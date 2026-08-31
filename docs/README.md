# Jenkins Jira plugin

Integrates Jenkins with [Atlassian Jira](http://www.atlassian.com/software/jira/) — link builds
back to issues, comment on issues from a build, and manage releases — for both **Jira Cloud** and
**Jira Server/Data Center**, in freestyle jobs and Pipelines.

## Getting started

```mermaid
flowchart LR
    A[Install the plugin] --> B[Create a Jira API token]
    B --> C[Add a Jenkins credential]
    C --> D[Configure your Jira site]
    D --> E[Validate the connection]
```

1. Install **Jira Integration** from *Manage Jenkins → Plugins*.
2. Skim [Features](features.md) to see what the plugin can do — every feature there comes with a
   ready-to-copy Pipeline snippet.
3. Follow [Configuration](configuration.md) to connect Jenkins to your Jira instance — it also
   covers System Properties (settings not exposed in the UI).

## Something doesn't work?

Check the [Troubleshooting](troubleshooting.md) page and
[existing GitHub issues](https://github.com/jenkinsci/jira-plugin/issues) first.

Still stuck, or found a bug? [Open an issue](https://github.com/jenkinsci/jira-plugin/issues/new).
Open Source Software relies on contributions from fellow developers — see
[Contributing](CONTRIBUTING.md) to send a pull request, or consider sponsoring a maintainer (see the
"Sponsor this project" section on the [GitHub repo](https://github.com/jenkinsci/jira-plugin)) if
you're not a developer.
