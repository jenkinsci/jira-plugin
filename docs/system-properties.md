# System Properties

There are some cases when you might want to change the plugin's behavior globally, by overriding [Jenkins system properties](https://www.jenkins.io/doc/book/managing/system-properties/). 

This plugin provides the following additional settings, that are not available via UI:

- `-Dhudson.plugins.jira.JiraMailAddressResolver.disabled=true`
    Use to disable resolving user email from Jira usernames. 
