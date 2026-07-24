---
title: "We Stopped Shipping Rag Top-K as a Constant (Mobile)"
published: false
tags: java, mobile, architecture, kiponos
description: "Live RAG top-k via Kiponos Java SDK — mobile posture without redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ops-posture-mobile-rag-topk-1186.md
main_image: https://files.catbox.moe/hcdp5x.jpg
---

**The Aha:** `topK` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

The bot farm finished the catalog while CI was still green-building a one-line timeout change.

Domain: API timeouts, payload caps. This essay maps hub key `topK` to **RAG top-k** so the lesson stays concrete for mobile operators.

## The problem: ceremony between judgment and effect

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

When RAG top-k is frozen in YAML, every incident becomes a process argument. When it lives in a hub with clamps, the argument ends and the work begins.

| Belief | Production |
|--------|------------|
| GitOps will handle it | Git is a ledger, not a pager for second-scale posture |
| We'll hotfix | Hotfix is still CI + roll + nerves |
| We'll tune next sprint | Incidents do not respect sprints |
| The mesh owns that | Mesh freezes share to another YAML dialect |

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  ops-mobile-rag-topk/
    topK: 8   # RAG top-k
    hardMax: compiled-in-app
    failClosed: true
```

```java
var policy = kiponos.path("examples", "ops-mobile-rag-topk");
String mode = policy.getString("degradeMode", "full");
int topK = policy.getInt("topK");
return router.decide(mode, topK);
```

Ops sets `topK` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
                               Hot path decision (RAG top-k)
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

1. Name the hub path so humans find it under pressure (`examples/ops-mobile-rag-topk/topK`).  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging with a sibling example module.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture on a hot path**: RAG top-k for mobile — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## Incident script (paste into runbook)

1. Confirm the signal (SLO burn, queue depth, partner errors).  
2. Move `topK` to the emergency value (documented floor/ceiling).  
3. Watch two metrics for five minutes.  
4. Step or revert.  
5. Postmortem line: who moved from→to and why.

## Pair with a sister dial

`topK` rarely moves alone. Pair with timeout/retry, canary share, or sampling so you do not fix one symptom by creating another.

## Failure budget vs this dial

Treat `topK` as a slice of **error budget**, not a comfort blanket. Raise it when the dependency is healthy; lower it when the dependency is already sick. Write the number that survives a bad day, not the number that flatters a sunny demo.


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

If mouth→process is longer than the incident, you already lost.

Ship judgment. Leave the jar alone.

---

*Series: Kiponos live ops posture · Pattern library: [kiponos-io/docs](https://github.com/kiponos-io/kiponos-io/tree/master/docs) · SDK examples: [examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java)*
