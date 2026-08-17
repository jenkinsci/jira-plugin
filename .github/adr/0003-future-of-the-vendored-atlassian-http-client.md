# Future of the vendored Atlassian HTTP client

* Status: proposed
* Date: 2026-08-17

## Context and Problem Statement

`src/main/java/com/atlassian/httpclient/**` is 16 files and ~1888 lines of Atlassian's
`atlassian-httpclient-plugin:0.23` copied into this repository. `CONTRIBUTING.md` records why: it was
imported to resolve [binary compatibility issues](https://github.com/jenkinsci/jira-plugin/pull/140) and to
control the HTTP(S) transport layer.

Carrying it has a real cost. The tree gets no upstream security or bug fixes, it is excluded from static
analysis (`pom.xml` scopes SpotBugs with `<onlyAnalyze>hudson.plugins.jira.*</onlyAnalyze>`), it has almost
no test coverage, it has no license headers, and it is built on Apache HttpClient 4 / httpcore-nio, whose
`SSLContextBuilder.useTLS()`, `X509HostnameVerifier` and `BROWSER_COMPATIBLE_HOSTNAME_VERIFIER` have been
deprecated since HC 4.4.

PR [#755](https://github.com/jenkinsci/jira-plugin/pull/755) proposes deleting the tree and depending on
`atlassian-httpclient-library` again. That is the obviously attractive move, and this ADR exists because it
is not safe as written.

**The vendored tree is not a pure copy of upstream.** It carries Jenkins-specific behaviour upstream does
not have:

* `ApacheAsyncHttpClient` imports `hudson.ProxyConfiguration` (l.20) and `jenkins.model.Jenkins` (l.41) —
  the *only* two Jenkins imports anywhere in the vendored tree.
* It reads the controller's proxy configuration (l.213) and installs a nested `JenkinsProxyRoutePlanner`
  (l.235) honouring `ProxyConfiguration.getNoProxyHostPatterns()` (l.228).
* `src/test/java/hudson/plugins/jira/JiraRestServiceProxyTest.java` covers exactly this.

Dropping to upstream therefore silently regresses Jenkins proxy support for every user behind a corporate
proxy — a population that overlaps heavily with self-hosted Jira Data Center users. PR #755 is also
conflicting and roughly nine months stale.

So: what happens to the fork?

## Decision Drivers

* Never regress Jenkins proxy support. It is the fork's whole reason for existing and it is exercised by a
  test that would keep passing only because the test would be deleted alongside the code.
* A modernisation programme that has to fix real correctness bugs in this tree (see
  [0006](0006-http-client-lifecycle-and-jenkins-60536.md)) cannot also be blocked on replacing it.
* Deprecated HC4 TLS APIs and an unanalysed, untested 1888-line tree are not an acceptable end state.
  "Keep the fork" must not mean "keep ignoring the fork".
* Breaking changes belong in 4.0, not in a 3.x patch — see
  [0005](0005-staged-deprecation-removal.md).

## Considered Options

1. **Keep the fork, fix it in place for 3.x; target HttpClient 5 in 4.0.** Bring it under SpotBugs, add
   license headers, fix the lifecycle and timeout bugs, and treat `apache-httpcomponents-client-5-api` as
   the 4.0 destination.
2. **Adopt upstream `atlassian-httpclient-library` and re-add proxy support on top**, e.g. by subclassing or
   by configuring a route planner from outside. Removes ~1888 lines from this repo. Rejected *for now*, not
   on principle: whether the proxy planner can be injected without patching internals is unverified, and the
   verification work is larger than the 3.x correctness fixes it would compete with. If someone establishes
   that upstream exposes a route-planner seam, this becomes the better option and this ADR should be
   superseded.
3. **Merge PR #755 as-is.** Rejected: regresses Jenkins proxy support, as above.
4. **Migrate straight to `apache-httpcomponents-client-5-api` now.** The right destination, wrong moment.
   HC5 changes the async API shape (`SimpleHttpRequest`, `HttpAsyncRequester`, reactor lifecycle), so it is a
   rewrite of the transport layer — landing it in the middle of the Cloud-search fix
   ([0002](0002-jira-cloud-detection-and-search-api-routing.md)) and the client-lifecycle fix
   ([0006](0006-http-client-lifecycle-and-jenkins-60536.md)) would make all three unreviewable at once.

## Decision Outcome

Chosen option: **1, keep and fix for 3.x, with HttpClient 5 as the declared 4.0 target**, because it is the
only option that lets the correctness work proceed immediately without betting proxy support on unverified
assumptions.

Implementation:

* **Document why the fork exists, in the code.** The Jenkins-specific divergence is currently discoverable
  only by reading `ApacheAsyncHttpClient` line by line. Add a package-level comment naming the proxy
  integration as the reason, pointing here and at `JiraRestServiceProxyTest`.
* **Add Apache-2.0 license headers** to all 16 files. Upstream shipped none, but the vendored
  `src/main/resources/atlassian-httpclient-plugin-0.23.0.pom` establishes the license.
* **Widen `<onlyAnalyze>`** in `pom.xml` to include `com.atlassian.httpclient.*`, and burn down what
  SpotBugs reports — as its own PR, so the diff-scoped SonarCloud gate stays legible. `RedirectStrategy`
  and `DefaultHttpClientFactory` are the security-adjacent ones.
* **Fix the known defects in place**: the broken `DefaultHttpClientFactory` singleton (non-volatile static
  read outside the lock, `synchronized (this)` on a per-call-site instance, no re-check inside the lock,
  `destroy()` NPE) which pins every site to the *first* site's `HttpClientOptions`; the unread
  `requestTimeout`; the never-configured connect timeout; the `finalize()` override; the deprecated HC4 TLS
  APIs; the dead `if (Jenkins.get() != null)` guard; the never-read `CompletableFuture` in
  `CompletableFuturePromiseHttpPromiseAsyncClient`; and the ~76 lines of event plumbing whose only consumer
  is a no-op publisher.
* **Close PR #755** with this rationale, explicitly inviting a rework along option 2 — the contributor's
  instinct is right, only the proxy seam is unaccounted for.
* **4.0 targets `apache-httpcomponents-client-5-api`.** That is also the natural moment to drop fugue,
  jettison and the pre-Jakarta `javax.ws.rs` usage in `hudson.plugins.jira.extension`.

### Consequences

* Proxy support is preserved, and for the first time it is documented as load-bearing rather than incidental.
* The correctness work in [0006](0006-http-client-lifecycle-and-jenkins-60536.md) is unblocked immediately.
* The tree enters static analysis, so its findings become visible instead of theoretical. Expect an initial
  batch — that is the point.
* We keep maintaining ~1888 lines of third-party code for at least one more major version, including its
  deprecated HC4 TLS surface. This is the explicit price of the decision, not an oversight.
* Residual accepted risk: `trustSelfSignedCertificates()` in the vendored client disables hostname
  verification entirely, not merely CA validation. Until the HC5 migration this stays true, so the site
  help text must say so plainly — a user enabling it for a self-signed certificate is also silently opting
  out of hostname checks.
