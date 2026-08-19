# Modernisation — draft epics and issues

Draft backlog for the modernisation programme, for manual review before anything is created on GitHub.
Derived from the modernisation plan and ADRs 0002-0006.

## Structure

The repo already has what this needs, so nothing new has to be invented:

- **`Epic` is an enabled issue type** here, alongside `Task`, `Bug`, `Enhancement` and `Documentation` —
  and the latter four are already in active use. So each epic is a real issue with type `Epic`, and the
  work below hangs off it as native **sub-issues**, not as a checklist in the body.
- **Two milestones only: `3.x` and `4.0`.** No per-release sub-milestones — sequencing lives in the epic
  grouping and in the row order within each epic. This also sidesteps the fact that CD (ADR 0004) makes
  version numbers incremental and on-demand, so fixed release numbers stop being predictable anyway. The
  existing `4.0` milestone is reused; `3.x` is new.
- **Labels** all exist already: `bug`, `enhancement`, `feature`, `maintenance`, `internal`, `test`,
  `documentation`, `breaking`, `pipeline`, `jira-cloud`, `jira-server`, `security`, `good first issue`,
  `configuration`, `jcasc-compatibility`, `localization`, `needs-real-testing`.

**Two backlog corrections to make first:**

- **#1175 moves out of the `4.0` milestone into `3.x`.** `maven.compiler.release` already evaluates to 21
  from the 2.555.x parent POM, so the minimum is a fact, not a pending break. What is left is making it
  explicit and fixing the docs that still claim Java 17.
- **The `4.0` milestone is otherwise empty** and becomes epic 6's home.

`[GFI]` marks issues suitable for the `good first issue` label: self-contained, one obvious fix, provable
with an offline test, no real Jira instance and no design judgement required.

Rows are listed in intended execution order within each epic.

---

## Epic 1 — Jira Cloud correctness

`Epic` · milestone `3.x` · `jira-cloud` · implements ADR 0002

> JQL operations fail against Jira Cloud sites configured with the API-gateway URL. Highest-impact open
> defect; ships before anything else and on its own.

| Type | Title | Labels |
|---|---|---|
| Bug | **#747** — JQL steps fail against `api.atlassian.com` Cloud sites | `bug` `jira-cloud` |
| Task | Add a site-level Cloud / Data Center override, so vanity domains and proxies aren't left to a hostname guess | `jira-cloud` |
| Task | Assert in tests that Cloud and Data Center sites reach their own search endpoints | `test` |
| Bug | Validating a site can hang a Jenkins request thread indefinitely | `bug` |
| Bug | Failed Jira calls lose their cause and swallow interrupts, making them hard to diagnose | `bug` |
| Task | **#1178** — Retire the second, legacy HTTP client still used for versions and components | `internal` |
| Enhancement | Fetch more than the first page of JQL results on Jira Cloud | `enhancement` `jira-cloud` |

The last one is the residual risk recorded in ADR 0002 — worth an issue now so it isn't forgotten once
the `410` stops being visible.

---

## Epic 2 — HTTP stack correctness

`Epic` · milestone `3.x` · `internal` · implements ADR 0003 and ADR 0006

> The vendored HTTP client has correctness bugs, its settings are partly inert, and it is excluded from
> static analysis. Fix it in place; HttpClient 5 is a 4.0 target.

| Type | Title | Labels |
|---|---|---|
| Task | Record in the code why an HTTP client is built per request, so it stops being "fixed" and reverted | `internal` |
| Bug | Every Jira site silently inherits the first configured site's connection settings | `bug` |
| Bug | The "Read timeout" setting has no effect, and connect timeout is never applied | `bug` `configuration` |
| Bug | A shared thread pool is sized from one site and can be shut down out from under the others | `bug` |
| Documentation | Document that trusting self-signed certificates also disables hostname verification | `documentation` `security` `[GFI]` |
| Task | Reproduce and root-cause the connection freeze behind JENKINS-60536 (spike) | `internal` |
| Task | Bring the vendored HTTP tree under static analysis and add its missing license headers | `internal` |
| Task | Clean up the vendored tree: deprecated TLS APIs, dead code paths, unused event plumbing | `internal` |

