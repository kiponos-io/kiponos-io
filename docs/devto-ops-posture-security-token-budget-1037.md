---
title: "Live Token Budget Per Request for Security — Mouth to Process in Seconds"
published: false
tags: java, security, devops, kiponos
description: "Live token budget per request via Kiponos Java SDK — security posture without redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ops-posture-security-token-budget-1037.md
main_image: https://files.catbox.moe/k81e1q.jpg
---

**The Aha:** `tokenBudget` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

A regional failover needed different posture, not a different binary. The ConfigMap disagreed.

Domain: WAF, RPS, auth leeway. This essay maps hub key `tokenBudget` to **token budget per request** so the lesson stays concrete for security operators.

## The problem: ceremony between judgment and effect

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

When token budget per request is frozen in YAML, every incident becomes a process argument. When it lives in a hub with clamps, the argument ends and the work begins.

| Belief | Production |
|--------|------------|
| Defaults are fine | Defaults become root causes under load |
| GitOps will handle it | Git is a ledger, not a pager for second-scale posture |
| The mesh owns that | Mesh freezes share to another YAML dialect |
| We'll hotfix | Hotfix is still CI + roll + nerves |

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  ops-security-token-budget/
    tokenBudget: 4000   # token budget per request
    hardMax: compiled-in-app
    failClosed: true
```

```java
var policy = kiponos.path("examples", "ops-security-token-budget");
String mode = policy.getString("degradeMode", "full");
int tokenBudget = policy.getInt("tokenBudget");
return router.decide(mode, tokenBudget);
```

Ops sets `tokenBudget` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
                               Hot path decision (token budget per request)
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

1. Name the hub path so humans find it under pressure (`examples/ops-security-token-budget/tokenBudget`).  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging with a sibling example module.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture on a hot path**: token budget per request for security — numbers humans already change verbally in war rooms.

Kiponos makes that verbal decision **executable** without a second control plane tax on every request.

## Pair with a sister dial

`tokenBudget` rarely moves alone. Pair with timeout/retry, canary share, or sampling so you do not fix one symptom by creating another.

## Rehearsal beats slides

In staging: set a painful value, prove recovery without restart, prove clamps reject nonsense, prove LKG when hub is firewalled. That drill ends half the architecture arguments.

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

## One-line runbook

*Who may move this key under P1, what is the clamp, what is the revert?* Write that sentence before you need it. Posture without a revert path is just another outage mode.

## Moral

token budget per request that requires a deploy is optimistic documentation.

Ship judgment. Leave the jar alone.

---

*Series: Kiponos live ops posture · Pattern library: [kiponos-io/docs](https://github.com/kiponos-io/kiponos-io/tree/master/docs) · SDK examples: [examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java)*
