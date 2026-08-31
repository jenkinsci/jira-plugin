# Contributing to jira-plugin

Thanks for looking to contribute! This guide gets you from clone to a passing build as fast as
possible, then covers the deeper stuff (local Jenkins, live Jira testing, releasing) for when you
need it.

**In this guide:** [Quick start](#quick-start) · [Before you open a PR](#before-you-open-a-pr) ·
[Building & testing](#building--testing) · [Testing](#testing) ·
[Updating documentation](#updating-documentation) · [Running Jenkins locally](#running-jenkins-locally) ·
[For Maintainers](#for-maintainers)

## Quick start

```sh
git clone https://github.com/jenkinsci/jira-plugin.git
cd jira-plugin

# 1. Install the exact Java + Maven versions this project uses
curl https://mise.run | sh          # skip if you already have mise: https://mise.jdx.dev
mise install                        # reads mise.toml -> Java 21, latest Maven

# 2. Install the git hooks (auto-formatting + commit message checks)
brew install pre-commit
pre-commit install --install-hooks

# 3. Build and run the tests
mise exec -- mvn clean test
```

That's it — you're ready to make changes. See [Building & testing](#building--testing) for the
day-to-day commands, or jump straight to [Running Jenkins locally](#running-jenkins-locally) if you
want to click around a real instance with the plugin installed.

No [mise](https://mise.jdx.dev)? Any JDK 17+ and Maven on your `PATH` works fine too — just drop the
`mise exec --` prefix from every command below.

## Before you open a PR

- First time contributing to a Jenkins plugin? Skim the
  [general Jenkins development guide](https://www.jenkins.io/doc/developer/book/).
- Add tests for what you change — see [Testing](#testing) for this project's patterns.
- Adding a field to a persisted class (`JiraSite`, build steps, notifiers, ...)? Read
  [backward compatibility](https://www.jenkins.io/doc/developer/persistence/backward-compatibility/)
  and add a test for the old-data-loading path.
- **Update the docs in the same PR** — see [Updating documentation](#updating-documentation).
  Docs-only and pure-refactoring PRs are the obvious exception; say so in the description.
- Rejecting a reasonable alternative, or making a call that will look wrong without the history?
  Add an ADR — see [`adr/README.md`](adr/README.md).
- Open the PR as a **draft** first, let CI run, then mark it ready for review once checks are green.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`type(scope): subject`, e.g. `fix(rest): handle 404 from getIssue`; allowed types: `feat`, `fix`,
  `docs`, `style`, `refactor`, `test`, `chore`, `perf`) — the pre-commit hook from the Quick start
  enforces this for you.

## Building & testing

```sh
mise exec -- mvn clean test                     # full build + test suite
mise exec -- mvn test -Dtest=SomeTest           # a single test class
mise exec -- mvn spotless:apply                 # fix formatting by hand (the pre-commit hook does this for you)
```

<details>
<summary>What the pre-commit hooks do</summary>

`pre-commit install --install-hooks` (from the Quick start) wires up two checks that run
automatically on every commit, mirroring what CI checks:

| Hook | Runs on | What it does |
|---|---|---|
| `spotless-apply` | every `git commit` | Auto-formats staged Java files with Spotless — fixes the file, then asks you to re-`git add` and re-commit |
| `conventional-pre-commit` | the commit message | Rejects commit messages that aren't Conventional Commits |

</details>

## Testing

`src/test/java/hudson/plugins/jira/wiremock/` is this project's integration test suite: it
exercises `JiraRestService`/`JiraSession` against a real HTTP request/response wire format instead
of a mocked REST client, so the actual API contract is validated, not just the plugin's own code.
It runs against two backends:

- **`JiraRestServiceWireMockTest`** — a local [WireMock](https://wiremock.org/) server. Runs as
  part of the normal `mvn test`, fully offline, no Jira credentials needed. This is what you'll use
  day-to-day.
- **`LiveJiraCloudE2ETest`** — a real [Jira Cloud test instance](https://jenkins-jira-plugin.atlassian.net/).
  Opt-in and skipped by default; official maintainers can grant PR submitters access on a
  need-to-have basis. Useful for validating/refreshing the WireMock stub shapes when the Jira API
  contract changes:

  ```sh
  JIRA_LIVE_TEST=true \
  JIRA_LIVE_URL=https://jenkins-jira-plugin.atlassian.net/ \
  JIRA_LIVE_USER=you@example.com \
  JIRA_LIVE_TOKEN=your-api-token \
  JIRA_LIVE_PROJECT_KEY=SANDBOX \
  mise exec -- mvn test -Dtest=LiveJiraCloudE2ETest
  ```

  > [!WARNING]
  > This creates a **real** issue and version in `JIRA_LIVE_PROJECT_KEY` (the plugin has no delete
  > API to clean up with) — point it at a disposable/sandbox project, never production.

<details>
<summary>How the two-backend suite is wired, and how to add coverage for a new <code>JiraRestService</code> method</summary>

The test bodies live once in `AbstractJiraRestServiceContractTest`. Each backend subclass only
implements a handful of `given*`/`prepare*`/`assert*` hooks (build the `JiraSite`, make an
issue/version exist, verify a side effect happened) — the WireMock subclass registers stubs, the
live subclass creates/verifies real data. A change to a test method in
`AbstractJiraRestServiceContractTest` automatically applies to both, so there's nothing to keep in
sync by hand.

WireMock's stub response bodies are derived from Atlassian's official
[Jira Cloud platform OpenAPI spec](https://developer.atlassian.com/cloud/jira/platform/swagger-v3.v3.json),
trimmed to the fields the bundled `jira-rest-java-client-core` actually parses. `OpenApiSpecConformance`
checks every fixture body against that spec at test time (via
`com.atlassian.oai:openapi-request-validator-core`), so a fixture that drifts from the documented
contract fails the test that defines it instead of silently going stale.

The spec is **not downloaded**. A trimmed copy carrying only the paths these tests validate lives in
`src/test/resources/hudson/plugins/jira/wiremock/jira-cloud-platform-openapi-trimmed.json`, so the
suite really does run offline. Regenerate it with:

```sh
node tools/trim-jira-openapi-spec.mjs
```

**Adding coverage for another `JiraRestService` method:**

1. Add a test method to `AbstractJiraRestServiceContractTest`.
2. Implement its `given*`/`prepare*`/`assert*` hooks in both subclasses.
3. Call `OpenApiSpecConformance.assertConformsToSpec(...)` on the new WireMock fixture body before
   stubbing it.
4. If the fixture validates against a path the trimmed spec doesn't carry yet, add it to `KEPT_PATHS`
   in `tools/trim-jira-openapi-spec.mjs` and re-run the script — the test fails with that instruction
   rather than silently validating against nothing.

</details>

## Updating documentation

This documentation site (the folder this very file lives in) is a
[docsify](https://docsify.js.org/) site — plain Markdown, no build step. `index.html` loads docsify
from a CDN, and [`_sidebar.md`](_sidebar.md) is the nav; add an entry there whenever you add a new
page or ADR, or docsify won't surface it.

```sh
npm i -g docsify-cli   # one-time; see note below if this fails
docsify serve docs
```

Open `http://localhost:3000` and check your change renders as expected — headings, tables, code
fences (`groovy`/`sh`/etc. for syntax highlighting), relative links between pages, and any
[Mermaid diagrams](https://handbook.gitlab.com/handbook/tools-and-tips/mermaid/) you added. There's
no CI check or linter behind these files, so the local preview *is* the test.

> [!NOTE]
> `docsify@5.0.0`'s `postinstall` currently runs `npx husky install`, which can abort the global
> install outright — see [docsifyjs/docsify#2789](https://github.com/docsifyjs/docsify/issues/2789).
> If `npm i -g docsify-cli` fails, retry with `npm i -g docsify-cli --ignore-scripts`.

Before opening the PR: confirm the page is linked from `_sidebar.md`, and if you touched an ADR,
that it's still listed in the index table in [`adr/README.md`](adr/README.md).

## Running Jenkins locally

```sh
docker compose up -d --build --force-recreate
```

Spins up a Jenkins controller (`localhost:8080`) with the plugin installed, plus an SSH build agent
— configured via [Jenkins Configuration as Code](../casc.d) with local volumes for both, so you get a
clean, disposable environment every time.

The controller needs an SSH **private** key at `secrets/id_jenkins.pem`, whose matching public key
is what the `jenkins-agent` service trusts (`JENKINS_AGENT_SSH_PUBKEY` in `docker-compose.yml`).
There's no key checked into the repo, so generate your own pair:

```sh
mkdir -p secrets
ssh-keygen -t ed25519 -f secrets/id_jenkins.pem -N ""
```

Then replace the `JENKINS_AGENT_SSH_PUBKEY` value in `docker-compose.yml` with the contents of
`secrets/id_jenkins.pem.pub` — as a local, uncommitted change. Don't push someone else's key over
the placeholder, and don't commit your own private key.

## For Maintainers

Notes for jira-plugin maintainers — release process and a couple of decisions that aren't obvious
from the code.

### Atlassian sources import

To resolve [some binary compatibility issues](https://github.com/jenkinsci/jira-plugin/pull/140),
sources from [`com.atlassian.httpclient:atlassian-httpclient-plugin:0.23`](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/)
are imported directly into this project to control the HTTP(S) transport layer. Those downloaded
sources had no license headers, but per their
[POM](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/atlassian-httpclient-plugin-0.23.0.pom)
(also vendored at `src/main/resources/atlassian-httpclient-plugin-0.23.0.pom`) they're
Apache-licensed.

### Releasing the plugin

Releases are published via the [`cd.yaml`](.github/workflows/cd.yaml) GitHub Actions workflow
(JEP-229 continuous delivery) — no more local `mvn release:prepare`/`release:perform`.

From the Actions tab, run the "cd" workflow manually (`workflow_dispatch`) on `master` when it's
in a releasable state — merging alone never publishes. The workflow deploys whatever `<revision>`
in `pom.xml` currently is, with a short commit-hash suffix appended for traceability (e.g.
`3.23-abc123def456`), publishes the GitHub release, then automatically bumps `<revision>` to the
next value and pushes that to `master` — the direct (non-maven-release-plugin) replacement for the
old "prepare for next development iteration" commit. There's no manual version-bump step to
remember.

See [releasing Jenkins plugins with CD](https://www.jenkins.io/doc/developer/publishing/releasing-cd/).
The manual process remains documented as a fallback: see
[releasing Jenkins plugins manually](https://www.jenkins.io/doc/developer/publishing/releasing-manually/).
