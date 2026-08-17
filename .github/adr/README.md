# Architecture Decision Records

This directory holds the architecture decisions taken in this plugin — the ones where the *reason* matters
more than the diff, and where a future contributor might otherwise "fix" something on sight and reintroduce
the problem it was solving.

Records use the [MADR](https://adr.github.io/madr/) format. Start from the
[MADR decision record template](https://github.com/architecture-decision-record/architecture-decision-record/blob/main/locales/en/templates/decision-record-template-of-the-madr-project/index.md),
and follow the shape of the existing records here:

```markdown
# Short title, in the imperative or as a noun phrase

* Status: proposed | accepted | superseded by [ADR-NNNN](NNNN-....md)
* Date: YYYY-MM-DD

## Context and Problem Statement
## Decision Drivers
## Considered Options
## Decision Outcome
### Consequences
```

## Index

| # | Decision | Status |
|---|---|---|
| [0001](0001-sonarcloud-analysis-for-fork-prs.md) | SonarCloud analysis for pull requests from forks — a two-workflow split so fork PRs get analysis without `SONAR_TOKEN` reaching fork-controlled code | accepted |
| [0002](0002-jira-cloud-detection-and-search-api-routing.md) | Jira Cloud detection and search API routing — pass an explicit `isCloudVersion` to JRJC, widen the heuristic to `api.atlassian.com`, add a site-level override | proposed |
| [0003](0003-future-of-the-vendored-atlassian-http-client.md) | Future of the vendored Atlassian HTTP client — keep and fix the fork in 3.x (it carries Jenkins proxy support upstream lacks), target HttpClient 5 in 4.0 | proposed |
| [0004](0004-continuous-delivery-and-version-numbering.md) | Continuous delivery and version numbering — JEP-229 CD with a manually controlled `3.x` prefix and an on-demand trigger | proposed |
| [0005](0005-staged-deprecation-removal.md) | Staged deprecation removal — correctness and internals in 3.x, all API removals batched into one 4.0 | proposed |
| [0006](0006-http-client-lifecycle-and-jenkins-60536.md) | HTTP client lifecycle — why a client is built per request, and the conditions under which that may change | accepted |

## When to write one

Write a record when a decision is not obvious from the code, when you rejected a reasonable alternative for a
reason worth keeping, or when the code will look wrong to someone who does not know the history. ADR
[0006](0006-http-client-lifecycle-and-jenkins-60536.md) is the archetype: the code it describes reads as a
plain bug, has been "fixed" in both directions more than once, and only the written reason breaks the loop.

Numbering is sequential and never reused. Do not edit an accepted record to change its decision — add a new
one and mark the old `superseded by`.
