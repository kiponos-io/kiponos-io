---
title: "Black Friday Pricing Weights Should Not Wait for a Release Train"
published: false
tags: java, retail, architecture, kiponos
description: "Live dynamic pricing weight via Kiponos — merchandising posture without redeploying checkout."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-retail-dynamic-pricing-knob.md
main_image: https://files.catbox.moe/1vhoe4.jpg
---

**The Aha:** `weight` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

Merchandising does not speak Gradle. They speak **margin** and **sell-through**. Your pricing weight in a properties file is an insult to both.

## The problem: ceremony between judgment and effect

A flash sale needs the discount weight raised in the next five minutes. The jar still believes last week's weight.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: Retail checkout, promotions, A/B pricing experiments.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  retail-dynamic-pricing-knob/
    weight: 1.0
```

```java
Folder policy = kiponos.path("examples", "retail-dynamic-pricing-knob");
double w = policy.getDouble("weight"); // live
return basePrice * w;
```

Ops sets `weight` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
Human / automation ──set weight──▶ Kiponos.io hub
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
cd kiponos-io/examples/java/retail-dynamic-pricing-knob
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/retail-dynamic-pricing-knob](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/retail-dynamic-pricing-knob)

Try it tonight:

1. Run tests — prove default `1.0` is coherent.  
2. Change `weight` in the hub — confirm the next run applies the new value without rebuild.  
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

## Merchandising speaks margin; engineering spoke Gradle

Pricing weight is a **commercial** control. Packaging it as a deploy artifact forces merchandising to borrow an engineering change train every flash sale.

That is not governance. That is friction dressed as safety.

## Guardrails that keep finance calm

Live does **not** mean unbounded:

- Clamp `weight` to an allowlisted range (e.g. 0.0–1.0 or basis points)  
- Dual-control on writes for production folder (role + break-glass)  
- Audit every change with actor, old, new, ticket id  
- Default to **last-known-good** if the hub is unreachable mid-checkout  

Finance cares that the number is **bounded and auditable**. They do not care that it lived in a JAR.

## Black Friday drill

Rehearse once before peak:

1. Staging: move weight, prove checkout price tag updates within one request cycle.  
2. Prove clamp rejects out-of-range values.  
3. Prove LKG when hub is firewalled.  
4. Time the full path: human decision → dashboard → next priced cart.  

If that path is longer than five minutes, you will invent Slack-driven "manual price overrides" — a worse control plane.

## What never goes live

- Tax tables that legal freezes quarterly  
- Currency conversion formulas needing dual review  
- Anything that rewrites historical invoices  

Weights, boosts, and experiment shares are posture. Ledgers are law.


## Moral

Pricing posture is a business decision. Super patterns put **business decisions in the hub**.

Ship judgment. Leave the jar alone.

---

*Runnable: [retail-dynamic-pricing-knob](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/retail-dynamic-pricing-knob) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
