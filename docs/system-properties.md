# System Properties

Some plugin behaviour is only changeable globally, by overriding
[Jenkins system properties](https://www.jenkins.io/doc/book/managing/system-properties/) — for
settings that aren't exposed in the UI.

- `-Dhudson.plugins.jira.JiraMailAddressResolver.disabled=true`

  Disables resolving a user's email address from their Jira username.
