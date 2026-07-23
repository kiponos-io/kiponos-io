---
title: "Drain Windows Are Incident Posture — Make Them Live"
published: false
tags: java, sre, kubernetes, kiponos
description: "Live graceful shutdown window via Kiponos — longer drains under load without a new image."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sre-graceful-shutdown-ms.md
main_image: https://files.catbox.moe/v0zoo5.jpg
---

**The Aha:** `drain-ms` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

Kubernetes terminationGracePeriodSeconds is not the only drain story. App-level shutdown budgets still hide in constants until a deploy.

## The problem: ceremony between judgment and effect

Rolling restart under peak. Connections need **more** time to drain. Your process still exits on last quarter's budget.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: HTTP servers, consumers, batch workers.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  sre-graceful-shutdown-ms/
    drain-ms: 15000
```

```java
Folder policy = kiponos.path("examples", "sre-graceful-shutdown-ms");
long drain = policy.getLong("drain-ms");
server.shutdown(drain, TimeUnit.MILLISECONDS);
```

Ops sets `drain-ms` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
Human / automation ──set drain-ms──▶ Kiponos.io hub
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
cd kiponos-io/examples/java/sre-graceful-shutdown-ms
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms)

Try it tonight:

1. Run tests — prove default `15000` is coherent.  
2. Change `drain-ms` in the hub — confirm the next run applies the new value without rebuild.  
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

## Drain windows are incident posture

`gracefulShutdownMs` is how long you let in-flight work finish before the process dies. Too short: dropped requests. Too long: stuck deploys and blocked autoscaling.

## Why live beats YAML

During an incident you may need to:

- **Lengthen** drains so payments finish while you fence traffic  
- **Shorten** drains so a bad pod stops holding a LoadBalancer target  

Neither desire should wait for a green pipeline.

## Pair with readiness

A live drain is useless if readiness still says "up" forever. Coordinate:

1. Mark not-ready (stop new traffic).  
2. Apply drain window from hub.  
3. Wait ≤ drain for in-flight.  
4. Exit.

Log the **chosen** drain on each shutdown so postmortems stop guessing.

## Clamps

- Compiled floor (never 0 on money paths unless explicit kill)  
- Compiled ceiling (never 30 minutes because someone typed extra zeros)  
- LKG when hub unreachable during SIGTERM (process still must exit)

Drain is posture between **user completion** and **cluster agility**.


## Moral

Shutdown posture is part of reliability. Fossils in the jar are **unreliability with better logging**.

Ship judgment. Leave the jar alone.

---

*Runnable: [sre-graceful-shutdown-ms](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
