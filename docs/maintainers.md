# For Maintainers

Notes for jira-plugin maintainers — release process and a couple of decisions that aren't obvious
from the code.

## Atlassian sources import

To resolve [some binary compatibility issues](https://github.com/jenkinsci/jira-plugin/pull/140),
sources from [`com.atlassian.httpclient:atlassian-httpclient-plugin:0.23`](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/)
are imported directly into this project to control the HTTP(S) transport layer. Those downloaded
sources had no license headers, but per their
[POM](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/atlassian-httpclient-plugin-0.23.0.pom)
(also vendored at `src/main/resources/atlassian-httpclient-plugin-0.23.0.pom`) they're
Apache-licensed.

## Releasing the plugin

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