Also on the epic, not as issues: close PR #755 with the ADR 0003 rationale, and add a SpotBugs
suppression pointing at ADR 0006 rather than "fixing" the per-request client.

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
| Bug | A Jira session created for one folder can be handed to jobs in another | `bug` `security` |
| Bug | A transient failure gets cached for two minutes, and the issue cache is unbounded | `bug` |
| Bug | The project list is never refreshed until restart, and times out without logging anything | `bug` |
| Task | **#731** — Adopt Java 21 language features where they make the code clearer | `maintenance` |
| Bug | Adding a Jira site to a folder throws an error every time | `bug` `[GFI]` |
| Bug | `jiraIssueSelector` throws when no selector is configured | `bug` `pipeline` `[GFI]` |
| Bug | `jiraSearch` fails unhelpfully when no Jira site is configured | `bug` `pipeline` `[GFI]` |
| Bug | Equal version objects can produce different hash codes | `bug` `[GFI]` |
| Bug | A redundant duplicate Jira call on every workflow transition | `bug` `[GFI]` |
| Bug | Credential migration reports success even when saving failed | `bug` `[GFI]` |
| Bug | The explicit issue selector stores its keys twice and they can disagree | `bug` |
| Task | Replace deprecated credentials and security APIs | `maintenance` |
| Task | Export Jira environment variables in Pipeline builds, not just freestyle | `pipeline` |
| Task | Make issue creation and issue migration usable from Pipeline | `pipeline` |
| Task | Give global and per-job configuration proper JCasC names | `jcasc-compatibility` |
| Task | Stop hiding Pipeline steps when class loading fails | `pipeline` `[GFI]` |
| Task | Delete config files and message keys for classes that no longer exist | `maintenance` `[GFI]` |
| Task | Move hard-coded English out of the config screens so it can be translated | `localization` `[GFI]` |

---

## Epic 4 — CI/CD, release automation and housekeeping

`Epic` · milestone `3.x` · `internal` · implements ADR 0004

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
| Documentation | Fix stale claims in `README.md` and `AGENTS.md` | `documentation` `[GFI]` |
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
| Bug | **#677** — Built-in fields such as labels can't be updated from Pipeline | `bug` `pipeline` |
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

`Epic` · milestone `4.0` · `breaking` · implements ADR 0005

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
non-surprising, and per ADR 0005 users get at least one release of warning where they'll actually see it.
They are the only rows in the whole draft whose milestone differs from their epic's.

---

## Counts

| Epic | Issues | Milestone | Already open |
|---|---|---|---|
| 1 · Jira Cloud correctness | 7 | 3.x | #747, #1178 |
| 2 · HTTP stack correctness | 8 | 3.x | — |
| 3 · Java 21 and code health | 21 | 3.x | #1175, #731 |
| 4 · CI/CD, release automation and housekeeping | 13 | 3.x | #468, #1179, #714 |
| 5 · Extend step and parameter functionality | 7 | 3.x | #677, #691, #694, #453 |
| 6 · 4.0 breaking changes | 9 | 4.0 (2 rows in 3.x) | #541 |
| **Total** | **65 issues + 6 epics** | 58 in `3.x`, 7 in `4.0` | 12 already open |

**16 carry `good first issue`** — 10 in epic 3, 5 in epic 4, 1 in epic 2. Those are the epics where a
drive-by contributor can be useful without needing a real Jira instance.

Four open PRs are resolved without merging as-is: **#754** superseded by epic 1, **#755** by ADR 0003,
**#562** closes with #541, and **#746** gets rebased under epic 5 rather than closed.
