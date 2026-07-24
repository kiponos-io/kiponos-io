---
title: "Inference Batch Size Should Not Wait for a Redeploy — Saas Live Posture"
published: false
tags: java, saas, architecture, kiponos
description: "Live inference batch size via Kiponos Python SDK — saas posture without redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ops-posture-saas-inference-batch-0285.md
main_image: https://files.catbox.moe/xhwqbq.jpg
---

**The Aha:** `batchSize` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

The dependency healed. Your circuit stayed open because open-wait was a fossil in the jar.

Domain: multi-tenant quotas, feature posture. This essay maps hub key `batchSize` to **inference batch size** so the lesson stays concrete for saas operators.

## The problem: ceremony between judgment and effect

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

When inference batch size is frozen in YAML, every incident becomes a process argument. When it lives in a hub with clamps, the argument ends and the work begins.

| Belief | Production |
|--------|------------|
| We'll tune next sprint | Incidents do not respect sprints |
| We'll hotfix | Hotfix is still CI + roll + nerves |
| Flags cover this | Second system, second delay, second outage mode |
| Defaults are fine | Defaults become root causes under load |

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Python SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  ops-saas-inference-batch/
    batchSize: 16   # inference batch size
    hardMax: compiled-in-app
    failClosed: true
```

```python
policy = kiponos.path("examples", "ops-saas-inference-batch")
batchSize = int(policy.get("batchSize", 16))
if not limiter.try_acquire(batchSize):
    return too_many()
return handle(req)
```

Ops sets `batchSize` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

## What stays in the jar vs the hub

| Jar (versioned) | Hub (live) |
|-----------------|------------|
| Code paths & clamps | Operational numbers |
| Hard maxima / allowlists | Current posture |
| Schema & types | Human judgment under pressure |
| Fail-closed defaults | Temporary incident overrides |

## Architecture

```
Dashboard / automation ──write──► Kiponos hub tree
                                      │ WebSocket delta
                                      ▼
                               SDK in-process cache
                                      │ local get
                                      ▼
                               Hot path decision (inference batch size)
```

No sidecar tax on every request. No second product for "just this one dial."

## Clone and learn the pattern

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
# See examples/java/* for runnable Super Pattern / Aha modules
# Profile: ['app']['release']['env']['config'] — same shape as production
```

Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md) · Product: [kiponos.io](https://kiponos.io)

## Scenarios

| Moment | Frozen YAML | Live hub |
|--------|-------------|----------|
| Incident | PR + pipeline | Seconds |
| Peak event | Over-provision | Dial down/up |
| Experiment | Long-lived branch | Same jar |
| Rollback | Redeploy previous | Revert hub value |
| Region skew | Copy three files | Per-folder values |

## When not to live-edit

- Protocol or schema changes that need coordinated rollouts  
- Values that compliance requires code-reviewed only  
- Anything you cannot clamp or allowlist safely  
- Secrets (use a secret manager — never the ops posture tree)

Live knobs are for **posture**, not for inventing untested systems under fire.

## Operational checklist

1. Name the hub path so humans find it under pressure (`examples/ops-saas-inference-batch/batchSize`).  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging with a sibling example module.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture on a hot path**: inference batch size for saas — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## Automation may write the same path

Budget burn high → automation tightens. Dependency green → automation restores baseline. Humans override both via the same hub path. One tree — no second product.

## What never goes live without review

Protocol/schema changes, crypto material, and legal freezes stay in code review. Posture numbers war rooms already shout belong in the hub with clamps.

## Multi-region folders

Same key **structure** in every env/region. Different values on purpose. Structure drift is a bug; value drift is often strategy.


## A note on testing

Unit-test structure with fixed strings (no network). Integration-test the hub path against the public sandbox when you can.

Good tests:

- Defaults when keys are missing  
- Clamps reject out-of-range values  
- Fail-closed behavior for money paths  

Bad tests:

- Hitting production hubs from CI  
- Asserting wall-clock times for WebSocket delivery  


## Closing note

Architecture diagrams do not absorb incidents. Steerable posture does — with audit, clamps, and a revert path written before you need it.

## Moral

Isolation patterns without steerable thresholds are beautiful diagrams that lose to Tuesday.

Ship judgment. Leave the jar alone.

---

*Series: Kiponos live ops posture · Pattern library: [kiponos-io/docs](https://github.com/kiponos-io/kiponos-io/tree/master/docs) · SDK examples: [examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java)*
