# Common issues

## Adding a custom Log Recorder

To help debug any issues with this plugin, it's useful to look at more detailed logs. To enable them, create a [custom Log Recorder](https://www.jenkins.io/doc/book/system-administration/viewing-logs/#logs-in-jenkins) for Logger `hudson.plugins.jira` and Log Level `FINE`. 

## JQL steps fail with `410 Gone` against `api.atlassian.com` Cloud sites

If your Jira site URL is configured using the API-gateway form recommended to avoid CAPTCHA
errors — `https://api.atlassian.com/ex/jira/{cloudId}/` — and any JQL-based step (`jiraSearch`,
issue selectors, "Move issues matching JQL to a version", etc.) fails with:

```stacktrace
RestClientException{statusCode=Optional.of(410), errorCollections=[ErrorCollection{status=410, errors={}, errorMessages=[The requested API has been removed. Please migrate to the /rest/api/3/search/jql API. ...]}]}
```

this is fixed as of this release: the plugin now recognizes `api.atlassian.com` as a Cloud
domain explicitly, instead of relying on `jira-rest-java-client`'s own detection (which only
knew about `*.atlassian.net` and `*.jira.com`, so it routed the search to the removed
Data Center-only `/search` endpoint). Upgrade the plugin to pick up the fix; no configuration
change is needed.

## Jenkins <---> Jira SSL connectivity problems

If you encounter stacktrace like this:

```stacktrace
Caused by: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

make sure the JRE/JDK that Jenkins master is running (or the Jenkins slaves are running) contain the valid CA chain certificates that Jira is running with.
You can test it using this [SSLPoke.java class](https://gist.github.com/warden/e4ef13ea60f24d458405613be4ddbc51):

```sh
$ wget -O SSLPoke.java https://gist.githubusercontent.com/warden/e4ef13ea60f24d458405613be4ddbc51/raw/7f258a30be4ddea7b67239b40ae305f6a2e98e0a/SSLPoke.java

$ /usr/java/jdk1.8.0_131/bin/javac SSLPoke.java

$ /usr/java/jdk1.8.0_131/jre/bin/java SSLPoke jira.domain.com 443
Successfully connected
```

References:

- [Jenkins fails with PKIX Path building error](https://stackoverflow.com/questions/52842214/jenkins-fails-with-pkix-path-building-error)
- [PKIX path building failed error message
](https://support.cloudbees.com/hc/en-us/articles/217078498-PKIX-path-building-failed-error-message)
