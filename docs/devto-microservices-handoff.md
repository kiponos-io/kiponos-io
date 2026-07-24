---
title: "Cross-Service Handoff Signals and Locks in Real Time (Kiponos Python SDK)"
published: true
tags: java, microservices, architecture, kiponos
description: "Coordinate service handoffs with live flags and lease TTLs in a shared Kiponos tree. Python workers read locally; ops flips workflow state without redeploy."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-handoff.md
main_image: https://files.catbox.moe/hkgx7a.jpg
---

**The Aha:** `handoffTimeoutMs` is not a property-file trophy. It is **incident posture** for service handoff — and posture that waits for a jar is already late.

At 02:14 the war room already knew the right **handoff timeout**. The jar still believed last week's YAML.

## The problem: ceremony between judgment and effect

When **handoff timeout** is frozen in a deploy unit, every incident becomes a process argument. Hotfixes still mean CI + roll. Feature flags often mean a second control plane with its own delay.

| Belief | Production |
|--------|------------|
| "It's just config" | Config is packaged as a deploy unit |
| "We'll hotfix" | Hotfix is still pipeline + nerves |
| "Flags cover this" | Second system, second outage mode |
| "Defaults are fine" | Defaults become root causes under load |

Domain: service handoff.

## The Aha: local read, live write

[Kiponos.io](https://kiponos.io) holds the tree. The **Java SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

```yaml
examples/
  microservices-handoff/
    handoffTimeoutMs: <live>
```

```java
Folder policy = kiponos.path("examples", "microservices-handoff");
int handoffTimeoutMs = policy.getInt("handoffTimeoutMs"); // local get — no hub RTT
if (handoffTimeoutMs <= 0) {
    return failClosed();
}
return apply(handoffTimeoutMs);
```

Ops (or automation) sets `handoffTimeoutMs` in the hub. The **next** evaluation uses it. Same binary. Same tests for structure.

## What stays in the jar vs the hub

| Jar (versioned) | Hub (live) |
|-----------------|------------|
| Code paths & clamps | Operational numbers |
| Hard maxima / allowlists | Current posture |
| Schema & types | Human judgment under pressure |
| Fail-closed defaults | Temporary incident overrides |

## Architecture

```
Dashboard / automation ──write──► Kiponos hub
                                      │ delta
                                      ▼
                               SDK in-process cache
                                      │ local get
                                      ▼
                               Hot path (handoff timeout)
```

No sidecar tax on every request. One tree humans and remote SDKs share.

## Clone and run the pattern

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
# examples/java/* — Super Patterns + Aha modules
# Profile: ['app']['release']['env']['config']
```

- Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)  
- Product: [kiponos.io](https://kiponos.io)  
- Canonical source: [this article on GitHub](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-handoff.md)

## Scenarios

| Moment | Frozen YAML | Live hub |
|--------|-------------|----------|
| Incident | PR + pipeline | Seconds |
| Peak event | Over-provision | Dial down/up |
| Experiment | Long-lived branch | Same jar |
| Rollback | Redeploy previous | Revert hub value |

## When not to live-edit

- Protocol / schema changes that need coordinated rollouts  
- Values compliance freezes to code review only  
- Secrets (secret manager — never the ops posture tree)  
- Anything you cannot clamp or allowlist safely  

## Operational checklist

1. Name the hub path so humans find it under pressure.  
2. Default safely when the hub is unreachable (fail closed on money paths).  
3. Allowlist writers (dashboard + automation identities).  
4. Log **decisions** and hub writes — not every get.  
5. Rehearse the flip in staging.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

## Why this is not "just another flag"

Feature flags are often product gates. This essay is about **ops posture** on service handoff: **handoff timeout** — numbers war rooms already shout.

Kiponos makes that verbal decision **executable** without a second control-plane tax on every request.

## Guardrails

Live does not mean unbounded. Compile a hard max. Audit actor/old/new/ticket. Prefer fail-closed on money and fraud paths when the hub is dark.

## Failure budget thinking

Treat `handoffTimeoutMs` as a slice of error budget. Raise when the world is healthy; lower when dependencies are sick. Write the number that survives a bad day.

## Observability

Ship counters for decisions applied, rejects, and hub write events. Logging every local get teaches nothing; logging every change teaches ownership.

## A note on testing

Unit-test structure with fixed strings (no network). Integration-test against the public sandbox when you can. Good tests: missing-key defaults, clamps, fail-closed. Bad tests: production hubs from CI.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

Posture beats ceremony — every time.

## Moral

**Handoff timeout** that requires a deploy is optimistic documentation.

Ship judgment. Leave the jar alone.

---

*Kiponos live ops · [docs library](https://github.com/kiponos-io/kiponos-io/tree/master/docs) · [examples/java](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java)*
