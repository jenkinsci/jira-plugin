# Contribution guidelines

General rules:

- check the [general Jenkins development guide](https://www.jenkins.io/doc/developer/book/)
- make sure to provide tests
- when adding new fields, make sure to [include backward-compatibility](https://www.jenkins.io/doc/developer/persistence/backward-compatibility/) and tests for that
- mark the Pull Request as _draft_ initially, to make sure all the checks pass correctly, then convert it to non-draft.

## Setting up your environment

### Install pre-commit hooks

The [pre-commit](https://pre-commit.com/#install) hooks run various checks to make sure no unwanted files are committed and that the submitted change follows the code style and formatting rules:

```sh
brew install pre-commit && pre-commit install --install-hooks
```

## Notes for maintainers

### Local testing

Use [docker-compose](./docker-compose.yml) to run a local Jenkins instance with the plugin installed. The configuration includes local volumes for both: Jenkins and ssh-agent, so you can easily test the plugin in a clean environment.


### Atlassian sources import

To resolve [some binary compatibility issues](https://github.com/jenkinsci/jira-plugin/pull/140),
the sources from the artifact [com.atlassian.httpclient:atlassian-httpclient-plugin:0.23](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/)
has been imported in the project to have control over http(s) protocol transport layer.
The downloaded sources didn't have any license headers but based on the [pom](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/atlassian-httpclient-plugin-0.23.0.pom)
sources are Apache License (see pom in src/main/resources/atlassian-httpclient-plugin-0.23.0.pom)

### Testing

There is a [Jira Cloud](https://jenkins-jira-plugin.atlassian.net/) test instance that the changes can be tested against, official maintainers are admins that can grant access for testing to PR submitters on a need-to-have basis.

### Automated integration tests

`src/test/java/hudson/plugins/jira/wiremock/` contains an integration test suite that exercises
`JiraRestService`/`JiraSession` against a real HTTP request/response wire format instead of a
mocked REST client, so the real API contract is validated, not just the plugin's own code. The
test bodies are written once, in `AbstractJiraRestServiceContractTest`, and run against two
different backends by two thin subclasses:

- `JiraRestServiceWireMockTest` — a local [WireMock](https://wiremock.org/) server. Runs as part
  of the normal `mvn test`, fully offline (WireMock only binds loopback), no live Jira credentials
  needed.
- `LiveJiraCloudE2ETest` — a real Jira Cloud instance (e.g. the one linked above). Opt-in, skipped
  by default, for validating/refreshing the WireMock stub shapes when the Jira API contract
  changes.

Each subclass only implements a handful of `given*`/`prepare*`/`assert*` hooks (build the
`JiraSite`, make an issue/version exist, verify a side effect happened) — the WireMock subclass's
implementations register stubs, the live subclass's create/verify real data. A change to a test
method in `AbstractJiraRestServiceContractTest` automatically applies to both backends, so there's
nothing to keep in sync by hand.

WireMock's stub response bodies are derived from Atlassian's official
[Jira Cloud platform OpenAPI spec](https://developer.atlassian.com/cloud/jira/platform/swagger-v3.v3.json),
trimmed to the fields the bundled `jira-rest-java-client-core` version actually parses — each stub
has a comment pointing at the spec operation it came from, and there are no separate stub-mapping
files to manage (everything's inline in the test class). `OpenApiSpecConformance` enforces this
automatically: every fixture body is checked against the real spec at test time (via
`com.atlassian.oai:swagger-request-validator-core`), so a fixture that drifts from the documented
contract fails the test that defines it instead of silently going stale — this is how the
`"id"` field's type (spec says string; jettison's `JSONObject.getLong()` was lenient enough to
mask a plain number too) was caught while writing this suite. To add coverage for another
`JiraRestService` method: add a test method to `AbstractJiraRestServiceContractTest`, implement
its hooks in both subclasses, and call `OpenApiSpecConformance.assertConformsToSpec(...)` on the
new WireMock fixture body before stubbing it.

To run the live e2e test locally (see `LiveJiraCloudE2ETest`'s Javadoc for details): it's skipped
unless `JIRA_LIVE_TEST=true` plus `JIRA_LIVE_URL`/`JIRA_LIVE_USER`/`JIRA_LIVE_TOKEN`/
`JIRA_LIVE_PROJECT_KEY` are set, e.g.:

```sh
JIRA_LIVE_TEST=true \
JIRA_LIVE_URL=https://jenkins-jira-plugin.atlassian.net/ \
JIRA_LIVE_USER=you@example.com \
JIRA_LIVE_TOKEN=your-api-token \
JIRA_LIVE_PROJECT_KEY=SANDBOX \
mvn test -Dtest=LiveJiraCloudE2ETest
```

Running it creates a real issue and version in the target project (this plugin has no delete API
to clean them up with), so point `JIRA_LIVE_PROJECT_KEY` at a disposable/sandbox project, not a
production one.

### Releasing the plugin

See [releasing Jenkins plugins](https://www.jenkins.io/doc/developer/publishing/releasing-manually/).
