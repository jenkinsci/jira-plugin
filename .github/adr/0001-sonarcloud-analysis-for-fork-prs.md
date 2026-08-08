# SonarCloud analysis for pull requests from forks

* Status: accepted
* Date: 2026-08-08

## Context and Problem Statement

`.github/workflows/sonarcloud.yml` runs `mvn ... sonar:sonar` on every `pull_request` and passes
`SONAR_TOKEN` into the job's environment. GitHub does not expose repository secrets to
`pull_request`-triggered workflow runs when the PR comes from a fork, so this check either fails
or silently skips analysis for every fork PR. Since this is a community-maintained Jenkins plugin,
most contributions arrive via forks, so in practice fork PRs get no SonarCloud feedback (and a
broken/red status check) today.

How do we get SonarCloud analysis working for fork PRs without exposing `SONAR_TOKEN` to
untrusted, fork-controlled code?

## Decision Drivers

* `SONAR_TOKEN` must never be reachable by code an external contributor controls (the classic
  "pwn request" vulnerability class in GitHub Actions).
* Fork contributors should still get automatic PR feedback, without a maintainer having to
  manually trigger anything per PR.
* Keep the existing, working `push` / same-repo-PR path untouched.

## Considered Options

1. **Two-workflow split (`pull_request` build + `workflow_run` analysis)** — an untrusted build
   job (no secrets) compiles/tests the fork PR, uploads artifacts; a separate, trusted job
   (triggered by `workflow_run`, has `SONAR_TOKEN`) downloads those artifacts and submits them to
   SonarCloud. This is the pattern documented by the SonarSource community and used by other OSS
   projects.
2. **`pull_request_target`** — runs with base-repo secrets and permissions directly on the PR
   event, but checking out and building the fork's code under this trigger is the textbook "pwn
   request" setup: the fork's build (including Maven build extensions declared in its `pom.xml`)
   would execute with `SONAR_TOKEN` in scope. Rejected as unsafe for a compiled-language project
   that needs a real build.
3. **Manual/`workflow_dispatch`-triggered analysis** — a maintainer manually re-runs analysis
   against a specific fork PR after reviewing it. Safer and simpler, but no automatic feedback on
   fork PRs and adds manual maintainer toil for every external contribution. Rejected in favor of
   automation, per user preference.
4. **Disable analysis on fork PRs entirely** (as some other `jenkinsci` plugins do, e.g.
   `build-history-manager-plugin`) — simplest, but leaves the largest share of contributions
   (forks) with no code-quality feedback at all. Rejected.

## Decision Outcome

Chosen option: **1, the two-workflow split**, because it is the only option that gives fork
contributors automatic SonarCloud feedback without ever putting `SONAR_TOKEN` in scope of
fork-controlled code execution.

Implementation:

* `.github/workflows/sonarcloud.yml` — unchanged behavior, gated with
  `if: github.event_name != 'pull_request' || github.event.pull_request.head.repo.fork == false`
  so it no longer runs (and fails) on fork PRs.
* `.github/workflows/sonarcloud-fork-build.yml` — runs on fork `pull_request` events with
  `permissions: {}`. Builds and tests the code, uploads `target/classes` +
  `target/site/jacoco/jacoco.xml` and PR metadata (head repo/ref/sha, base ref, PR number) as
  short-lived (`retention-days: 1`) artifacts. No secrets in scope.
* `.github/workflows/sonarcloud-fork-analysis.yml` — triggered by `workflow_run` after the build
  workflow succeeds. Runs in the base repo's trusted context (`SONAR_TOKEN` available). Downloads
  the artifacts, checks out the fork's head commit read-only, and submits analysis via
  `SonarSource/sonarqube-scan-action` (the `sonar-scanner` CLI) — **not** `mvn sonar:sonar` —
  so this trusted job never asks Maven to parse the fork's `pom.xml` (which could otherwise
  execute arbitrary code via `<build><extensions>` regardless of which goal is requested). PR
  metadata pulled from the untrusted artifact (head ref, head sha, head repo, base ref) is
  validated against a strict allowlist regex before being used in `checkout`'s `ref:`/`repository:`
  inputs or in `-Dsonar.pullrequest.*` arguments, since these are attacker-influenced strings.

### Consequences

* Fork PRs get the same SonarCloud PR decoration/quality-gate feedback as same-repo PRs, fully
  automatically.
* Two extra workflow files to maintain in parallel with `sonarcloud.yml`; if the build command or
  Java version there changes, the fork build workflow needs the same change.
* Residual accepted risk: the trusted analysis job still reads (checks out) fork-authored source
  files and jacoco XML/class files produced by the untrusted build job. It does not execute them,
  but a maliciously crafted `jacoco.xml`/class file exploiting a bug in `sonar-scanner` itself is a
  theoretical (if unlikely) risk shared by every project using this pattern. Mitigated by scoping
  `SONAR_TOKEN` on sonarcloud.io to "Execute Analysis" only.
