---
title: "Feature Flags Gate Product — Config Hubs Steer Ops Posture"
published: false
tags: java, architecture, devops, kiponos
description: "When to use feature flags vs a live config hub like Kiponos."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-feature-flags-vs-config-hub.md
main_image: https://files.catbox.moe/y5ugzw.jpg
---

**The Aha:** `rps` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

Conflating product gates with ops dials is how you buy two control planes and still redeploy for RPS.

Domain: Platform architecture, flag vendors vs ops hubs.

## Rough split

| Concern | Tool |
|---------|------|
| Incomplete feature, audience targeting | Feature flag |
| Retry budget, RPS, canary %, drain ms | Config hub |
| Secrets | Secret manager |

Local get on the hub path keeps hot paths honest. Do not RTT the flag service per request for pool size either — same lesson.


## The problem: ceremony between judgment and effect

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```java
Folder policy = kiponos.path("examples", "arch-feature-flags-vs-config-hub");
int value = policy.getInt("rps");
// use value on the decision path
```

Ops sets the key in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar.

## What stays in the jar vs the hub

| Jar (versioned) | Hub (live) |
|-----------------|------------|
| Code paths & clamps | Operational numbers |
| Hard maxima | Current posture |
| Schema & types | Human judgment under pressure |

## Architecture

```
Dashboard / automation
        │ write
        ▼
   Kiponos hub tree
        │ delta
        ▼
  Java SDK in-process cache ──► local get on hot path
```

No sidecar tax on every request. No second product for "just this one dial."

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/arch-feature-flags-vs-config-hub   # or nearest aha-* sibling
# follow README — KIPONOS_ID / KIPONOS_ACCESS for sandbox
```

Unit-test with fixed strings. Integration-test against the public sandbox when you can.

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

Feature flags are often product gates. This essay is about **ops posture on an architecture concern** — pools, cutovers, retries, circuits, canaries, thresholds — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## A note on testing

Unit-test structure with fixed strings (no network). Integration-test the hub path against the public sandbox when you can. Good tests: defaults when keys are missing; clamps; fail-closed on money paths. Bad tests: hitting production hubs from CI.

## Two planes, one hot path rule

Whatever tool you pick, **local memory** on the decision path. Remote evaluate-per-request is a latency and outage story.

## Buy vs build clarity

Flag vendors excel at targeting and experiments. Ops hubs excel at steerable numbers with audit. Using one for the other creates awkward shoes.

## Team ownership

Product owns flag keys. SRE/platform owns hub posture keys. Document the split so on-call does not file the wrong ticket at 3am.



## Closing

Architecture diagrams do not absorb incidents. Steerable posture does.


## War-room protocol

1. **Name the path** in the runbook before the incident (`examples/<stem>/<key>`).  
2. **State the clamp** out loud (min/max) before anyone types.  
3. **Write the reason code** with the change (`sev`, `peak`, `cost`, `drill`).  
4. **Watch two signals** for five minutes (user SLO + dependency health).  
5. **Revert or step** — never leave an experimental value as the silent new normal.  
6. **Postmortem line:** who moved what, from→to, and whether automation should own it next time.

## Defaults when the hub is dark

| Path class | Hub unreachable default |
|------------|-------------------------|
| Money / fraud | Fail closed or conservative floor |
| Ingress RPS | Last-known-good or safe mid |
| Canary share | 0% (stable binary only) |
| Observability sample | Modest baseline (not zero on audit paths) |
| Drain window | Compiled mid (still exit) |

Dark-hub behavior is part of the design, not an afterthought.

## Related reading in this library

- Aha series: retries, RPS, pools, sampling — same Super Pattern spine  
- Super Patterns: GoF shapes with live policy objects  
- Product: [kiponos.io](https://kiponos.io) · [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

If you only remember one architecture sentence: **version the code paths; live-edit the posture.**


## Moral

Flags ship product. Hubs ship judgment on hot paths.

Ship judgment. Leave the jar alone.

---

*Runnable examples: [kiponos-io/examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
