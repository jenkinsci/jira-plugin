# AGENTS.md

Guidance for coding agents working in this repository. See `CONTRIBUTING.md` for the
human-oriented contribution workflow (pre-commit hooks, local Jenkins via docker-compose, the
Jira Cloud test instance) — this file focuses on what an agent needs to build, test, and stay
consistent with existing conventions.

## Project overview

A Jenkins plugin (`org.jenkins-ci.plugins:jira`) that integrates Jenkins with Atlassian Jira
(Cloud and Server), via `jira-rest-java-client` (pinned at `${jira-rest-client.version}` in
`pom.xml`, currently `6.0.2`). Packaging is `hpi`; the project uses the standard
`org.jenkins-ci.plugins:plugin` parent POM.

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
  see Testing instructions below).

## Dev environment tips

- Requires Java 17+ (the Jenkins baseline in `pom.xml`, currently `2.492`, needs it) and Maven.
- If `mise` is configured (see `mise.toml`), prefix commands with `mise exec --`.
- `jira-rest-java-client-core` is built on Jersey 2 / Apache HttpClient 4, provided via the
  Jenkins-bundled `jersey2-api` / `apache-httpcomponents-client-4-api` plugins rather than
  bundled directly (see the exclusions block around the `jira-rest-java-client-*` dependencies
  in `pom.xml`). Check any new HTTP-related dependency against this before assuming it'll just
  work — prefer shaded/standalone artifacts (as done for `wiremock-standalone`) when there's a
  real risk of a Jersey/Jetty/Jackson version collision.
- `io.jenkins.plugins:jackson2-api` already provides Jackson (currently `2.18.3`) — don't add a
  competing direct Jackson dependency; check `mvn dependency:tree -Dincludes='com.fasterxml.jackson*'`
  if unsure.

## Setup commands

```sh
mvn clean install -DskipTests   # build the .hpi without running the test suite
mvn spotless:apply              # auto-fix formatting (run before every commit)
```

## Testing instructions

```sh
mvn clean test                                                        # full build + test suite
mvn test -Dtest=SomeTest                                              # single class
mvn test -Dtest=SomeTest#someMethod                                   # single method
mvn test -Dtest='JiraRestServiceWireMockTest,LiveJiraCloudE2ETest' -DfailIfNoTests=false
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
- `-Dtest=SomeClass` (or `#someMethod`) can make a passing `@WithJenkins` test fail with
  `class ... is missing its descriptor` — an extension-indexing artifact of filtering, not a real
  failure. Before treating that as a bug, confirm with a full `mvn test` (no `-Dtest`), which is
  what CI actually runs.
- Unit tests (most of `src/test/java/hudson/plugins/jira/`) mock `ExtendedJiraRestClient` and
  its sub-clients directly with Mockito 5 — no real HTTP.
- `src/test/java/hudson/plugins/jira/wiremock/` is a real-wire-format integration suite:
  `AbstractJiraRestServiceContractTest` defines the test methods once; `JiraRestServiceWireMockTest`
  (offline, runs in every `mvn test`) and `LiveJiraCloudE2ETest` (opt-in, env-var-gated, real Jira
  Cloud) each implement a handful of `given*`/`prepare*`/`assert*` hooks to supply the backend.
  Adding coverage for another `JiraRestService` method means adding one test method to the
  abstract class and implementing its hooks in both subclasses — don't duplicate test bodies
  between the two subclasses.
  - WireMock fixture bodies are checked against Atlassian's Jira Cloud OpenAPI spec at test time via
    `OpenApiSpecConformance` (`com.atlassian.oai:openapi-request-validator-core`). If you add a
    new fixture, call `OpenApiSpecConformance.assertConformsToSpec(...)` on it before stubbing.
    The spec is a trimmed copy checked in under `src/test/resources/hudson/plugins/jira/wiremock/`,
    not a download, so this suite runs offline. If your fixture needs a path the copy doesn't carry,
    add it to `KEPT_PATHS` in `tools/trim-jira-openapi-spec.mjs` and re-run that script — the test
    tells you so rather than validating against nothing.
  - Do **not** add `com.atlassian.oai:swagger-request-validator-wiremock*` — those pull in
    `com.github.tomakehurst:wiremock-jre8` (WireMock 2.x), which collides with this project's
    `org.wiremock:wiremock-standalone` (WireMock 3.x) under the same package namespace.
  - Full details, including the exact live-e2e-test command, are in `CONTRIBUTING.md`.
