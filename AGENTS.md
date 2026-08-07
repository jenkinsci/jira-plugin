# AGENTS.md

Guidance for coding agents working in this repository. See `CONTRIBUTING.md` for the
human-oriented contribution workflow (pre-commit hooks, local Jenkins via docker-compose, the
Jira Cloud test instance) — this file focuses on what an agent needs to build, test, and stay
consistent with existing conventions.

## What this is

A Jenkins plugin (`org.jenkins-ci.plugins:jira`) that integrates Jenkins with Atlassian Jira
(Cloud and Server), via `jira-rest-java-client` (pinned at `${jira-rest-client.version}` in
`pom.xml`, currently `6.0.2`). Packaging is `hpi`; the project uses the standard
`org.jenkins-ci.plugins:plugin` parent POM.

## Build & test

Requires Java 17+ (the Jenkins baseline in `pom.xml`, currently `2.492`, needs it) and Maven.
If `mise` is configured (see `mise.toml`), prefix commands with `mise exec --`.

```sh
mvn clean test                                    # full build + test suite
mvn test -Dtest=SomeTest                          # single class
mvn test -Dtest=SomeTest#someMethod                # single method
mvn test -Dtest='JiraRestServiceWireMockTest,LiveJiraCloudE2ETest' -DfailIfNoTests=false
mvn spotless:apply                                # auto-fix formatting (see below)
```

- Tests use `jenkins-test-harness` (`@WithJenkins` + a `JenkinsRule` parameter), which boots a
  real embedded Jenkins instance per test — this is normal, not a bug, and it's why individual
  test runs take a few seconds each.
- `surefire` is configured with `reuseForks=false`; don't remove that without checking why (see
  the comment above it in `pom.xml` — mock serialization issues otherwise).
- `src/test/resources/logging.properties` quiets jenkins-test-harness's own INFO-level boot
  logging so real test failures aren't buried in console noise. If a test failure seems to be
  missing its actual stack trace, check `target/surefire-reports/` directly — it always has the
  full detail regardless of console verbosity.

## Code style — enforced by the build, not optional

- **Formatting**: Spotless + Palantir Java Format, `mvn spotless:apply` before committing.
  `spotless.check.skip=false` in `pom.xml`, so a normal build checks it. Don't hand-format
  imports — Spotless reorders them; just run `spotless:apply` after editing.
- **JUnit 5 only**: an enforcer rule bans `junit.**`/`org.junit.**` imports except
  `org.junit.jupiter.**`/`org.junit.platform.**`. Never add a JUnit 4 import.
- **Commons Lang 3 only**: `org.apache.commons.lang.**` (Commons Lang 2) imports are banned by
  an enforcer rule; use `org.apache.commons.lang3.**`.
- **Stapler**: `org.kohsuke.stapler.StaplerRequest`/`StaplerResponse` (v1) and `javax.servlet.**`
  are banned; use `StaplerRequest2`/`StaplerResponse2` and `jakarta.servlet.**`.
- Prefer reusing existing utilities/patterns over introducing new ones — this codebase has a
  fairly small, consistent surface area (see below); check for an existing equivalent before
  adding a new helper.

## Architecture map

- `hudson.plugins.jira.JiraSite` — per-Jenkins-config representation of a Jira instance (URL,
  credentials, timeouts). `getSession(Item)` resolves credentials and hands back a `JiraSession`.
- `hudson.plugins.jira.JiraSession` — thin facade over `JiraRestService` (`public final
  JiraRestService service` field) with some business logic on top (status-id caching,
  fixVersion regex replace, etc.). This is what build steps/notifiers actually call through.
- `hudson.plugins.jira.JiraRestService` — wraps the Atlassian `ExtendedJiraRestClient` for most
  operations, plus a few raw Apache `fluent.Request` HTTP calls (`getVersions`, `getComponents`)
  for endpoints the Atlassian client doesn't expose. `BASE_API_PATH = "rest/api/2"` is used by
  those raw calls and by explicitly-built URIs (`addComment`, `releaseVersion`) — but the
  Atlassian client's *own* internal calls (`getIssueClient()`, `getMetadataClient()`, etc.)
  always route through `/rest/api/latest` regardless of that constant. Don't assume one base
  path applies everywhere; check which call path you're in.
- `hudson.plugins.jira.extension.*` — this plugin's own extensions on top of
  `jira-rest-java-client` for operations it doesn't support natively (extended version/
  mypermissions REST clients). `JiraSite.ExtendedAsynchronousJiraRestClientFactory` (nested in
  `JiraSite.java`) is the actual `JiraRestClientFactory` in use.
- `hudson.plugins.jira.JiraSessionFactory` — builds the `ExtendedJiraRestClient` +
  `JiraRestService` pair from a `JiraSite`, `URI`, and credentials; picks Basic vs. Bearer auth.
- Build steps / notifiers (`JiraIssueUpdater`, `JiraCreateIssueNotifier`,
  `JiraVersionCreatorBuilder`, `JiraReleaseVersionUpdateBuilder`, etc.) all go through
  `JiraSite.getSession(item)` — don't bypass it to call `JiraRestService` directly from plugin
  code (test code doing so intentionally, to validate the wire format, is the one exception —
  see below).

## Testing conventions

- Unit tests (most of `src/test/java/hudson/plugins/jira/`) mock `ExtendedJiraRestClient` and
  its sub-clients directly with Mockito 5 — no real HTTP.
- `src/test/java/hudson/plugins/jira/wiremock/` is a real-wire-format integration suite:
  `AbstractJiraRestServiceContractTest` defines the test methods once; `JiraRestServiceWireMockTest`
  (offline, runs in every `mvn test`) and `LiveJiraCloudE2ETest` (opt-in, env-var-gated, real Jira
  Cloud) each implement a handful of `given*`/`prepare*`/`assert*` hooks to supply the backend.
  Adding coverage for another `JiraRestService` method means adding one test method to the
  abstract class and implementing its hooks in both subclasses — don't duplicate test bodies
  between the two subclasses.
  - WireMock fixture bodies are checked against Atlassian's real OpenAPI spec at test time via
    `OpenApiSpecConformance` (`com.atlassian.oai:swagger-request-validator-core`). If you add a
    new fixture, call `OpenApiSpecConformance.assertConformsToSpec(...)` on it before stubbing.
  - Do **not** add `com.atlassian.oai:swagger-request-validator-wiremock*` — those pull in
    `com.github.tomakehurst:wiremock-jre8` (WireMock 2.x), which collides with this project's
    `org.wiremock:wiremock-standalone` (WireMock 3.x) under the same package namespace.
  - Full details, including the exact live-e2e-test command, are in `CONTRIBUTING.md`.

## Dependency/classpath gotchas worth knowing before adding a test dependency

- `jira-rest-java-client-core` is built on Jersey 2 / Apache HttpClient 4, provided via the
  Jenkins-bundled `jersey2-api` / `apache-httpcomponents-client-4-api` plugins rather than
  bundled directly (see the exclusions block around the `jira-rest-java-client-*` dependencies
  in `pom.xml`). Any new HTTP-related test dependency should be checked against this before
  assuming it'll just work — prefer shaded/standalone artifacts (as done for
  `wiremock-standalone`) when there's a real risk of a Jersey/Jetty/Jackson version collision.
- `io.jenkins.plugins:jackson2-api` already provides Jackson (currently `2.18.3`) — don't add a
  competing direct Jackson dependency; check `mvn dependency:tree -Dincludes='com.fasterxml.jackson*'`
  if unsure.
