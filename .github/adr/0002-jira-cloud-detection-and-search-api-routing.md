# Jira Cloud detection and search API routing

* Status: proposed
* Date: 2026-08-17

## Context and Problem Statement

Atlassian removed `GET`/`POST /rest/api/{2,3,latest}/search` from Jira Cloud (CHANGE-2046); the endpoint
now returns `410 Gone`. The replacement is `POST /rest/api/{2|3|latest}/search/jql`, with cursor paging via
`nextPageToken` and an explicit `fields` list.

`jira-rest-java-client` (JRJC) already handles both: `AsynchronousSearchRestClient` targets the legacy
Data Center endpoint, `AsynchronousCloudSearchRestClient` targets `search/jql`. Which one you get is
decided in `AsynchronousJiraRestClient`. Its two-argument constructor picks by calling
`UriUtil.isURICloud(serverUri)`, whose `CLOUD_DOMAINS` list is exactly:

```
["atlassian.net", "jira.com"]
```

`hudson.plugins.jira.extension.ExtendedAsynchronousJiraRestClient` calls that two-argument constructor, so
this plugin inherits the heuristic wholesale.

The heuristic misses `api.atlassian.com/ex/jira/{cloudId}/` — the gateway form Atlassian now pushes for
scoped access. Every JQL operation against a site configured that way gets a `410`. That is issue
[#747](https://github.com/jenkinsci/jira-plugin/issues/747).

Worse, this plugin actively steers users into it. `src/main/resources/hudson/plugins/jira/JiraSite/help-url.html`
says, verbatim:

> **Note for Jira Cloud users:** To avoid CAPTCHA errors, use the API endpoint
> `https://api.atlassian.com/ex/jira/{cloudId}/` instead of your standard Atlassian URL.

That advice is correct on its own terms — the CAPTCHA problem is real — but it points at the one URL form
the detection does not recognise. Users who follow our own help text break their JQL steps.

Two open PRs and two Renovate PRs each assume this needs either a dependency bump or a class-shadowing
hack. Neither is true, which is what this ADR exists to record.

## Decision Drivers

* Fix a live production breakage in a patch-level 3.x release, with a small, reviewable diff.
* No new dependency, no dependency bump on the critical path — the HTTP stack work (see
  [0006](0006-http-client-lifecycle-and-jenkins-60536.md)) has to settle before JRJC moves.
* Users behind vanity domains, gateways or reverse proxies must not be at the mercy of a hostname
  heuristic; they need an escape hatch.
* Do not shadow third-party classes. A same-FQCN override in `src/main/java` is invisible at the call
  site, silently wins or loses depending on classloader order, and rots the moment upstream changes.

## Considered Options

1. **Thread an explicit `isCloudVersion` flag through this plugin's own client construction.**
   `AsynchronousJiraRestClient` has a three-argument constructor, `(URI, DisposableHttpClient, boolean)`,
   that takes the decision as a parameter and skips `UriUtil` entirely. Verified present in JRJC
   **6.0.2 — the version already pinned in `pom.xml`** — along with `AsynchronousCloudSearchRestClient`.
   So the capability is already on the classpath; only the wiring is missing.
2. **Shadow `com.atlassian.jira.rest.client.internal.async.UriUtil`** with a fixed copy in this repo
   (PR [#754](https://github.com/jenkinsci/jira-plugin/pull/754)). Smallest possible diff. Rejected:
   classpath-order-dependent, undiscoverable from the call site, and it adds a second vendored-upstream
   tree to maintain on top of the one we already carry (see
   [0003](0003-future-of-the-vendored-atlassian-http-client.md)).
3. **Wait for the upstream fix**, then bump JRJC. Rejected on evidence: the 8.0.0 sources were decompiled
   and checked, and `CLOUD_DOMAINS` is *still* `["atlassian.net", "jira.com"]` there. There is nothing to
   wait for. This also removes the argument for rushing Renovate PR
   [#1170](https://github.com/jenkinsci/jira-plugin/pull/1170) (JRJC 8.0.0) — it does not fix #747.
4. **Bypass JRJC's search clients and call `search/jql` directly** from `JiraRestService`. Full control,
   including paging. Rejected as the first move: it is a much larger change to the hottest code path in the
   plugin, and it duplicates work JRJC 6.0.2 already does correctly once routing is fixed. Revisit only if
   cursor paging (see Consequences) proves impossible through the client API.

## Decision Outcome

Chosen option: **1, thread an explicit flag**, because the fix ships today on the pinned dependency, is
visible at the call site, and leaves the escape hatch somewhere a user can reach.

Implementation:

* `ExtendedAsynchronousJiraRestClient` takes a `boolean cloud` and passes it to
  `super(serverUri, httpClient, cloud)` instead of the two-argument constructor.
* The flag is threaded from `JiraSite` through `JiraSite.ExtendedAsynchronousJiraRestClientFactory`
  (nested in `JiraSite.java`) and `JiraSessionFactory.create(...)`.
* `JiraSite` computes it with a helper that widens the upstream heuristic to `atlassian.net`, `jira.com`
  **and** `api.atlassian.com`.
* A tri-state site-level override (auto / force Cloud / force Data Center) as a `@DataBoundSetter`, so
  vanity domains and reverse proxies are configurable rather than guessed. It must be hand-copied in
  `JiraSite.readResolve()` — that method rebuilds via a deprecated constructor and re-applies only a
  hardcoded subset of setters, so anything omitted is silently reset on load.
* WireMock coverage in `AbstractJiraRestServiceContractTest` asserting the request actually reaches
  `/rest/api/latest/search/jql` for a Cloud site and `/search` for a Data Center site — asserting on URL
  **and** request body, not merely that a call happened.
* PR [#754](https://github.com/jenkinsci/jira-plugin/pull/754) is closed as superseded.

### Consequences

* #747 is fixed on JRJC 6.0.2, with no dependency change and no shadowed classes.
* The heuristic is still a heuristic — but now it is *our* heuristic, in *our* code, with a documented
  override, instead of an invisible inherited one.
* `help-url.html` stops being a bug amplifier and its advice becomes safe to follow.
* Renovate PRs #1163 (6.0.4) and #1170 (8.0.0) are decoupled from this fix and can be judged on their own
  merits and their own schedule.
* Residual accepted risk — **paging is still broken on Cloud, and this ADR does not fix it.**
  `AsynchronousCloudSearchRestClient.searchJql(jql, maxResults, startAt, fields)` throws
  `UnsupportedOperationException` unless `startAt` is `null` or `0`. `JiraRestService` passes `0`, so the
  existing call site works — but that also means the plugin can only ever retrieve the *first* page from a
  Cloud instance. `JiraSite.maxIssuesFromJqlSearch` masks the symptom today. Cursor paging via
  `enhancedSearchJql(..., nextPageToken, ...)` is tracked as follow-up work, deliberately kept out of this
  change so the `410` fix stays small enough to ship on its own.
