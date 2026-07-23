---
title: "DB Pool Ceiling Without a Redeploy — Live Ops Knob 1"
published: false
tags: java, devops, architecture, kiponos
description: "Live max open connections on the primary pool via Kiponos example aha-generated-knob-001 (hub key knob-1)."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-generated-knob-001.md
main_image: https://files.catbox.moe/ssmz78.jpg
---

**The Aha:** `knob-1` is not a property file trophy. It is **incident posture** — and posture that waits for a jar is already late.

When checkout waits on connections, the ceiling is the incident — not the schema. This stream demo maps hub key `knob-1` to **max open connections on the primary pool** so the lesson stays concrete.

## The problem: ceremony between judgment and effect

The continuous-factory stub called it generic. Production never is. Treat `knob-1` as **DB Pool Ceiling** and practice the same discipline you use for retries and RPS.

You already know the right number. Everyone in the war room knows the right number. What you do not have is a path from **mouth → running process** that is shorter than a release train.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still CI + roll |
| "Flags cover this" | Second system, second delay |
| "Defaults are fine" | Defaults become root causes |

Domain: Demo stream knob with production meaning: max open connections on the primary pool.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  aha-generated-knob-001/
    knob-1: 11   # interpreted as DB Pool Ceiling
```

```java
Folder policy = kiponos.path("examples", "aha-generated-knob-001");
int v = policy.getInt("knob-1"); // local get — interpret as DB Pool Ceiling
// apply to max open connections on the primary pool
useValue(v);
```

Ops sets `knob-1` in the dashboard (or automation writes the same path). The **next** evaluation uses the new value. Same jar. Same tests for structure.

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
Human / automation ──set knob-1──▶ Kiponos.io hub
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
cd kiponos-io/examples/java/aha-generated-knob-001
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-001](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-001)

Try it tonight:

1. Run tests — prove default `11` is coherent.  
2. Change `knob-1` in the hub — confirm the next run applies the new value without rebuild.  
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

## Pool ceilings are congestion control

`max open connections` is how hard you lean on the database when every service panics together. Raise it blindly and you melt the primary. Lower it blindly and you queue the business.

## Live ceiling, compiled hard max

- Hub: operational ceiling operators may touch  
- Jar: absolute hard max the driver will never exceed  
- Default: safe mid value when hub is unreachable  

That split keeps a typo from opening 10k connections while still letting on-call relieve a legitimate stampede **without** a deploy.

## What to watch after a change

1. Active connections vs ceiling  
2. Wait time acquiring connections  
3. DB CPU / lock waits  
4. Application timeouts that used to be pool waits  

If wait time drops but DB CPU pegs, you did not fix capacity — you **moved** the bottleneck.

## Rehearsal

In staging, set ceiling low until checkout queues, then raise live and prove recovery without restart. That single drill ends half the "we need a bigger pool in the next release" arguments.


Ship the clamp. Ship the audit. Ship the revert path.

## Moral

If pool size waits for a jar, you scheduled the timeout. Super pattern lesson: **name the posture**, even when the demo key is generic.

Ship judgment. Leave the jar alone.

---

*Runnable: [aha-generated-knob-001](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-001) · Product: [kiponos.io](https://kiponos.io) · Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)*
