---
title: "The Saas War Room Already Knew logSamplePerThousand — The Jar Did Not"
published: false
tags: java, saas, architecture, kiponos
description: "Live log sample per thousand via Kiponos Python SDK — saas posture without redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ops-posture-saas-log-sample-0263.md
main_image: https://files.catbox.moe/3yqf58.jpg
---

**The Aha:** `logSamplePerThousand` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

Merchandising spoke margin. Engineering spoke Gradle. The gap was a release train.

Domain: multi-tenant quotas, feature posture. This essay maps hub key `logSamplePerThousand` to **log sample per thousand** so the lesson stays concrete for saas operators.

## The problem: ceremony between judgment and effect

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

When log sample per thousand is frozen in YAML, every incident becomes a process argument. When it lives in a hub with clamps, the argument ends and the work begins.

| Belief | Production |
|--------|------------|
| We'll tune next sprint | Incidents do not respect sprints |
| We'll hotfix | Hotfix is still CI + roll + nerves |
| It's just config | Config is packaged as a deploy unit |
| The mesh owns that | Mesh freezes share to another YAML dialect |

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Python SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  ops-saas-log-sample/
    logSamplePerThousand: 50   # log sample per thousand
    hardMax: compiled-in-app
    failClosed: true
```

```python
policy = kiponos.path("examples", "ops-saas-log-sample")
logSamplePerThousand = min(int(policy.get("logSamplePerThousand", 50)), HARD_MAX)
worker_pool.resize(logSamplePerThousand)  # or admission semaphore
```

Ops sets `logSamplePerThousand` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
                               Hot path decision (log sample per thousand)
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

1. Name the hub path so humans find it under pressure (`examples/ops-saas-log-sample/logSamplePerThousand`).  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging with a sibling example module.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture on a hot path**: log sample per thousand for saas — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## What never goes live without review

Protocol/schema changes, crypto material, and legal freezes stay in code review. Posture numbers war rooms already shout belong in the hub with clamps.

## Multi-region folders

Same key **structure** in every env/region. Different values on purpose. Structure drift is a bug; value drift is often strategy.

## Automation may write the same path

Budget burn high → automation tightens. Dependency green → automation restores baseline. Humans override both via the same hub path. One tree — no second product.


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

## One-line runbook

*Who may move this key under P1, what is the clamp, what is the revert?* Write that sentence before you need it. Posture without a revert path is just another outage mode.

## Moral

Budget is posture, not a fossil in properties.

Ship judgment. Leave the jar alone.

---

*Series: Kiponos live ops posture · Pattern library: [kiponos-io/docs](https://github.com/kiponos-io/kiponos-io/tree/master/docs) · SDK examples: [examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java)*
