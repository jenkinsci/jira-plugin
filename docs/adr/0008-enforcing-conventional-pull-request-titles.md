# Enforcing Conventional Commit pull request titles in CI

* Status: accepted
* Date: 2026-09-07

## Context and Problem Statement

Three places in this repository state the same rule: PR titles and commit messages are Conventional
Commits, with type one of `feat|fix|docs|style|refactor|test|chore|perf`. They are
`.pre-commit-config.yaml`, `AGENTS.md` and `docs/CONTRIBUTING.md`.

Only one of them enforces anything, and it enforces the wrong artifact in the wrong place. The
`conventional-pre-commit` hook checks **commit messages**, on the contributor's machine, and only
after they have run `pre-commit install`. It never runs in CI. Nothing at all checks the **PR
title**, which is what actually reaches `master`, because this repository squash-merges.

The result is visible in the history. Of the last 100 pull requests, 67 titles do not match
`^(feat|fix|docs|style|refactor|test|chore|perf)(\([a-z0-9-]+\))?!?: `. Almost all of that is bot
traffic: Renovate at 29 of 29, Dependabot at 18 of 19 before it was retired, and the Crowdin
workflow's hardcoded `Update localization`. Human titles conform in recent history and drift in
older history.

Two further details decide the shape of the fix:

* The shared `github>jenkinsci/renovate-config` preset extends `:semanticCommitsDisabled`, so
  Renovate is configured org-wide to *not* produce `chore(deps): ...` titles.
* This repository's `squash_merge_commit_title` setting is `COMMIT_OR_PR_TITLE`, not `PR_TITLE`.
  When a PR contains exactly one commit, GitHub squashes using that **commit's** subject and ignores
  the PR title entirely.

## Decision Drivers

* The rule should hold for every PR, so that `git log master` is uniformly parseable.
* Enforcement belongs where it cannot be skipped, not in an opt-in local hook.
* A check must not need secrets or a checkout of untrusted code, per
  [0001](adr/0001-sonarcloud-analysis-for-fork-prs.md).
* Bot PRs are the majority of traffic; whatever is done has to work for them without a permanent
  exemption that hollows out the rule.

## Considered Options

* **Leave it to the pre-commit hook.** Costs nothing, changes nothing: the hook is opt-in and does
  not see PR titles.
* **Check the title in CI, exempt the bots.** Simple and immediate, but leaves the majority of
  commits on `master` non-conventional, which is most of what the rule was for.
* **Check the title in CI, and make the bots conform.** Chosen.

## Decision Outcome

`.github/workflows/pr-title.yml` runs `amannn/action-semantic-pull-request` on every pull request,
with **no bot exemption**. It triggers on `opened`, `edited`, `reopened` and `synchronize`, so
renaming an open PR re-runs the check. It uses plain `pull_request` with `permissions: pull-requests: read`,
needing neither a checkout nor a secret.

`validateSingleCommit` is on. That is not belt-and-braces: with `squash_merge_commit_title` set to
`COMMIT_OR_PR_TITLE`, a single-commit PR with a well-formed title but a sloppy commit subject lands
the sloppy subject. Setting the repository to `PR_TITLE` in GitHub's merge settings would close the
same hole and let this option be turned off, but that is a click in the UI that nothing in the
repository records, so the check does not depend on it.

The bots are fixed at the source rather than exempted:

* `.github/renovate.json` sets `"semanticCommits": "enabled"`, overriding the
  `:semanticCommitsDisabled` it inherits from `github>jenkinsci/renovate-config`. Renovate then
  titles its PRs `chore(deps): update ...`. **This divergence from the shared jenkinsci preset is
  deliberate**; removing it to "match the org default" reintroduces the failures.
* `.github/workflows/crowdin.yml` titles its generated PR `chore(l10n): update localization`.

Neither change affects release notes: `.github/release-drafter.yml` categorises by **label**, and
both bots keep their labels (`dependencies`, `localization`).

### Consequences

* Every PR, human or bot, now has a parseable subject on `master`, which is the precondition for
  ever generating a changelog from history rather than by hand (see `docs/roadmap.md`).
* Contributors get the failure on the pull request instead of at `git commit`, which is later but
  reaches everyone rather than only those who ran `pre-commit install`.
* The allowed-type list now lives in four places rather than three. `.github/workflows/pr-title.yml`,
  `.pre-commit-config.yaml`, `AGENTS.md` and `docs/CONTRIBUTING.md` must be changed together; each
  carries a comment saying so.
* The check reports as **Conventional Commits** and does not block merging until it is added to the
  branch protection rules for `master`.
