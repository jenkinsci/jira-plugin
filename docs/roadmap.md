# Modernisation — draft epics and issues

Derived from the modernisation plan and [ADRs 0002-0006](adr/README.md) by @rantoniuk.

## Epic 1 — Jira Cloud correctness

`Epic` · milestone `3.x` · `jira-cloud` · implements [ADR 0002](adr/0002-jira-cloud-detection-and-search-api-routing.md)

> JQL operations fail against Jira Cloud sites configured with the API-gateway URL. Highest-impact open
> defect; ships before anything else and on its own.

| Type | Title | Labels |
|---|---|---|
| Bug | **#747** — JQL steps fail against `api.atlassian.com` Cloud sites | `bug` `jira-cloud` |
| Task | Add a site-level Cloud / Data Center override, so vanity domains and proxies aren't left to a hostname guess | `jira-cloud` |
| Task | Assert in tests that Cloud and Data Center sites reach their own search endpoints | `test` |
| Bug | Validating a site can hang a Jenkins request thread indefinitely | `bug` |
| Bug | Failed Jira calls lose their cause and swallow interrupts, making them hard to diagnose | `bug` |
| Task | **#1178** — Retire the second, legacy HTTP client still used for versions and components — open PR [#1193](https://github.com/jenkinsci/jira-plugin/pull/1193) (draft) | `internal` |
| Enhancement | Fetch more than the first page of JQL results on Jira Cloud | `enhancement` `jira-cloud` |

The last one is the residual risk recorded in [ADR 0002](adr/0002-jira-cloud-detection-and-search-api-routing.md) — worth an issue now so it isn't forgotten once
the `410` stops being visible.

---

## Epic 2 — HTTP stack correctness

`Epic` · milestone `3.x` · `internal` · implements [ADR 0003](adr/0003-future-of-the-vendored-atlassian-http-client.md) and [ADR 0006](adr/0006-http-client-lifecycle-and-jenkins-60536.md)

> The vendored HTTP client has correctness bugs, its settings are partly inert, and it is excluded from
> static analysis. Fix it in place; HttpClient 5 is a 4.0 target.

