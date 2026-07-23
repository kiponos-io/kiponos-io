---
title: "Ingress RPS Was a Ticket — We Made It a Live Dial Before the Bot Farm Finished"
published: false
tags: java, security, devops, kiponos
description: "Live ingress RPS cap via Kiponos Java SDK — throttle without a redeploy mid-attack."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-rate-limit-rps.md
main_image: https://files.catbox.moe/y0prh4.jpg
---

**The Aha:** `rps` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

The bot farm does not wait for your change-management window. Your rate limit does — unless it lives in a hub.

## The problem: ceremony between judgment and effect

YAML rate-limit looked fine at launch. At 02:14 the scrape farm finds you. SRE wants 50 RPS **now**. CI is green in 40 minutes.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: Edge filters, public APIs, webhook receivers.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  aha-rate-limit-rps/
    rps: 120
```

```java
Folder policy = kiponos.path("examples", "aha-rate-limit-rps");
int rps = policy.getInt("rps");
if (!limiter.tryAcquire(rps)) {
    return tooManyRequests();
}
return chain.proceed(req);
```

Ops sets `rps` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
Human / automation ──set rps──▶ Kiponos.io hub
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
cd kiponos-io/examples/java/aha-rate-limit-rps
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-rate-limit-rps](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-rate-limit-rps)

Try it tonight:

1. Run tests — prove default `120` is coherent.  
2. Change `rps` in the hub — confirm the next run applies the new value without rebuild.  
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

## Bot farms and honest customers share the same dial

Ingress RPS is not only anti-abuse. It is also **fairness** during flash sales and partner spikes. One number serves three masters:

1. **Security** — starve scrapers before they finish the catalog  
2. **SRE** — keep p99 inside SLO when a dependency limps  
3. **Product** — prefer graceful 429 over silent 503 death spirals  

If those three orgs file three tickets to change three YAML copies, you already lost.

## Where to enforce

Prefer the earliest durable hop that still knows identity:

| Layer | Pros | Cons |
|-------|------|------|
| Edge/WAF | Cheap rejection | Weak app identity |
| API gateway | Central policy | Another control plane |
| Service middleware + hub | Local get, live write | Must clamp in-process |

Kiponos sits where the **decision is made**: middleware reads `rps` from memory; edge can still hard-cap a catastrophic upper bound that only changes with a real release.

## Incident script (paste into runbook)

1. Confirm scrape or overload (rate of 429 vs origin errors).  
2. Drop hub `rps` to the emergency floor (documented number, not vibes).  
3. Watch `retry_exhausted` / queue depth for 2 minutes.  
4. Raise in steps of 10–20% once origin recovers.  
5. Write the postmortem line: *who moved the dial, from→to, why*.  

No PR. No image rebuild. No "we'll catch the next deploy window."


## Moral

Rate limits that require a deploy are just **optimistic documentation**.

Ship judgment. Leave the jar alone.

---

*Runnable: [aha-rate-limit-rps](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-rate-limit-rps) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
