---
title: "Velocity Caps Are Fraud Posture — Stop Shipping Them as Constants"
published: false
tags: java, fintech, security, kiponos
description: "Live transaction velocity cap via Kiponos — fraud ops without a payment service redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-fintech-velocity-cap.md
main_image: ./devto-cover-fintech-velocity-cap.jpg
---

**The Aha:** `max-per-hour` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

Fraud teams watch velocity like weather. Engineering still treats the cap like a compile-time constant.

## The problem: ceremony between judgment and effect

Card testing rings spike. Fraud wants velocity-cap cut in half **this minute**. Payment pods are mid-canary for an unrelated change.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: Card auth, wallet top-ups, P2P rails.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  fintech-velocity-cap/
    max-per-hour: 12
```

```java
Folder policy = kiponos.path("examples", "fintech-velocity-cap");
int cap = policy.getInt("max-per-hour");
if (countLastHour(card) > cap) {
    return RouteDecision.review("velocity");
}
```

Ops sets `max-per-hour` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
Human / automation ──set max-per-hour──▶ Kiponos.io hub
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
cd kiponos-io/examples/java/fintech-velocity-cap
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fintech-velocity-cap](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fintech-velocity-cap)

Try it tonight:

1. Run tests — prove default `12` is coherent.  
2. Change `max-per-hour` in the hub — confirm the next run applies the new value without rebuild.  
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

## Moral

If fraud cannot move a number without a jar, you do not have live fraud ops — you have **scheduled regret**.

Ship judgment. Leave the jar alone.

---

*Runnable: [fintech-velocity-cap](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fintech-velocity-cap) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