- Add or update tests for the code you change, even if nobody asked. **A test only proves what it
  actually checks** — two easy ways to end up with a test that looks meaningful but isn't:
  - A WireMock stub returns the same canned response no matter what request body was sent, and
    `wireMock.verify(putRequestedFor(...))` alone only proves *a* call was made to that URL — not
    that it carried the right data. If the behavior under test is "did we send X," assert on the
    request body too (`.withRequestBody(matchingJsonPath(...))`), the way
    `JiraRestServiceWireMockTest.assertVersionReleased` does.
  - A Mockito stub set up with loose matchers (`any()`, `anyString()`) plus a fixed return value,
    asserted only via the method's boolean/`Result` outcome, can't catch a bug that passes the
    wrong project key, assignee, jql, etc. Capture and assert on the real arguments with
    `ArgumentCaptor` (or exact-value matchers) instead — see `JiraCreateIssueNotifierTest`/
    `JiraIssueUpdateBuilderTest` for the pattern.
- Fix any test errors until the whole suite is green before considering a change done.

## Code style

Enforced by the build, not optional:

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
  fairly small, consistent surface area (see Project overview above); check for an existing
  equivalent before adding a new helper.
- **Check `docs/adr/` before "fixing" code that looks wrong.** Architecture decisions are recorded
  there in MADR format (`docs/adr/README.md` has the index and template) - some of this code looks
  like a plain bug and is not. If you make a decision worth keeping, or reject a reasonable alternative for a
  non-obvious reason, add a record.
- New `catch` blocks around Jira REST calls should re-interrupt on `InterruptedException` and log
  via a deferred `Supplier`, not eager string concatenation (see SonarCloud section below for why):
  ```java
  } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // ... log + throw
  } catch (RestClientException | ExecutionException | TimeoutException e) {
      // ... log + throw
  }
  ```
  logged with `LOGGER.log(WARNING, e, () -> "message: " + e.getMessage())`.

## SonarCloud quality gate (PR-blocking)

Every PR runs a SonarCloud analysis (`.github/workflows/sonarcloud.yml`, or the fork-build/
fork-analysis pair for PRs from forks) that gates on the *diff*, not the whole file — pre-existing
issues elsewhere in a file you touch don't count, but anything you add does. Two conditions bite
most often:

- **`new_coverage` ≥ 80%.** Only lines under `src/main` count, and only if a test that runs in
  the default `mvn test` actually exercises them. `LiveJiraCloudE2ETest` doesn't count towards
  this — it's env-var-gated and never runs in CI, so new `JiraRestService` methods added only for
  its sake still need a `JiraRestServiceWireMockTest` (or other offline) test to be covered here.
- **`new_reliability_rating` = A (no new Bugs).** The most common trigger is copying an existing
  `JiraRestService` catch block as a template — see the Code style section above for the pattern
  to use instead.

Check gate status before assuming a PR is done: `gh pr checks <PR#>` shows pass/fail per check;
for the actual failing conditions, use the SonarQube MCP tools
(`mcp__sonarqube__get_project_quality_gate_status` with `pullRequest: "<PR#>"`, then
`mcp__sonarqube__search_sonar_issues_in_projects` to find the specific issues) or open the
`sonarcloud.io/dashboard?id=jenkinsci_jira-plugin&pullRequest=<PR#>` link from the check.

## PR instructions

- Title format: Conventional Commits — `<type>(<scope>): <subject>`, where type is one of
  `feat|fix|docs|style|refactor|test|chore|perf`.
- Always run `mvn spotless:apply` and `mvn clean test` before committing/opening a PR.
- **Every PR ships a documentation change too.** If the change is user-visible — new behaviour, a new
  or renamed step parameter, a changed default, a changed failure mode — update the relevant
  **Declarative Pipeline** example in `docs/features.md` in the same PR (every feature there has one),
  and `docs/configuration.md` or `docs/troubleshooting.md` when one of those is the right page. Write
  new and updated examples in Declarative form (`pipeline { agent any; stages { ... } }`) — don't add
  scripted (`node { ... }`) snippets. A step's example must stay runnable: if you add a required
  parameter, every example using that step needs it. Docs-only and pure-refactoring PRs are the
  exception; state that in the PR description rather than silently skipping.
- Write issue and PR descriptions, and comments, in **GitHub-flavored Markdown** — headings,
  fenced code blocks with a language tag, bullet/numbered lists, tables, and task lists
  (`- [ ]`) — rather than dense unformatted paragraphs.
- Confirm the SonarCloud quality gate (above) passes before considering a PR done.
- Commit message length max 79 chars.
