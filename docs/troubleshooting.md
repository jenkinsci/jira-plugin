# Troubleshooting

Connection or authentication problems are most often one of the cases already covered in
[Configuration](configuration.md) — the Jira Cloud URL format, CAPTCHA, and Bearer Authentication
warnings. Check those first.

## Turning on debug logging

To see more detail on what the plugin is doing, add a
[custom Log Recorder](https://www.jenkins.io/doc/book/system-administration/viewing-logs/#logs-in-jenkins)
for logger `hudson.plugins.jira` at level `FINE`.

If that's not enough detail and you need to see the raw HTTP request/response actually sent to Jira (method, URL, headers), add these two loggers to the same Log Recorder at `ALL`/finest level as well:

- `org.apache.http.wire`
- `org.apache.http.headers`

These come from the Apache HttpComponents library the plugin's HTTP client is built on, and log every outgoing request line and header, and every response line and header, for each call. Handy for telling apart a routing/URL problem from an authentication problem (e.g. a `401` coming back with no `Authorization` header actually sent), but keep in mind the Authorization header value is only Base64-encoded, not encrypted, so treat that log output as containing your credentials in the clear.

## Jenkins ↔ Jira SSL connectivity problems

If you see a stack trace like this:

```stacktrace
Caused by: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

the JRE/JDK running your Jenkins controller (or agents, if the connection is made from there)
doesn't trust the CA chain Jira's certificate was issued from. Verify this with the
[SSLPoke.java class](https://gist.github.com/warden/e4ef13ea60f24d458405613be4ddbc51):

```sh
wget -O SSLPoke.java https://gist.githubusercontent.com/warden/e4ef13ea60f24d458405613be4ddbc51/raw/7f258a30be4ddea7b67239b40ae305f6a2e98e0a/SSLPoke.java
javac SSLPoke.java
java SSLPoke jira.domain.com 443
# Successfully connected
```

Run `javac`/`java` from the same JRE/JDK Jenkins itself uses, then import the missing CA
certificate into that JRE/JDK's trust store.

References:

- [Jenkins fails with PKIX Path building error](https://stackoverflow.com/questions/52842214/jenkins-fails-with-pkix-path-building-error)
- [PKIX path building failed error message](https://support.cloudbees.com/hc/en-us/articles/217078498-PKIX-path-building-failed-error-message)

## Still stuck?

Check [existing GitHub issues](https://github.com/jenkinsci/jira-plugin/issues), or
[open a new one](https://github.com/jenkinsci/jira-plugin/issues/new) with your debug log attached.
