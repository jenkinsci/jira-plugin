# Staged deprecation removal: 3.x for fixes, 4.0 for removals

* Status: proposed
* Date: 2026-08-17

## Context and Problem Statement

The plugin has accumulated a substantial deprecated surface that nothing removes:

* Six deprecated `JiraSite` constructors, plus deprecated `getSession()`, `existsIssue(...)` and
  `getVersions(...)`.
* `JiraVersionCreator` and `JiraReleaseVersionUpdater` — deprecated `Notifier`s still registered as
  `@Extension`, **with display names identical to their `Builder` replacements**, so users pick from two
  indistinguishable "Jira: Mark a version as Released" entries in the UI.
* `useHTTPAuth`, a configuration knob that does nothing: outside its own getter, setter and constructors it
  appears only in `JiraSite/config.jelly`, and `JiraSessionFactory` branches solely on `isUseBearerAuth()`.
  It has confused contributors for four years
  ([#541](https://github.com/jenkinsci/jira-plugin/issues/541), PR
  [#562](https://github.com/jenkinsci/jira-plugin/pull/562)).
* `JiraFolderProperty.setSites(JiraSite)`, deprecated and *guaranteed* to throw
  `UnsupportedOperationException` — it calls `add` on a `Collections.emptyList()` field.
* An ungated legacy migration in `JiraProjectProperty.readResolve()` that mutates and `save()`s global
  configuration during deserialisation, with no removal marker.
* The `@Extension`-on-static-field descriptor pattern in six classes.

Meanwhile there is urgent work that must ship soon: a live production breakage against Jira Cloud
([0002](0002-jira-cloud-detection-and-search-api-routing.md)) and correctness bugs in the HTTP stack
([0003](0003-future-of-the-vendored-atlassian-http-client.md),
[0006](0006-http-client-lifecycle-and-jenkins-60536.md)).

With ~28,400 installs on a declining trend, the failure mode to avoid is obvious: a release that fixes the
Cloud breakage *and* removes API in the same breath, so upgrading to get the fix means also absorbing
breakage. Users who cannot take the breaking change stay on a broken version.

How do the two kinds of change get sequenced?

## Decision Drivers

* Users must be able to take correctness fixes without taking API removals.
* Removals must actually happen; "deprecate and never remove" is how the current backlog was built.
* Breaking changes are easier to absorb, document and support as one announced event than as a drip.
* Each individual PR must stay small enough to review against a diff-scoped quality gate.

## Considered Options

1. **Two-stage: correctness and internals in 3.x, all removals batched into a single 4.0 with an upgrade
   guide.** Deprecations are *marked* during 3.x and *removed* at the boundary.
2. **Remove opportunistically as each area is touched.** Fastest cleanup, and each PR stays small. Rejected:
   it couples the Cloud fix to API breakage, so users have no version that is both fixed and compatible —
   the exact failure mode above.
3. **Never remove; keep everything deprecated forever.** Zero migration cost for users. Rejected: it is the
   status quo that produced this list, it keeps two identically-named notifiers in the UI, and it keeps a
   dead `useHTTPAuth` knob that reads as a security control.
4. **Two parallel branches**, a maintenance `3.x` and a development `4.x`. Standard, and clean in principle.
   Rejected as disproportionate for a plugin with this maintainer capacity: every fix needs landing twice,
   and backport drift is precisely the tax a small team cannot pay.

## Decision Outcome

Chosen option: **1, two-stage**, because it is the only option where a user can pick up the Cloud fix without
also picking up an API break, while still committing to a date at which the backlog is actually cleared.

Implementation:

* **3.x carries**: the Cloud search fix, the HTTP-stack correctness work, `JiraSite` decomposition,
  session/cache correctness, the Java 21 idiom pass, pipeline gaps, fork convergence, and CI/CD
  ([0004](0004-continuous-delivery-and-version-numbering.md)). Behaviour changes and internal refactors,
  no removals.
* **3.x also prepares**: anything destined for removal gets `@Deprecated` with a Javadoc `@deprecated` note
  naming 4.0 as the removal release, and dead UI knobs are hidden before they are deleted. Users get at least
  one release of warning in the place they will actually see it.
* **4.0 carries the removals as one release**, with a written upgrade guide:
  * the six deprecated `JiraSite` constructors, `getSession()`, `existsIssue`, `getVersions`;
  * the deprecated `JiraVersionCreator` / `JiraReleaseVersionUpdater` notifiers, **with config migration** to
    their `Builder` replacements — this one cannot be a bare deletion, because the duplicate display names
    mean existing jobs may be on either;
  * `useHTTPAuth`, keeping the XStream field ignored rather than read, so old configs still load;
  * the `@Extension`-on-static-field descriptor pattern;
  * the ungated `JiraProjectProperty` migration;
  * the HttpClient 5 migration from [0003](0003-future-of-the-vendored-atlassian-http-client.md), and with it
    fugue, jettison and the pre-Jakarta `javax.ws.rs` usage.
* **`JiraFolderProperty.setSites(JiraSite)` is the exception and may be fixed or removed in 3.x.** It throws
  unconditionally, so it has no working callers to break. Removing something nobody can be using is not a
  breaking change.
* **4.0 waits for 3.x to soak.** The point of staging is that the fixes have been in users' hands before the
  breakage lands; releasing 4.0 immediately after would forfeit that.

### Consequences

* There is always a version that is both current and non-breaking, which is what makes the Cloud fix
  adoptable.
* The deprecated surface stays around for the whole 3.x line, and the confusing duplicate notifier entries
  stay visible in the UI until 4.0. Accepted deliberately: hiding them is a 3.x change, deleting them is not.
* 4.0 becomes a large, coordinated release needing a real upgrade guide and post-release support attention.
  Batching is what buys the single migration event; the concentration of risk is the other side of that coin.
* This ADR sets the sequencing rule the rest of the modernisation programme is planned against, so changing
  it invalidates that plan rather than just this document.
* Residual accepted risk: `readResolve()` in `JiraSite` rebuilds via a deprecated constructor and re-applies
  only a hardcoded subset of setters — so *any* field added during 3.x is silently reset on load unless it is
  hand-copied there. Removing constructors in 4.0 will force that method to be rewritten. An XStream
  round-trip test asserting every property survives is a prerequisite for both, and should land early in 3.x
  rather than alongside the removals.
