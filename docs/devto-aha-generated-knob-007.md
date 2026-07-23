---
title: "Log Sample Per Thousand Without a Redeploy — Live Ops Knob 7"
published: false
tags: java, devops, architecture, kiponos
description: "Live structured log sampling under load via Kiponos example aha-generated-knob-007 (hub key knob-7)."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-generated-knob-007.md
main_image: ./devto-cover-aha-generated-knob-007.jpg
---

**The Aha:** `knob-7` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

Log volume is a fire. Sampling is a hose. This stream demo maps hub key `knob-7` to **structured log sampling under load** so the lesson stays concrete.

## The problem: ceremony between judgment and effect

The continuous-factory stub called it generic. Production never is. Treat `knob-7` as **Log Sample Per Thousand** and practice the same discipline you use for retries and RPS.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: Demo stream knob with production meaning: structured log sampling under load.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  aha-generated-knob-007/
    knob-7: 17   # interpreted as Log Sample Per Thousand
```

```java
Folder policy = kiponos.path("examples", "aha-generated-knob-007");
int v = policy.getInt("knob-7"); // local get — interpret as Log Sample Per Thousand
// apply to structured log sampling under load
useValue(v);
```

Ops sets `knob-7` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
Human / automation ──set knob-7──▶ Kiponos.io hub
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
cd kiponos-io/examples/java/aha-generated-knob-007
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-007](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-007)

Try it tonight:

1. Run tests — prove default `17` is coherent.  
2. Change `knob-7` in the hub — confirm the next run applies the new value without rebuild.  
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

Cost and sight — both dials. Super pattern lesson: **name the posture**, even when the demo key is generic.

Ship judgment. Leave the jar alone.

---

*Runnable: [aha-generated-knob-007](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-007) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
