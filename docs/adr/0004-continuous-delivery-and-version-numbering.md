# Continuous delivery and version numbering

* Status: accepted
* Date: 2026-08-17

## Context and Problem Statement

Releases are currently cut by hand, following
[releasing Jenkins plugins manually](https://www.jenkins.io/doc/developer/publishing/releasing-manually/).
The last three (3.20, 3.21, 3.22) span nine months, so fixes wait on a maintainer finding time rather than
on being ready.

Almost every prerequisite for [JEP-229 continuous delivery](https://www.jenkins.io/doc/developer/publishing/releasing-cd/)
is already in the repository:

* `pom.xml` uses `<version>${revision}${changelist}</version>` with `revision` / `changelist` properties.
* `.mvn/extensions.xml` declares `git-changelist-maven-extension:1.13`.
* `.mvn/maven.config` enables `-Pconsume-incrementals -Pmight-produce-incrementals`.
* `.github/workflows/release-drafter.yml` already assembles draft release notes.

Missing: `.github/workflows/cd.yaml` and a `MAINTAINERS` file. Also `<scm><tag>jira-3.20</tag></scm>` is
stale — two releases behind the actual `jira-3.22`.

So the mechanics are nearly free. [#468](https://github.com/jenkinsci/jira-plugin/issues/468) has been open
since 2022 anyway, for two reasons that are about policy, not plumbing:

* The default CD version scheme *increments the major component* on every release
  (`3.x` → `4.x` → `5.x` …), which throws away the signal a major version is supposed to carry — and this
  project has a real 4.0 planned with real breaking changes (see
  [0005](adr/0005-staged-deprecation-removal.md)). Two things cannot both own the number "4".
* A maintainer is **-0 on the whole idea**, on the grounds that something as important as releasing should
  not depend on external concurrent tooling.

Any decision that ignores either objection will not stick.

## Decision Drivers

* Version numbers must stay meaningful: `3.x` for the correctness/internals work, `4.0` reserved for the
  deliberate breaking release.
* Releasing must not become *less* controllable than it is today. Automation should remove the toil, not the
  maintainer's judgement about when to ship.
* Address the "-0" objection directly rather than out-voting it — a release process one maintainer distrusts
  is a release process that stalls at the worst moment.
* Keep the working manual path available as a fallback.

## Considered Options

1. **CD with a manually controlled version prefix, released on demand rather than on every merge.** Pin the
   prefix so versions read `3.<incremental>.v<sha>`, and disable the automatic trigger so publishing is a
   deliberate act (`workflow_dispatch`) rather than a consequence of merging.
2. **CD with the default scheme** (`1.x`, then major-incrementing). Least configuration. Rejected: it burns
   the major-version signal, which is exactly the objection raised on #468, and it collides with the planned
   4.0.
3. **Versioning with wrapped components** — `4.0.0-123.vabcdef456789`, the other variant floated on the
   issue. Keeps a semantic prefix and is a legitimate choice. Rejected as more machinery than this project
   needs: it is aimed at plugins wrapping an external component whose version is the meaningful part, which
   is not the situation here.
4. **Stay fully manual.** Honest about the "-0", and today's behaviour. Rejected: nine months for three
   releases is the status quo being argued against, and it leaves the shipped fix for a live production
   breakage ([0002](adr/0002-jira-cloud-detection-and-search-api-routing.md)) waiting on calendar time.
5. **Automate only the release notes** (keep `release-drafter`, keep manual `mvn release`). Rejected as
   already the status quo — release-drafter is wired and the toil that remains is the part it does not cover.

## Decision Outcome

Chosen option: **1, CD with a manually controlled prefix and an on-demand trigger**, because it removes the
toil while leaving both the numbering and the timing under maintainer control — which is what the two
objections on #468 were actually about.

Implementation:

* Add `.github/workflows/cd.yaml` per JEP-229, with `workflow_dispatch` rather than push-triggered release.
  Merging to `master` never publishes on its own; a maintainer decides when a release happens, exactly as
  today. This is the concession to the "-0" position, and it is deliberate: what is being automated is the
  *mechanics* of cutting a release, not the *decision* to cut one.
* Set the manually controlled prefix to `3` so releases read `3.<incremental>.v<sha>`, keeping the `3.x`
  line intact and leaving `4.0` free for the breaking release in
  [0005](adr/0005-staged-deprecation-removal.md). When 4.0 arrives, the prefix moves to `4` — one property
  change, not a scheme change.
* Add `MAINTAINERS` listing the maintainers authorised to release.
* Fix `<scm><tag>` to track reality, and stop hand-maintaining it once CD owns it.
* Prove it before switching over: cut one release through `cd.yaml` from a branch, verify the artifact and
  the version string, and only then point `master` at it. The manual path stays documented in
  `CONTRIBUTING.md` as the fallback.

### Consequences

* Fixes ship when they are ready rather than when a release window happens to open, and the
  release-notes automation already in place finally has something to hang off.
* Version numbers keep meaning what they say: `3.x` is the current line, `4.0` is the announced breaking
  change and nothing else.
* Releasing does gain a dependency on GitHub Actions and the Jenkins CD infrastructure. This is real and was
  the stated objection. It is mitigated, not eliminated: the trigger is manual, the manual path is retained,
  and the first release through the new pipeline is a rehearsal on a branch.
* `workflow_dispatch` means the incremental component advances only when someone releases, so version numbers
  will have gaps relative to merge count. That is intended — the number tracks releases, not commits.
* Residual accepted risk: on-demand triggering keeps the human bottleneck that CD is usually adopted to
  remove. If release cadence is still the constraint after a few cycles, revisit the trigger — not the
  numbering — and supersede this ADR.