| Type | Title | Labels |
|---|---|---|
| Task | Record in the code why an HTTP client is built per request, so it stops being "fixed" and reverted | `internal` |
| Bug | Every Jira site silently inherits the first configured site's connection settings — open PR [#1199](https://github.com/jenkinsci/jira-plugin/pull/1199) | `bug` |
| Bug | The "Read timeout" setting has no effect, and connect timeout is never applied — open PR [#1200](https://github.com/jenkinsci/jira-plugin/pull/1200) | `bug` `configuration` |
| Bug | A shared thread pool is sized from one site and can be shut down out from under the others — open PR [#1200](https://github.com/jenkinsci/jira-plugin/pull/1200) | `bug` |
| Documentation | Document that trusting self-signed certificates also disables hostname verification | `documentation` `security` `[GFI]` |
| Task | Reproduce and root-cause the connection freeze behind JENKINS-60536 (spike) | `internal` |
| Task | Bring the vendored HTTP tree under static analysis and add its missing license headers | `internal` |
| Task | Clean up the vendored tree: deprecated TLS APIs, dead code paths, unused event plumbing | `internal` |

Also on the epic, not as issues: close PR #755 with the [ADR 0003](adr/0003-future-of-the-vendored-atlassian-http-client.md)
rationale, and add a SpotBugs suppression pointing at
[ADR 0006](adr/0006-http-client-lifecycle-and-jenkins-60536.md) rather than "fixing" the per-request client.

---

## Epic 3 — Java 21 and code health

`Epic` · milestone `3.x` · `maintenance`

> The plugin compiles at Java 21 but uses none of it, `JiraSite` has grown to 1639 lines, and a cluster
> of small independent defects has accumulated.

| Type | Title | Labels |
|---|---|---|
| Task | **#1175** — Declare the Java 21 minimum explicitly and fix the docs that still say Java 17 | `maintenance` `[GFI]` |
| Task | Add a test proving a fully-configured Jira site survives being saved and reloaded | `test` |
| Task | Split `JiraSite` — extract client construction and issue-link caching | `internal` |
| Bug | A Jira session created for one folder can be handed to jobs in another — open PR [#1200](https://github.com/jenkinsci/jira-plugin/pull/1200) | `bug` `security` |
| Bug | A transient failure gets cached for two minutes, and the issue cache is unbounded — open PR [#1200](https://github.com/jenkinsci/jira-plugin/pull/1200) | `bug` |
| Bug | The project list is never refreshed until restart, and times out without logging anything — open PR [#1200](https://github.com/jenkinsci/jira-plugin/pull/1200) | `bug` |
| Task | **#731** — Adopt Java 21 language features where they make the code clearer | `maintenance` |
| Bug | Adding a Jira site to a folder throws an error every time — open PR [#1202](https://github.com/jenkinsci/jira-plugin/pull/1202) | `bug` `[GFI]` |
| Bug | `jiraIssueSelector` throws when no selector is configured | `bug` `pipeline` `[GFI]` |
| Bug | `jiraSearch` fails unhelpfully when no Jira site is configured | `bug` `pipeline` `[GFI]` |
| Bug | Equal version objects can produce different hash codes — open PR [#1201](https://github.com/jenkinsci/jira-plugin/pull/1201) | `bug` `[GFI]` |
| Bug | `JiraIssueField.compareTo` recurses into itself and throws `StackOverflowError` — open PR [#1208](https://github.com/jenkinsci/jira-plugin/pull/1208) | `bug` |
| Bug | A redundant duplicate Jira call on every workflow transition — open PR [#1200](https://github.com/jenkinsci/jira-plugin/pull/1200) | `bug` `[GFI]` |
| Bug | Credential migration reports success even when saving failed — open PR [#1200](https://github.com/jenkinsci/jira-plugin/pull/1200) | `bug` `[GFI]` |
| Bug | The explicit issue selector stores its keys twice and they can disagree — open PR [#1205](https://github.com/jenkinsci/jira-plugin/pull/1205) | `bug` |
| Task | Replace deprecated credentials and security APIs | `maintenance` |
| Task | Give global and per-job configuration proper JCasC names | `jcasc-compatibility` |
| Task | Stop hiding Pipeline steps when class loading fails | `pipeline` `[GFI]` |
| Task | Delete config files and message keys for classes that no longer exist | `maintenance` `[GFI]` |
| Task | Move hard-coded English out of the config screens so it can be translated | `localization` `[GFI]` |

---

## Epic 4 — CI/CD, release automation and housekeeping

`Epic` · milestone `3.x` · `internal` · implements [ADR 0004](adr/0004-continuous-delivery-and-version-numbering.md)

> Releases are cut by hand though the prerequisites are already in the repo; some checks never run in CI;
> several docs describe a version of the project that no longer exists. Independent of epics 1-3.

| Type | Title | Labels |
|---|---|---|
| Task | **#468** — Automate releases, keeping the version scheme and the release decision under maintainer control | `internal` |
| Task | Add a `MAINTAINERS` file and fix the stale release tag in `pom.xml` | `internal` |
| Task | **#1179** — Comment on issues when a release that fixes them is published | `internal` |
| Task | **#714** — Separate the fast unit tests from the ones that boot Jenkins | `test` |
| Bug | The test suite claims to run offline but downloads a spec on start-up | `bug` `test` |
| Task | Run the static-analysis check in CI, where it currently never runs | `internal` |
| Task | Remove the dead coverage plugin and its obsolete workaround | `maintenance` `[GFI]` |
| Task | Delete the leftover manual test programs from the pre-JUnit 5 era | `test` `[GFI]` |
| Documentation | Fix stale claims in `README.md` and `AGENTS.md` — open PR [#1190](https://github.com/jenkinsci/jira-plugin/pull/1190) | `documentation` `[GFI]` |
| Documentation | `docs/changelog.md` stops in March 2020 — point it at releases or regenerate it | `documentation` `[GFI]` |
| Task | Delete stale branches from this repo — five are merged or abandoned and every new fork copies them | `internal` `[GFI]` |
| Task | Triage the ~25 imported JENKINS-* issues from 2011-2019 | `maintenance` |
| Enhancement | Consider adding CodeQL alongside the existing security scan | `security` |

---

## Epic 5 — Extend step and parameter functionality

`Epic` · milestone `3.x` · `feature`

> A set of long-standing gaps in what the steps and build parameters can actually express: fields that
> cannot be updated, updates that overwrite instead of append, steps unavailable as post-build actions,
> and version/issue parameters that cannot be filtered the way users ask for. Several already have
> working implementations in the wild that can be adapted rather than written from scratch.

| Type | Title | Labels |
|---|---|---|
| Bug | **#677** — Built-in fields such as labels can't be updated from Pipeline — open PR [#1207](https://github.com/jenkinsci/jira-plugin/pull/1207) | `bug` `pipeline` |
| Enhancement | Support structured and multi-value field updates, appending instead of overwriting | `enhancement` `pipeline` |
| Enhancement | **#691**, **#694** — Allow field updates and workflow transitions as post-build actions | `enhancement` |
| Enhancement | **#453** — Issue selector for all changes since the last successful build | `enhancement` |
| Enhancement | Set a release date when creating a version | `enhancement` |
| Enhancement | Offer only versions with no unresolved issues in the version parameter | `enhancement` |
| Task | Check whether the Rebuild plugin can still round-trip a Jira issue parameter | `test` |

Rows 1 and 2 both rewrite the same Pipeline step and must land as one change — file them as sub-issues of
a single PR, or merge them into one issue. The unresolved-issue filter depends on #1178 in epic 1, because
the endpoint it needs sits on the code path that change deletes.

---

## Epic 6 — 4.0 breaking changes

`Epic` · milestone `4.0` · `breaking` · implements [ADR 0005](adr/0005-staged-deprecation-removal.md)

> Removals batched into one announced release, so users can take the Cloud fix without also taking API
> breakage. Opens now to collect scope; starts only once the 3.x work has soaked.

| Type | Title | Milestone |
|---|---|---|
| Task | Give the duplicated "Mark a version as Released" entries distinguishable names | `3.x` |
| Task | Hide the non-functional HTTP authentication option and mark it for removal | `3.x` |
| Task | **#541** — Remove the non-functional HTTP authentication option | `4.0` |
| Task | Remove the deprecated `JiraSite` constructors and accessors | `4.0` |
| Task | Remove the superseded version notifiers, migrating existing job configuration | `4.0` |
| Task | Remove the ungated legacy per-project configuration migration | `4.0` |
| Task | Modernise the descriptor registration pattern | `4.0` |
| Task | Move to Apache HttpClient 5 and drop the libraries it makes redundant | `4.0` |
| Documentation | Write the 4.0 upgrade guide | `4.0` |

The first two are 3.x mitigations deliberately parented to this epic: they make the 4.0 removals
non-surprising, and per [ADR 0005](adr/0005-staged-deprecation-removal.md) users get at least one release of warning where they'll actually see it.
They are the only rows in the whole draft whose milestone differs from their epic's.

---

## Epic 7 — Pipeline step parity

`Epic` · milestone `4.0` · `pipeline`

> Three freestyle-only classes have no Pipeline equivalent: `JiraCreateIssueNotifier`,
> `JiraIssueMigrator`, and `JiraEnvironmentVariableBuilder` — exactly the gap `docs/features.md`
> names in its "Freestyle only" section and explains in "What is not yet supported in Pipeline".
> The [Jenkins.io Pipeline Steps Reference](https://www.jenkins.io/doc/pipeline/steps/jira/) is
> generated straight from this plugin's `@Symbol` steps on every release, so it's the objective
> measure of parity: closing this epic means every plugin capability shows up there. Follows the
> same `Builder`+`SimpleBuildStep`+`@Symbol` pattern already used to replace
> `JiraVersionCreator`/`JiraReleaseVersionUpdater` (epic 6) — sequenced into `4.0` alongside that
> cleanup, since retiring the freestyle-only originals once their replacements exist is itself a
> breaking change.

| Type | Title | Labels |
|---|---|---|
| Task | Add a Pipeline step that creates/comments on an issue when a build fails, replacing `JiraCreateIssueNotifier` | `pipeline` |
| Task | Add a Pipeline step that adds or migrates a fix version on matching issues, replacing `JiraIssueMigrator` | `pipeline` |
| Task | Decide whether `JiraEnvironmentVariableBuilder` still needs a dedicated step, given the `jiraIssueSelector` + `JiraSite.get(...)` workaround already documented in `docs/features.md` | `pipeline` |
| Documentation | Update `docs/features.md`'s "Freestyle only" and "What is not yet supported in Pipeline" sections once parity is reached | `documentation` `pipeline` |

Once these land, epic 6 gains a follow-up row to retire the superseded `JiraCreateIssueNotifier` and
`JiraIssueMigrator` classes the same way it already retires `JiraVersionCreator` and
`JiraReleaseVersionUpdater`.

---

## Counts

| Epic | Issues | Milestone | Already open |
|---|---|---|---|
| 1 · Jira Cloud correctness | 7 | 3.x | #747, #1178 |
| 2 · HTTP stack correctness | 8 | 3.x | — |
| 3 · Java 21 and code health | 20 | 3.x | #1175, #731 |
| 4 · CI/CD, release automation and housekeeping | 13 | 3.x | #468, #1179, #714 |
| 5 · Extend step and parameter functionality | 7 | 3.x | #677, #691, #694, #453 |
| 6 · 4.0 breaking changes | 9 | 4.0 (2 rows in 3.x) | #541 |
| 7 · Pipeline step parity | 4 | 4.0 | — |
| **Total** | **68 issues + 7 epics** | 57 in `3.x`, 11 in `4.0` | 12 already open |

**16 carry `good first issue`** — 10 in epic 3, 5 in epic 4, 1 in epic 2. Those are the epics where a
drive-by contributor can be useful without needing a real Jira instance.

Four open PRs are resolved without merging as-is: **#754** superseded by epic 1, **#755** by
[ADR 0003](adr/0003-future-of-the-vendored-atlassian-http-client.md), **#562** closes with #541, and
**#746** gets rebased under epic 5 rather than closed.
