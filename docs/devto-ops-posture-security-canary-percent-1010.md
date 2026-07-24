---
title: "Canary Traffic Share Should Not Wait for a Redeploy — Security Live Posture"
published: false
tags: java, security, devops, kiponos
description: "Live canary traffic share via Kiponos Java SDK — security posture without redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ops-posture-security-canary-percent-1010.md
main_image: https://files.catbox.moe/ne8ehd.jpg
---

**The Aha:** `canaryPercent` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

The bot farm finished the catalog while CI was still green-building a one-line timeout change.

Domain: WAF, RPS, auth leeway. This essay maps hub key `canaryPercent` to **canary traffic share** so the lesson stays concrete for security operators.

## The problem: ceremony between judgment and effect

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

When canary traffic share is frozen in YAML, every incident becomes a process argument. When it lives in a hub with clamps, the argument ends and the work begins.

| Belief | Production |
|--------|------------|
| GitOps will handle it | Git is a ledger, not a pager for second-scale posture |
| We'll tune next sprint | Incidents do not respect sprints |
| Flags cover this | Second system, second delay, second outage mode |
| The mesh owns that | Mesh freezes share to another YAML dialect |

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  ops-security-canary-percent/
    canaryPercent: 5   # canary traffic share
    hardMax: compiled-in-app
    failClosed: true
```

```java
Folder policy = kiponos.path("examples", "ops-security-canary-percent");
int canaryPercent = policy.getInt("canaryPercent");
if (canaryPercent <= 0) {
    return failClosed(); // money paths
}
return apply(canaryPercent);
```

Ops sets `canaryPercent` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
                               Hot path decision (canary traffic share)
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

1. Name the hub path so humans find it under pressure (`examples/ops-security-canary-percent/canaryPercent`).  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging with a sibling example module.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture on a hot path**: canary traffic share for security — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## Multi-region folders

Same key **structure** in every env/region. Different values on purpose. Structure drift is a bug; value drift is often strategy.

## Incident script (paste into runbook)

1. Confirm the signal (SLO burn, queue depth, partner errors).  
2. Move `canaryPercent` to the emergency value (documented floor/ceiling).  
3. Watch two metrics for five minutes.  
4. Step or revert.  
5. Postmortem line: who moved from→to and why.

## Where to enforce

Prefer the earliest durable hop that still knows identity. Edge hard-caps stay as seatbelts; app middleware reads live posture under that seatbelt. Do not invent a third control plane.


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

Budget is posture, not a fossil in properties.

Ship judgment. Leave the jar alone.

---

*Series: Kiponos live ops posture · Pattern library: [kiponos-io/docs](https://github.com/kiponos-io/kiponos-io/tree/master/docs) · SDK examples: [examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java)*
