---
title: "We Used to Redeploy to Change Retry Budget — Then the Outage Ate the Pipeline"
published: false
tags: java, resilience, devops, kiponos
description: "Live max-attempts for retries via Kiponos — stop amplifying outages with a jar-shaped budget."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-retry-max-attempts.md
main_image: https://files.catbox.moe/1s7ykc.jpg
---

**The Aha:** `max-attempts` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

There is a particular graph that makes senior engineers stop joking: error rate flat, but **downstream load climbing** because every client retries with the same stubborn budget.

## The problem: ceremony between judgment and effect

Someone set max-attempts=8 in application.yml last quarter for a flaky partner. Tonight that partner is actually down. Your retry budget is a **DDoS you scheduled yourself**.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: Partner APIs, payment acquirers, SMS gateways — any place retry multiplies pain.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  aha-retry-max-attempts/
    max-attempts: 3
```

```java
Folder policy = kiponos.path("examples", "aha-retry-max-attempts");
int max = policy.getInt("max-attempts"); // local get
for (int i = 0; i < max; i++) {
    try { return callPartner(); }
    catch (TransientException e) { backoff(i); }
}
throw last;
```

Ops sets `max-attempts` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

## What stays in the jar vs the hub

| Versioned (jar) | Live (hub) |
|-----------------|------------|
| How the knob is applied | The number / enum itself |
| Allowlists / clamps | Day-to-day posture |
| Metrics and audit lines | Temporary incident values |
| Secrets | **Never** in the hub |

Clamps matter: refuse absurd values even if someone types them in a panic.

## Architecture

```text
Human / automation ──set max-attempts──▶ Kiponos.io hub
                                      │ WebSocket delta
                                      ▼
                                 Java SDK cache
                                      │ get() local
                                      ▼
                                 request / job path
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/aha-retry-max-attempts
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-retry-max-attempts](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-retry-max-attempts)

Try it tonight:

1. Run tests — prove default `3` is coherent.  
2. Change `max-attempts` in the hub — confirm the next run applies the new value without rebuild.  
3. Time your real release path vs a hub write.  
4. Write the runbook sentence: *who is allowed to move this key under P1?*

## Scenarios

| Moment | Frozen YAML | Live hub |
|--------|-------------|----------|
| Incident | PR + pipeline | Seconds |
| Peak event | Over-provision | Dial down/up |
| Experiment | Long-lived branch | Same jar |
| Rollback | Redeploy previous | Revert hub value |

## When not to live-edit

- Protocol or schema changes that need coordinated rollouts  
- Values that compliance requires code-reviewed only  
- Anything you cannot clamp or allowlist safely  

Live knobs are for **posture**, not for inventing untested systems under fire.

## Operational checklist

1. Name the hub path so humans find it under pressure.  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging with this example module.  
6. Document the one-line kill path (revert key).

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture on a hot path**: retries, RPS, fraud velocity, canaries, drains, sampling — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## A note on testing

Unit-test structure with fixed strings (no network). Integration-test the hub path against the public sandbox when you can. Good tests: defaults when keys are missing; clamps; fail-closed on money paths. Bad tests: hitting production hubs from CI.

## Failure budget vs retry budget

Retries spend **downstream capacity**. Every extra attempt is a vote that the dependency should absorb more load while it is already sick.

Treat `maxAttempts` as a slice of the **error budget**, not a comfort blanket:

| Signal | Raise attempts? | Prefer instead |
|--------|-----------------|----------------|
| Transient blips, healthy p99 | Cautious +1 | Jitter + shorter timeout |
| Dependency already 5xx-storming | **No** | Shed load, open circuit |
| Client timeouts cascading | **No** | Fail faster, protect queue |
| Idempotent read path | Maybe | Cap total wall clock |

Write the number that survives a **bad day**, not the number that flatters a sunny demo.

## Observability you actually need

Ship three counters with the key path baked in:

1. `retry_attempts_total{path,attempt}` — histogram of how deep you go  
2. `retry_exhausted_total{path}` — the only number leadership should screenshot  
3. `retry_decision_changes_total` — hub writes that moved the dial  

When exhausted spikes and nobody moved the hub, the outage is **under-instrumented judgment**, not "config debt."

## Anti-patterns we retired

- Shipping `maxAttempts=10` "just in case" on payment writes  
- Coupling retry count to deploy cadence ("we'll tune next sprint")  
- Logging every get of the hub value (noise) instead of every **change**  
- Using retries to paper over a missing circuit breaker  

The Super Pattern here is simple: **budget is posture**. Posture that waits for a jar is already late.


## Moral

Retry is not loyalty. Super-pattern thinking: **budget is posture**, not a fossil in the jar.

Ship judgment. Leave the jar alone.

---

*Runnable: [aha-retry-max-attempts](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-retry-max-attempts) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
