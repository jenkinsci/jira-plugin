# Common issues

## Adding a custom Log Recorder

To help debug any issues with this plugin, it's useful to look at more detailed logs. To enable them, create a [custom Log Recorder](https://www.jenkins.io/doc/book/system-administration/viewing-logs/#logs-in-jenkins) for Logger `hudson.plugins.jira` and Log Level `FINE`. 

If that's not enough detail and you need to see the raw HTTP request/response actually sent to Jira (method, URL, headers), add these two loggers to the same Log Recorder at `ALL`/finest level as well:

- `org.apache.http.wire`
- `org.apache.http.headers`

These come from the Apache HttpComponents library the plugin's HTTP client is built on, and log every outgoing request line and header, and every response line and header, for each call. Handy for telling apart a routing/URL problem from an authentication problem (e.g. a `401` coming back with no `Authorization` header actually sent), but keep in mind the Authorization header value is only Base64-encoded, not encrypted, so treat that log output as containing your credentials in the clear.

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
