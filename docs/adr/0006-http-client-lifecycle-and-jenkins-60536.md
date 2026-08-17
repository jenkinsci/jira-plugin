# HTTP client lifecycle: why a client is built per request (JENKINS-60536)

* Status: proposed
* Date: 2026-08-17

## Context and Problem Statement

`ApacheAsyncHttpClient.getPromiseHttpAsyncClient(Request)` declares a **local**
`CloseableHttpAsyncClient` that shadows the instance field of the same name, builds it, and starts it — on
every call. Each Jira REST request therefore pays for a fresh `DefaultConnectingIOReactor`, a fresh
`PoolingNHttpClientConnectionManager`, `ioThreadCount` new daemon threads and a new `SSLContext`, with zero
connection reuse and a full TLS handshake. `CompletableFuturePromiseHttpPromiseAsyncClient` then closes that
client in the completion callback. The instance field built in the constructor executes nothing and is only
touched by `destroy()`.

Read cold, this looks unambiguously like a bug — and specifically like an accidental regression, because two
commits exist purely to make the client a singleton: `3eb65b0` ("use only one http client instance…") and
`b4410be` ("really use only one httpclient instance (my bad!)"). The obvious fix is to execute against the
instance client and delete the per-request build.

**That reading is wrong, and acting on it reintroduces a hang.**

Tracing the code: the exact local-shadowing body was authored in the `xft-devs/jira-plugin` fork, whose first
two commits are titled *"Proof of concept workaround for connection freeze seen in JENKINS-60536"*. The same
change landed upstream as commit `9e3b451` (PR [#230](https://github.com/jenkinsci/jira-plugin/pull/230),
April 2020), titled *"[JENKINS-60536] bugfix: Jenkins job hangs in Jira call after git rev-list command"*.

So the current design — one client per request, closed on completion — is a **deliberate, shipped fix for a
liveness failure**, adopted from a fork's self-declared proof of concept and never finished. It does undo
`3eb65b0` and `b4410be`, but it was traded for liveness, not lost by accident. Nothing in the code says so,
which is why this ADR exists: the code has now ping-ponged between "shared client" and "client per request"
three times, each time fixing one failure mode by reintroducing the other.

## Decision Drivers

* Never silently reintroduce JENKINS-60536. A hung Jira call hangs a build, and the symptom appears long
  after the change that caused it.
* Per-request reactor, thread-set and TLS handshake construction is a real and ongoing cost that should not
  be permanent.
* Whatever the eventual shape, the *reason* must be recorded in a place a contributor reads before deleting
  the code — the two prior reverts happened because it was not.
* A regression test that guards only one of the two invariants is what enabled the ping-pong.

## Considered Options

1. **Keep the per-request client for now; require JENKINS-60536 to be root-caused before replacing it, and
   require a two-sided regression test when it is.** Documents the trade, sets an explicit exit condition.
2. **Revert to the shared instance client now.** Restores connection reuse and honours the intent of
   `3eb65b0` / `b4410be`. Rejected: this is precisely what PR #230 was written to fix, and there is no
   evidence the underlying cause was ever addressed. It would be the fourth iteration of the same loop.
3. **Pooled client per `JiraSite` with idle eviction.** Bounded cost, and an idle-eviction policy plausibly
   sidesteps whatever wedges a long-lived reactor. Kept as the declared fallback if root-causing
   JENKINS-60536 proves open-ended — it preserves the liveness property without paying per request.
4. **Leave it undocumented and fix it opportunistically later.** Rejected: this is the status quo, and the
   status quo is what produced three contradictory attempts.

## Decision Outcome

Chosen option: **1, keep and document, with an explicit exit condition**, because the cost is real but
bounded, whereas reverting without understanding the hang has already failed twice.

Implementation:

* **A comment at `getPromiseHttpAsyncClient` naming JENKINS-60536, commit `9e3b451` and this ADR**, so the
  next contributor to read that method learns why before deleting it. This is the load-bearing part of the
  decision.
* The per-request build may be replaced only when **both** hold:
  1. JENKINS-60536 is reproduced and root-caused. The current suspects are the shared
     `JiraSite.executorService` — `static`, but sized from the *instance* `threadExecutorNumber`, read
     outside its own `synchronized` block, non-daemon, never shut down, and passed to
     `ApacheAsyncHttpClient.destroy()`, which calls `shutdown()` on it and would thereby break Jira for
     every site on the controller — and the callback handoff in
     `CompletableFuturePromiseHttpPromiseAsyncClient.execute`, which builds its promise from `clientFuture`
     while completing a `CompletableFuture` nobody ever reads.
  2. A **two-sided** regression test lands in the same PR: the async client is constructed **once** across
     N requests, *and* a request issued after a long idle gap still completes. Both directions or neither —
     a single-construction assertion alone walks the code straight back to the 2019 hang.
* If (1) proves open-ended, take option 3 (pooled client per `JiraSite` with idle eviction) rather than
  leaving the per-request build permanent.
* Bringing the vendored tree under SpotBugs (see
  [0003](adr/0003-future-of-the-vendored-atlassian-http-client.md)) will flag this method. Suppress it with a
  pointer to this ADR rather than "fixing" it, until the exit condition is met.

### Consequences

* The trade is now explicit, and a contributor who deletes the per-request build has to argue against a
  written decision rather than against an apparent typo.
* Every Jira REST call keeps paying for a new IO reactor, thread set and TLS handshake until the exit
  condition is met. Accepted knowingly: for the request volumes a Jira plugin generates this is wasteful
  rather than dangerous, and a hang is worse than slow.
* `DefaultHttpClientFactory`'s broken singleton — which pins every site to the *first* site's
  `HttpClientOptions` — is a **separate** bug and is *not* covered by this ADR. It must be fixed regardless,
  and fixing it does not require touching the per-request build.
* The related timeout defects are also independent and should be fixed now: `HttpClientOptions.requestTimeout`
  is set by `JiraSite` but never read by the client builder (so the UI "Read timeout" has no HTTP-layer
  effect — it only bounds `Future.get(...)`), connect timeout is never configured at all, and
  `getMaxTotalConnections()` is never applied.
* Residual accepted risk: JENKINS-60536 may not be reproducible on demand — it was reported as a freeze after
  a long-running external process, which is timing-dependent. If it cannot be reproduced, option 3 is the
  answer, not a hopeful revert.
