# Run the live Jira Cloud e2e suite as a release gate, not on every PR

* Status: accepted
* Date: 2026-09-16

## Context and Problem Statement

`LiveJiraCloudE2ETest` runs the shared `AbstractJiraRestServiceContractTest` suite against a real Jira
Cloud instance ([jenkins-jira-plugin.atlassian.net](https://jenkins-jira-plugin.atlassian.net/)), where
`JiraRestServiceWireMockTest` runs the same suite against canned responses. The WireMock suite is what
catches regressions in the plugin; the live suite is what catches the *stubs* going stale when Atlassian
changes the API. Until now the live suite only ran when a maintainer remembered to run it by hand, so
contract drift was found by users after a release, not by CI before one.

The repository now holds the credentials for the test instance (`JIRA_LIVE_USER` / `JIRA_LIVE_TOKEN`
secrets, `JIRA_LIVE_URL` / `JIRA_LIVE_PROJECT_KEY` variables). Where in CI should the suite run?

## Decision Drivers

* Secrets must never reach fork-controlled code, the same constraint that shaped
  [ADR 0001](0001-sonarcloud-analysis-for-fork-prs.md).
* Every run creates a real issue and a real version in the target project, and the plugin has no delete
  API to clean them up with. Run frequency is therefore a cost, not just a wall-clock number.
* A check nobody looks at is not a check. The result has to land somewhere a human is already waiting.
* Releasing must not get *less* safe than it is today ([ADR 0004](0004-continuous-delivery-and-version-numbering.md)).

## Considered Options

1. **Blocking step in the on-demand `cd.yaml` release workflow.** Runs after the "latest commit's CI is
   green" check and before anything is drafted or published. Secrets are in scope because
   `workflow_dispatch` only runs from the base repository; a failure stops the release.
2. **On every pull request.** Fork PRs do not receive secrets, so the check would fail or silently skip for
   most contributions, and the two-workflow split from ADR 0001 does not help here because the trusted
   half would have to *run* untrusted test code, not just upload its output. Rejected.
3. **On every push to `master`, or on a nightly schedule.** Secrets are available, but every run litters
   the sandbox project with an issue and a version, and a red nightly run has no owner. Rejected: the
   moment the result matters is the moment before a release, and that already has a human at the wheel.
4. **Non-blocking (`continue-on-error`) step in `cd.yaml`.** Keeps releases flowing through a sandbox
   outage. Rejected: a gate that cannot fail is a log line, and the point of the exercise is to stop a
   release shipping against an API contract we know is broken.

## Decision Outcome

Chosen option: **1, a blocking step in `cd.yaml`**, because it is the only place where the secrets are
safely in scope, the run count is bounded by the (deliberately manual) release cadence, and the person who
triggered the workflow is already watching the result.

Implementation:

* `.github/workflows/cd.yaml` runs `mvn --batch-mode -ntp test -Dtest=LiveJiraCloudE2ETest` with
  `JIRA_LIVE_TEST=true` and the four connection variables in the step's `env`, between the CI-status check
  and the release drafter, with a `timeout-minutes` guard so a hung Jira call cannot block a release run
  indefinitely.
* The test's own `requireEnv` fails fast with a clear message if a secret or variable is missing, so a
  misconfigured repository refuses to release rather than skipping the gate.

### Consequences

* API contract drift is caught before a release ships, not after.
* A Jira Cloud sandbox outage blocks a release. Re-run the workflow later, or fall back to the manual
  release path documented in `CONTRIBUTING.md`; do not add `continue-on-error` to get past it.
* The sandbox project accrues one issue and one version per release run. Releases are rare and manual
  (ADR 0004), so this stays bounded; if it becomes a nuisance, add cleanup to the test rather than
  moving the gate.
* `LiveJiraCloudE2ETest` still does not count towards SonarCloud's `new_coverage` (it never runs on PRs),
  so offline coverage in `JiraRestServiceWireMockTest` remains mandatory for new `JiraRestService` code.
