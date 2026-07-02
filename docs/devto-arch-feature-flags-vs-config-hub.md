---
title: "Feature Flags vs a Live Config Hub — When Kiponos Is the Better Primitive (Architecture)"
published: false
tags: architecture, devops, config, java
description: Feature-flag SaaS solves boolean rollouts. Kiponos solves the whole live config surface — floats, trees, cross-service collaboration, and zero-latency reads. Architecture guide for platform teams.
canonical_url: https://dev.to/kiponos/feature-flags-vs-a-live-config-hub-when-kiponos-is-the-better-primitive-architecture-4pb
main_image: https://files.catbox.moe/ejz860.jpg
---

Platform teams buy feature-flag SaaS for **boolean rollouts**. Then they need **numeric thresholds**, **nested JSON**, **per-tenant limits**, and **cross-service coordination** — and bolt on Redis, Consul, and YAML anyway. Three systems, three caches, three deploy stories.

[Kiponos.io](https://kiponos.io) is a **live config hub** — not a boolean-only flag vendor.

## What feature flags do well

- User targeting (`if flag enabled for cohort`)
- Percentage rollouts of **features**
- Audit for product experiments

## Where flags stop

| Need | Boolean flag? |
|------|----------------|
| Fraud `block_score = 85` | Awkward JSON blob |
| Saga `step_timeout_ms` | Wrong tool |
| ML `learning_rate` | Wrong tool |
| Multi-service `shipping_ready` | No shared tree |
| **Zero-latency read in hot loop** | SDK evaluation still network |

## Kiponos positioning

![Architecture diagram](https://files.catbox.moe/nxbszi.png)

**Use flags** for product cohort experiments tied to identity.

**Use Kiponos** for:

- Operational thresholds (rates, timeouts, scores)
- Structured trees (`fraud/thresholds/block_score`)
- **Shared collaboration state** across microservices
- Java + Python same hub
- Environments as **profile paths** not file forks

You can still store `features/new_checkout_enabled: true` — it is a first-class config key, not a special boolean SKU.

## Architecture decision matrix

| Criterion | Feature-flag SaaS | Kiponos |
|-----------|-------------------|---------|
| Hot-path read latency | Network eval | **Local cache** |
| Numeric tuning | Hacky | Native |
| Cross-service shared tree | No | **Yes** |
| Delta updates | Yes | **Yes** |
| Product cohort targeting | **Strong** | Use profile + your logic |

## Hybrid pattern (common)

```java
boolean newCheckout = kiponos.path("features").getBool("new_checkout_enabled");
int fraudBlock = kiponos.path("fraud", "thresholds").getInt("block_score");
```

One SDK, one WebSocket, one dashboard — product **and** ops keys.

## Getting started

1. Map existing flags **and** thresholds into one Kiponos profile
2. Replace `launchDarklyVariation` for **numeric** keys with `kiponos.path().get*()`
3. Keep cohort targeting in app code or external ID service — feed **weights** from Kiponos

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — beyond booleans. The live config hub for systems that must change at runtime.*