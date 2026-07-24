---
title: "Upload Size Ceiling Mb Should Not Wait for a Redeploy — Retail Live Posture"
published: false
tags: java, retail, devops, kiponos
description: "Live upload size ceiling MB via Kiponos Java SDK — retail posture without redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ops-posture-retail-upload-max-mb-0020.md
main_image: https://files.catbox.moe/qi6qjb.jpg
---

**The Aha:** `uploadMaxMb` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

At 03:07, the war room already knew the number. The jar still believed last week's YAML.

Domain: checkout, merchandising, inventory. This essay maps hub key `uploadMaxMb` to **upload size ceiling MB** so the lesson stays concrete for retail operators.

## The problem: ceremony between judgment and effect

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

When upload size ceiling MB is frozen in YAML, every incident becomes a process argument. When it lives in a hub with clamps, the argument ends and the work begins.

| Belief | Production |
|--------|------------|
| The mesh owns that | Mesh freezes share to another YAML dialect |
| We'll hotfix | Hotfix is still CI + roll + nerves |
| Defaults are fine | Defaults become root causes under load |
| GitOps will handle it | Git is a ledger, not a pager for second-scale posture |

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  ops-retail-upload-max-mb/
    uploadMaxMb: 25   # upload size ceiling MB
    hardMax: compiled-in-app
    failClosed: true
```

```java
var policy = kiponos.path("examples", "ops-retail-upload-max-mb");
String mode = policy.getString("degradeMode", "full");
int uploadMaxMb = policy.getInt("uploadMaxMb");
return router.decide(mode, uploadMaxMb);
```

Ops sets `uploadMaxMb` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
                               Hot path decision (upload size ceiling MB)
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

1. Name the hub path so humans find it under pressure (`examples/ops-retail-upload-max-mb/uploadMaxMb`).  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging with a sibling example module.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture on a hot path**: upload size ceiling MB for retail — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## Multi-region folders

Same key **structure** in every env/region. Different values on purpose. Structure drift is a bug; value drift is often strategy.

## Incident script (paste into runbook)

1. Confirm the signal (SLO burn, queue depth, partner errors).  
2. Move `uploadMaxMb` to the emergency value (documented floor/ceiling).  
3. Watch two metrics for five minutes.  
4. Step or revert.  
5. Postmortem line: who moved from→to and why.

## Rehearsal beats slides

In staging: set a painful value, prove recovery without restart, prove clamps reject nonsense, prove LKG when hub is firewalled. That drill ends half the architecture arguments.


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

The jar is for what is true across deploys. The hub is for what must be true in the next minute.

Ship judgment. Leave the jar alone.

---

*Series: Kiponos live ops posture · Pattern library: [kiponos-io/docs](https://github.com/kiponos-io/kiponos-io/tree/master/docs) · SDK examples: [examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java)*
