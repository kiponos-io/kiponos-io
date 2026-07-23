---
title: "Canary Percent Is a Steering Wheel — Stop Welding It to the Release"
published: false
tags: java, sre, devops, kiponos
description: "Live canary traffic percent via Kiponos — steer rollouts without another pipeline run."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sre-canary-percent.md
main_image: https://files.catbox.moe/f3o9ne.jpg
---

**The Aha:** `percent` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

Canary is supposed to be a steering wheel. Most teams weld the percent into the pipeline YAML and call it progressive delivery.

## The problem: ceremony between judgment and effect

Error budget is bleeding on the 10% canary. You want 2% **immediately**. The number is right there — frozen.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: Service mesh weights, edge routers, feature audiences.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  sre-canary-percent/
    percent: 10
```

```java
Folder policy = kiponos.path("examples", "sre-canary-percent");
int pct = policy.getInt("percent");
return router.canary(userId, pct) ? next : stable;
```

Ops sets `percent` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
Human / automation ──set percent──▶ Kiponos.io hub
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
cd kiponos-io/examples/java/sre-canary-percent
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-canary-percent](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-canary-percent)

Try it tonight:

1. Run tests — prove default `10` is coherent.  
2. Change `percent` in the hub — confirm the next run applies the new value without rebuild.  
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

## Canary percent is a steering wheel

Welding canary share to the release artifact means every traffic move is another pipeline. That is how teams "skip canary" under pressure — the process became the enemy.

## Live share, frozen binary

Ship **one** canary-capable build. Steer `canaryPercent` from the hub:

- 1% → 5% → 25% → 100% without rebuilding  
- Instant rollback: set share to 0 while you keep the bad binary offline  
- Progressive delivery tools can write the same hub path your humans use  

The jar answers *what code can run*. The hub answers *how much traffic tastes it*.

## Guardrails

- Max step size (no 1% → 100% in one click without break-glass)  
- Automatic freeze if error budget burn exceeds threshold (automation writes 0)  
- Separate folders for prod vs staging so drills do not bleed  

## What stays in the release

- Feature code itself  
- Schema migrations  
- Feature flags that gate unfinished product (different problem)

Canary **share** is SRE posture. Treat it like a steering wheel, not a welded strut.


## Moral

If you cannot shrink a canary without a pipeline click, you do not have progressive delivery — you have **progressive paperwork**.

Ship judgment. Leave the jar alone.

---

*Runnable: [sre-canary-percent](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-canary-percent) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
